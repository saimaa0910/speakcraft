package com.example.speech.session

import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.RmsSeriesFeatureExtractor
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.AttemptInput
import com.example.speech.feedback.TipDefinition
import com.example.speech.feedback.TipSelector
import com.example.speech.pipeline.IssueOverride
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.recognition.RecognitionError
import com.example.speech.rules.IssueCode
import com.example.speech.tier.SpeechTier
import com.example.speech.video.FrameObservation
import com.example.speech.video.VideoAnalyzer

/** What the attempt is about (content-pack activity fields used by evaluation; ARC-03 §6). */
data class AttemptRequest(
    val attemptId: String,
    val templateId: String,
    val maxDurationMs: Long,
    val referenceText: String? = null,
    val targetWord: String? = null,
    val targetWordInflections: List<String> = emptyList(),
    val targetMinWords: Int? = null,
    val targetMaxWords: Int? = null,
    val framework: String? = null,
    val techniqueIds: List<String> = emptyList(),
    val previousMetrics: Map<SpeechMetricKey, SpeechMetricValue>? = null,
    val issueOverrides: Map<IssueCode, IssueOverride> = emptyMap()
)

/** Thread-safe collector for camera frame observations (fed by the MediaPipe analyser; ARC-07 §3). */
class FrameCollector(private val maxFrames: Int = 6_000) {
    private val frames = ArrayList<FrameObservation>()

    /** False between attempts, so a camera preview can stay bound without collecting frames. */
    @Volatile var accepting: Boolean = true

    fun add(frame: FrameObservation) = synchronized(frames) {
        if (accepting && frames.size < maxFrames) frames += frame
    }
    fun snapshot(): List<FrameObservation> = synchronized(frames) { frames.toList() }

    /** Clears frames from a previous attempt and starts collecting. */
    fun restart() = synchronized(frames) { frames.clear(); accepting = true }
}

/** Everything one attempt produced, for persistence and display. */
data class AttemptOutcome(
    val evaluation: AttemptEvaluationResult,
    val features: AcousticFeatures,
    val stopReason: StopReason,
    /** A fatal recogniser error (network, permission, language unavailable …) — shown with SRS-ERR-002 advice. */
    val recognitionError: RecognitionError?,
    val usedPartialTranscript: Boolean
)

/**
 * Runs one speaking attempt end to end on the device (CMP-29; ARC-05 §2–§5, ARC-07):
 *
 * | Tier | Audio evidence | Transcript |
 * |---|---|---|
 * | T1 | app-owned PCM → [CaptureController] (and piped to the recogniser via [AttemptWiring.recognizerPipe]) | [RecognitionSession] SEGMENTED_SESSION |
 * | T2 / T3a | recogniser `onRmsChanged` → [RmsSeriesFeatureExtractor] | [RecognitionSession] RESTART_PER_SEGMENT |
 * | T3b | app-owned PCM → [CaptureController] | none |
 *
 * The optional camera feeds a [FrameCollector]; its frames go through [VideoAnalyzer] at stop.
 * Only one attempt can record at a time ([RecordingArbiter]). Pure Kotlin; Android adapters supply the
 * [AudioSource], [RecognizerEngine] and frame stream.
 */
class SpeechAttemptRunner(
    private val arbiter: RecordingArbiter,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Coach tips for feedback: the content pack's tips when a valid pack ships, else the built-in list. */
    private val tipCatalog: () -> List<TipDefinition> = { TipSelector.CATALOG }
) {

    /** Platform objects for one attempt; which ones are needed depends on the tier. */
    data class AttemptWiring(
        val audioSource: AudioSource? = null,
        val recognizer: RecognizerEngine? = null,
        /** T1: sink that writes PCM into the recogniser's `EXTRA_AUDIO_SOURCE` pipe. */
        val recognizerPipe: PcmSink? = null,
        /** E.g. the WAV writer for replay (T1/T3b). */
        val extraSinks: List<PcmSink> = emptyList(),
        val frames: FrameCollector? = null,
        val captureListener: CaptureListener? = null
    )

    inner class ActiveAttempt internal constructor(
        val request: AttemptRequest,
        val tier: SpeechTier,
        private val capture: CaptureController?,
        private val recognition: RecognitionSession?,
        private val frames: FrameCollector?
    ) {
        private val startedAt = clock()
        @Volatile private var closed = false

        /** Stops recording and recognition, then evaluates on the device. */
        fun stop(reason: StopReason = StopReason.USER): AttemptOutcome {
            check(!closed) { "attempt already stopped" }
            closed = true
            try {
                val captureResult = capture?.stop(reason)
                val recognized = recognition?.stop()
                val features = captureResult?.features
                    ?: RmsSeriesFeatureExtractor.analyse(recognized?.rmsSamples.orEmpty(), recognized?.durationMs ?: (clock() - startedAt))
                val transcript = if (tier.hasTranscript) recognized?.transcript.orEmpty() else null
                val video = frames?.snapshot()?.takeIf { it.isNotEmpty() }?.let { VideoAnalyzer.analyse(it) }
                val input = AttemptInput.fromFeatures(
                    attemptId = request.attemptId,
                    templateId = request.templateId,
                    features = features,
                    transcript = transcript,
                    referenceText = request.referenceText,
                    targetWord = request.targetWord,
                    targetWordInflections = request.targetWordInflections,
                    targetMinWords = request.targetMinWords,
                    targetMaxWords = request.targetMaxWords,
                    framework = request.framework,
                    techniqueIds = request.techniqueIds,
                    previousMetrics = request.previousMetrics,
                    issueOverrides = request.issueOverrides,
                    video = video
                )
                return AttemptOutcome(
                    evaluation = LocalIntelligencePipeline.evaluate(input, tipCatalog()),
                    features = features,
                    stopReason = captureResult?.stopReason ?: reason,
                    recognitionError = recognized?.error,
                    usedPartialTranscript = recognized?.usedPartialFallback ?: false
                )
            } finally {
                arbiter.release(request.attemptId)
            }
        }

        /** Discards the attempt (SRS-FR-SESS-011): nothing is evaluated. */
        fun cancel() {
            if (closed) return
            closed = true
            runCatching { capture?.stop(StopReason.INTERRUPTED) }
            runCatching { recognition?.cancel() }
            arbiter.release(request.attemptId)
        }

        /** For UI auto-stop: waits for the capture to end by itself (max duration or failure). */
        fun awaitAutoStop(timeoutMs: Long): CaptureResult? = capture?.awaitStopped(timeoutMs)
    }

    /**
     * Starts an attempt. Throws [IllegalStateException] when another recording is active or when the
     * wiring does not match the tier; exceptions from starting the audio source (e.g. permission revoked)
     * propagate after the arbiter is released.
     */
    fun start(request: AttemptRequest, tier: SpeechTier, wiring: AttemptWiring): ActiveAttempt {
        check(arbiter.tryAcquire(request.attemptId)) { "another recording is active: ${arbiter.activeAttemptId}" }
        try {
            val needsPcm = tier == SpeechTier.T1_FULL_ON_DEVICE || tier == SpeechTier.T3B_AUDIO_ONLY
            val needsRecognizer = tier.hasTranscript
            if (needsPcm) requireNotNull(wiring.audioSource) { "$tier needs an AudioSource" }
            if (needsRecognizer) requireNotNull(wiring.recognizer) { "$tier needs a RecognizerEngine" }
            if (tier == SpeechTier.T1_FULL_ON_DEVICE) requireNotNull(wiring.recognizerPipe) { "T1 needs the recogniser pipe" }

            val recognition = if (needsRecognizer) RecognitionSession(
                wiring.recognizer!!,
                if (tier == SpeechTier.T1_FULL_ON_DEVICE) RecognitionMode.SEGMENTED_SESSION else RecognitionMode.RESTART_PER_SEGMENT,
                clock
            ) else null

            val capture = if (needsPcm) CaptureController(
                source = wiring.audioSource!!,
                sinks = listOfNotNull(wiring.recognizerPipe) + wiring.extraSinks,
                maxDurationMs = request.maxDurationMs,
                listener = wiring.captureListener
            ) else null

            recognition?.start()
            try {
                capture?.start()
            } catch (e: Exception) {
                recognition?.cancel()
                throw e
            }
            return ActiveAttempt(request, tier, capture, recognition, wiring.frames)
        } catch (e: Exception) {
            arbiter.release(request.attemptId)
            throw e
        }
    }
}
