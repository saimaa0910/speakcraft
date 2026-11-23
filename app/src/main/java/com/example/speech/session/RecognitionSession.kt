package com.example.speech.session

import com.example.speech.analysis.RmsSample
import com.example.speech.recognition.RecognitionError
import com.example.speech.recognition.SegmentStitcher
import com.example.speech.recognition.TranscriptSegment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * One platform recogniser (CMP-28). The Android implementation wraps `SpeechRecognizer`
 * (`PlatformRecognizerEngine`); tests use scripted fakes. Callbacks may arrive on any thread.
 */
interface RecognizerEngine {
    /** Starts listening for one segment (T2/T3a) or one segmented session (T1). */
    fun startSegment(callbacks: RecognizerCallbacks)
    /** Asks the engine to finish the current segment and deliver its final result. */
    fun stopSegment()
    fun cancel()
    fun release()
}

interface RecognizerCallbacks {
    fun onPartialResult(text: String)
    /** A final result for one segment. [confidence] is `null` when the recogniser gives none (SRS-BR-005). */
    fun onFinalResult(text: String, confidence: Float?)
    fun onRms(rmsDb: Float)
    fun onError(error: RecognitionError)
    /** T1 segmented sessions: the whole session has ended. */
    fun onSessionEnded() {}
}

/** How the recogniser is driven (ARC-05 §3–§4.2). */
enum class RecognitionMode {
    /** T1: app-owned audio piped in (`EXTRA_AUDIO_SOURCE`) with `EXTRA_SEGMENTED_SESSION`; one session, many finals. */
    SEGMENTED_SESSION,
    /** T2/T3a: the recogniser owns the microphone; restart after each final and stitch (SRS-FR-SPCH-009). */
    RESTART_PER_SEGMENT
}

data class RecognitionOutcome(
    /** Stitched final text; empty when nothing was recognised. */
    val transcript: String,
    val segments: List<TranscriptSegment>,
    /** `onRmsChanged` readings, relative to session start (tier T2/T3a acoustic evidence). */
    val rmsSamples: List<RmsSample>,
    /** A fatal error that ended recognition early; `null` for normal completion or no-match restarts. */
    val error: RecognitionError?,
    val durationMs: Long,
    /** True when the final text came only from a partial result because the final never arrived. */
    val usedPartialFallback: Boolean
)

/**
 * Drives a [RecognizerEngine] for one attempt and assembles the transcript (CMP-28, ARC-05 §4.2).
 *
 * - [RecognitionMode.RESTART_PER_SEGMENT]: after each final result — and after "no match" / speech
 *   timeout — the engine is restarted while the session is active, so a long answer is captured in full
 *   (the original app captured only the first utterance, FND-AI-02). Overlapping words at segment
 *   boundaries are removed by [SegmentStitcher].
 * - Fatal errors (network, permission, language unavailable, audio, client) end recognition and are
 *   reported (SRS-FR-SPCH-010); "no match" never becomes a capability failure.
 */
class RecognitionSession(
    private val engine: RecognizerEngine,
    private val mode: RecognitionMode,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxRestarts: Int = 200
) {
    private val lock = Any()
    private var startedAt = 0L
    private var endedAt = 0L
    private var active = false
    private var stopping = false
    private var transcript = ""
    private var lastPartial = ""
    private var restarts = 0
    private var fatal: RecognitionError? = null
    private val segments = ArrayList<TranscriptSegment>()
    private val rms = ArrayList<RmsSample>()
    private val done = CountDownLatch(1)

    private val callbacks = object : RecognizerCallbacks {
        override fun onPartialResult(text: String) = synchronized(lock) { lastPartial = text }

        override fun onFinalResult(text: String, confidence: Float?) {
            var restart = false
            synchronized(lock) {
                val now = clock() - startedAt
                if (text.isNotBlank()) {
                    transcript = SegmentStitcher.stitch(transcript, text)
                    val prevEnd = segments.lastOrNull()?.endOffsetMs ?: 0L
                    segments += TranscriptSegment(segments.size, text, isFinal = true, confidence = confidence, startOffsetMs = prevEnd, endOffsetMs = now)
                }
                lastPartial = ""
                if (stopping || !active) finishLocked()
                else if (mode == RecognitionMode.RESTART_PER_SEGMENT) restart = canRestartLocked()
            }
            if (restart) engine.startSegment(this)
        }

        override fun onRms(rmsDb: Float) = synchronized(lock) {
            if (active || stopping) rms += RmsSample(clock() - startedAt, rmsDb)
        }

        override fun onError(error: RecognitionError) {
            var restart = false
            synchronized(lock) {
                val benign = error is RecognitionError.NoMatch || error is RecognitionError.SpeechTimeout
                when {
                    stopping || !active -> finishLocked()
                    benign && mode == RecognitionMode.RESTART_PER_SEGMENT -> restart = canRestartLocked()
                    benign -> Unit // T1: the segmented session continues
                    else -> { fatal = error; active = false; finishLocked() }
                }
            }
            if (restart) engine.startSegment(this)
        }

        override fun onSessionEnded() = synchronized(lock) { finishLocked() }
    }

    fun start() {
        synchronized(lock) {
            check(!active && done.count == 1L) { "session already started" }
            startedAt = clock()
            active = true
        }
        engine.startSegment(callbacks)
    }

    /** Stops listening and waits up to [finalTimeoutMs] for the last final result. */
    fun stop(finalTimeoutMs: Long = 2_000): RecognitionOutcome {
        val alreadyDone: Boolean
        synchronized(lock) {
            alreadyDone = done.count == 0L
            if (!alreadyDone) { stopping = true; active = false }
        }
        if (!alreadyDone) {
            engine.stopSegment()
            if (!done.await(finalTimeoutMs, TimeUnit.MILLISECONDS)) {
                engine.cancel()
                synchronized(lock) { finishLocked() }
            }
        }
        engine.release()
        return synchronized(lock) { outcomeLocked() }
    }

    /** Cancels without waiting (attempt discarded). */
    fun cancel() {
        synchronized(lock) { active = false; stopping = false; finishLocked() }
        engine.cancel()
        engine.release()
    }

    private var partialFallback = false

    private fun canRestartLocked(): Boolean {
        if (restarts >= maxRestarts) { active = false; finishLocked(); return false }
        restarts++
        return true
    }

    private fun finishLocked() {
        if (done.count == 0L) return
        if (lastPartial.isNotBlank()) {
            // The final result never arrived for the last words: keep what the recogniser heard,
            // flagged, rather than silently dropping speech.
            transcript = SegmentStitcher.stitch(transcript, lastPartial)
            partialFallback = true
            lastPartial = ""
        }
        endedAt = clock()
        active = false
        done.countDown()
    }

    private fun outcomeLocked() = RecognitionOutcome(
        transcript = transcript,
        segments = segments.toList(),
        rmsSamples = rms.toList(),
        error = fatal,
        durationMs = (if (endedAt > 0) endedAt else clock()) - startedAt,
        usedPartialFallback = partialFallback
    )
}
