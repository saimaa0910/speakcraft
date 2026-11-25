package com.example.data.evaluation

import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.FeatureSource
import com.example.speech.analysis.PcmFeatureSink
import com.example.speech.analysis.StreamingFeatureExtractor
import com.example.speech.analysis.WavPcmReader
import com.example.speech.android.RecordedAudioAnalyzer
import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.InsightSeverity
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * Practice-lab evaluation through the local intelligence engine (LabAttemptEvaluator): the lab report and the
 * saved session carry the engine's feedback, audio-only attempts make no transcript claims, inconclusive attempts
 * get no score, and recordings are measured from PCM (WAV of any rate/channels).
 */
class LabAttemptEvaluatorTest {

    private fun features(durationMs: Long, voicedMs: Long, pauses: Int = 0) = AcousticFeatures(
        source = FeatureSource.PCM,
        durationMs = durationMs,
        voicedDurationMs = voicedMs,
        pauses = List(pauses) { com.example.speech.analysis.PauseSpan(1_000L + it * 3_000L, 2_200L + it * 3_000L) },
        activeFrameRms = List((voicedMs / 20).toInt().coerceAtLeast(0)) { 0.1 },
        noiseFloorRms = 0.005,
        clippingRatio = 0.0,
        firstVoicedMs = if (voicedMs > 0) 300L else null,
        lastVoicedMs = if (voicedMs > 0) durationMs - 300L else null
    )

    private val answerRequest = LabAttemptRequest(
        sessionId = "lab_1",
        moduleType = "SPEAKING_LAB",
        promptTitle = "Team Update",
        target = LabPracticeTarget.answer(),
        localMediaPath = "/data/rec.m4a"
    )

    private val transcript42 =
        "Yesterday my manager asked about the release and he don't think we are ready yet so I explained our " +
            "testing plan step by step and showed the results from last week which made the whole team feel " +
            "calmer about the launch date"

    // ── Template selection ─────────────────────────────────────────────────────

    @Test
    fun templates_followTheLab_readAloudScriptsStructureFrameworksAndFreeSpeaking() {
        val read = LabPracticeTarget.readAloud("Speed builds urgency. [PAUSE 1s] That contrast makes people listen.")
        assertEquals(LabPracticeTarget.TPL_READ, read.templateId)
        assertEquals("Speed builds urgency. That contrast makes people listen.", read.referenceText)
        assertEquals(LabPracticeTarget.TPL_ANSWER, LabPracticeTarget.readAloud("  ").templateId)

        val prep = LabPracticeTarget.fromPrompt("PREP Opinion Sprint", "Point -> Reason -> Example -> Point")
        assertEquals(LabPracticeTarget.TPL_STRUCT, prep.templateId)
        assertEquals("PREP", prep.framework)
        assertEquals("STAR", LabPracticeTarget.fromPrompt("Describe a conflict and how you resolved it using STAR").framework)
        // Lower-case words are not framework names ("prep work", "a star player").
        assertEquals(LabPracticeTarget.TPL_ANSWER, LabPracticeTarget.fromPrompt("Do your prep work like a star player").templateId)
        assertEquals(LabPracticeTarget.TPL_ANSWER, LabPracticeTarget.fromPrompt("CLEAR Technical Explainer").templateId)
    }

    // ── Valid attempt with transcript ──────────────────────────────────────────

    @Test
    fun speakingLabAttemptWithTranscript_carriesEngineFeedbackIntoReportAndSession() {
        val f = features(durationMs = 20_000, voicedMs = 16_000)
        val report = LabAttemptEvaluator.evaluate(answerRequest, f, transcript42)

        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        val engine = report.engineResult as AttemptEvaluationResult.Valid
        assertEquals(LocalIntelligencePipeline.EVALUATION_VERSION, engine.evaluationVersion)
        assertEquals("TPL-ANSWER", engine.templateId)
        assertNull("free speaking has no score (D-06)", report.overallScore)
        assertFalse(report.isPartialEvaluation)

        val session = report.session!!
        assertEquals("COMPLETED", session.evaluationStatus)
        assertEquals(transcript42, session.transcript)
        assertEquals(listOf(engine.feedback.strength.message), session.strengths)
        assertEquals(engine.issues.size, session.mistakes.size)
        assertTrue(session.mistakes.any { it.startsWith(IssueCode.ISSUE_GRAMMAR_POSSIBLE.title) && it.contains("he don't") })
        assertTrue(session.improvementSuggestions.containsAll(engine.feedback.tips.map { "${it.title}: ${it.text}" }))
        assertTrue(session.betterAlternatives.contains("Instead of: \"he don't\" → Say: \"he doesn't\""))
        // Pace is measured (words over measured speaking time); nothing else is invented.
        val wpm = engine.allMetrics.getValue(SpeechMetricKey.M_WPM).rawValue!!
        assertEquals(Math.round(wpm), session.wordsPerMinute)
        assertEquals(0L, session.fluencyScore)
        assertEquals(0L, session.pronunciationScore)
        assertFalse(session.pronunciationAssessable)
        assertTrue(session.evaluatedDimensionKeys.isEmpty())
        assertTrue(report.dimensions.isEmpty())
        // The engine version travels with the saved session (hidden data after the note separator).
        assertEquals(LocalIntelligencePipeline.EVALUATION_VERSION, LabAttemptEvaluator.evaluationVersionOf(session))
        assertTrue(LabAttemptEvaluator.isEngineEvaluated(session))
        assertFalse(session.audioQualityNote.substringBefore(LabAttemptEvaluator.NOTE_DATA_SEPARATOR).contains("engine="))
        // Transcript highlights are the engine's (they agree with the raised issues).
        assertTrue(engine.transcriptInsights.any { it.severity == InsightSeverity.ERROR && it.highlightedText == "he don't" })
    }

    @Test
    fun readAloudWithTranscript_isScoredByRecognisedAsIntended_only() {
        val request = answerRequest.copy(moduleType = "PRONUNCIATION", target = LabPracticeTarget.readAloud("She sells sea shells by the sea shore"))
        val report = LabAttemptEvaluator.evaluate(request, features(4_000, 3_000), "she sells sea shells by the sea shore")
        val engine = report.engineResult as AttemptEvaluationResult.Valid
        assertEquals(100, report.overallScore)
        assertEquals(engine.allMetrics.getValue(SpeechMetricKey.M_MATCH).rawValue!!.toInt(), report.overallScore)
        assertEquals("Recognised as intended", LabAttemptEvaluator.scoreLabel(engine))
        assertEquals(true, engine.passed)
    }

    // ── Audio-only ─────────────────────────────────────────────────────────────

    @Test
    fun audioOnlyAttempt_makesNoTranscriptClaims() {
        val report = LabAttemptEvaluator.evaluate(answerRequest, features(20_000, 15_000, pauses = 1), transcript = "  ")

        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        val engine = report.engineResult as AttemptEvaluationResult.Valid
        assertTrue(engine.allMetrics.keys.none { it in MetricEngine.TRANSCRIPT_METRICS })
        assertTrue(engine.transcriptInsights.isEmpty())
        assertEquals(
            listOf(SpeechMetricKey.M_SPK, SpeechMetricKey.M_PAUSE, SpeechMetricKey.M_RATIO),
            engine.headlineMetrics.map { it.key }
        )
        assertNull(report.overallScore)
        assertTrue(report.isPartialEvaluation)
        assertTrue(report.partialReason!!.startsWith("Audio-only feedback"))

        val session = report.session!!
        assertEquals("", session.transcript)
        assertEquals(0L, session.wordsPerMinute)
        assertTrue(session.betterAlternatives.isEmpty())
        assertTrue(session.mistakes.none { it.contains("filler", ignoreCase = true) || it.contains("pace", ignoreCase = true) })
    }

    @Test
    fun readAloudWithoutTranscript_getsNoScore() {
        val request = answerRequest.copy(target = LabPracticeTarget.readAloud("She sells sea shells by the sea shore"))
        val report = LabAttemptEvaluator.evaluate(request, features(4_000, 3_000), transcript = null)
        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        assertNull("no transcript → nothing to check against the script, so no score", report.overallScore)
        assertTrue(report.partialReason!!.contains("checking your words against the script"))
    }

    @Test
    fun transcriptThatCannotCoverTheRecording_isNotEvaluated() {
        val f = features(30_000, 20_000)
        assertEquals(TranscriptUse.INCOMPLETE, LabAttemptEvaluator.transcriptUse(f, "hello there friend"))
        assertEquals(TranscriptUse.NONE, LabAttemptEvaluator.transcriptUse(f, ""))
        assertEquals(TranscriptUse.USED, LabAttemptEvaluator.transcriptUse(features(20_000, 16_000), transcript42))

        val report = LabAttemptEvaluator.evaluate(answerRequest, f, "hello there friend")
        val engine = report.engineResult as AttemptEvaluationResult.Valid
        assertTrue(engine.allMetrics.keys.none { it in MetricEngine.TRANSCRIPT_METRICS })
        assertEquals("", report.session!!.transcript)
        assertTrue(report.partialReason!!.contains("covered only part of your recording"))
    }

    // ── Inconclusive and unreadable ────────────────────────────────────────────

    @Test
    fun inconclusiveAttempt_hasNoScoreNoSession_andCarriesTheEnginesAdvice() {
        val report = LabAttemptEvaluator.evaluate(answerRequest, features(1_000, 800), transcript42)

        assertEquals(CoachEvaluationState.INSUFFICIENT_EVIDENCE, report.evaluationState)
        val engine = report.engineResult as AttemptEvaluationResult.Inconclusive
        assertNull(report.overallScore)
        assertNull("inconclusive attempts are never saved", report.session)
        assertTrue(report.statusMessage.contains(engine.advice.advice))
        assertTrue(report.statusMessage.contains(engine.reason.userMessage))
        assertFalse(report.validation.isValidForEvaluation)
        assertEquals(EvidenceIssueCode.DURATION_TOO_SHORT, report.validation.primaryIssueCode)
    }

    @Test
    fun unreadableRecording_isReportedAsNotEvaluated() {
        val report = LabAttemptEvaluator.evaluate(answerRequest, features = null, transcript = transcript42)
        assertEquals(CoachEvaluationState.ANALYSIS_FAILED, report.evaluationState)
        assertNull(report.overallScore)
        assertNull(report.session)
        assertNull(report.engineResult)
    }

    // ── Measuring recordings ───────────────────────────────────────────────────

    /** (durationMs, amplitude) segments of a 220 Hz tone over faint noise, as interleaved PCM16. */
    private fun pcm(rate: Int, channels: Int, vararg segments: Pair<Int, Double>): ShortArray {
        val out = ArrayList<Short>()
        var n = 0
        var seed = 12345L
        for ((ms, amp) in segments) {
            repeat(rate * ms / 1000) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                val rnd = (seed % 2001 - 1000) / 1000.0
                val v = ((amp * sin(2 * PI * 220.0 * n / rate) + 0.0005 * rnd) * 32767.0).coerceIn(-32768.0, 32767.0)
                repeat(channels) { out.add(v.toInt().toShort()) }
                n++
            }
        }
        return out.toShortArray()
    }

    private fun wav(samples: ShortArray, rate: Int, channels: Int): ByteArray {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { data.putShort(it) }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples.size * 2); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(channels.toShort()); putInt(rate)
            putInt(rate * channels * 2); putShort((channels * 2).toShort()); putShort(16)
            put("data".toByteArray()); putInt(samples.size * 2)
        }
        return ByteArrayOutputStream().apply { write(header.array()); write(data.array()) }.toByteArray()
    }

    private val speechLike = arrayOf(500 to 0.0, 2_000 to 0.3, 1_200 to 0.0, 1_500 to 0.3, 500 to 0.0)

    @Test
    fun wav44kStereo_isMeasuredLikeTheSame16kMonoSpeech() {
        val reference = StreamingFeatureExtractor.analyse(pcm(16_000, 1, *speechLike))
        val measured = WavPcmReader.analyse(ByteArrayInputStream(wav(pcm(44_100, 2, *speechLike), 44_100, 2)))!!

        assertEquals(reference.durationMs.toDouble(), measured.durationMs.toDouble(), 25.0)
        assertEquals(reference.voicedDurationMs.toDouble(), measured.voicedDurationMs.toDouble(), 100.0)
        assertEquals(1, measured.pauseCount)
        assertEquals(reference.pauseCount, measured.pauseCount)
    }

    @Test
    fun pcmSink_downmixesAndResamplesTo16k() {
        val sink = PcmFeatureSink(sourceRateHz = 48_000, channels = 2)
        assertEquals(16_000, sink.analysisRateHz)
        sink.accept(pcm(48_000, 2, 1_000 to 0.3))
        assertEquals(16_000L, sink.analysedSamples)
        assertEquals(1_000L, sink.finish().durationMs)
        assertEquals(8_000, PcmFeatureSink(8_000, 1).analysisRateHz)
    }

    @Test
    fun nonWavInput_isNotGuessed() {
        assertNull(WavPcmReader.analyse(ByteArrayInputStream("definitely not audio".toByteArray())))
        assertNull(WavPcmReader.analyse(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun recordedWavFile_endToEnd_givesAudioOnlyEngineFeedback() {
        val file = File.createTempFile("lab_attempt_", ".wav").apply {
            writeBytes(wav(pcm(16_000, 1, *speechLike), 16_000, 1))
            deleteOnExit()
        }
        val measured = RecordedAudioAnalyzer.analyse(file)
        assertNotNull(measured)
        val report = LabAttemptEvaluator.evaluate(answerRequest.copy(localMediaPath = file.absolutePath), measured, transcript = "")
        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        assertTrue(report.engineResult is AttemptEvaluationResult.Valid)
        assertEquals(file.absolutePath, report.session!!.localMediaPath)
        file.delete()
    }
}
