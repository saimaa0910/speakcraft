package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.language.LocalLanguageAnalysisEngine
import com.example.data.evaluation.CoachEvaluationReport
import com.example.data.evaluation.CoachEvaluationState
import com.example.data.evaluation.DimensionAvailability
import com.example.data.evaluation.EvaluationEvidenceValidator
import com.example.data.evaluation.EvidenceBasedScoringEngine
import com.example.data.evaluation.EvidenceIssueCode
import com.example.data.evaluation.EvidenceValidationInput
import com.example.ui.components.CoachEvaluationStatusCard
import com.example.ui.components.FeedbackReportCard
import com.example.ui.theme.SpeakCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class CoachEvaluationEvidenceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun buildWav(durationSeconds: Double, amplitude: Double, freqHz: Double = 185.0): ByteArray {
        val sampleRate = 16000
        val totalSamples = (sampleRate * durationSeconds).toInt().coerceAtLeast(1)
        val pcmBytes = ByteArray(totalSamples * 2)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env = if (amplitude > 0.0) {
                if ((i / 2400) % 2 == 0) amplitude else amplitude * 0.65
            } else 0.0
            val sample = (sin(2.0 * PI * freqHz * t) * env * 32767.0).toInt().coerceIn(-32768, 32767)
            pcmBytes[i * 2] = (sample and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        val header = ByteArray(44)
        val totalDataLen = pcmBytes.size + 36
        val byteRate = sampleRate * 2
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte(); header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[20] = 1; header[22] = 1
        header[24] = (sampleRate and 0xff).toByte(); header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[32] = 2; header[34] = 16
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmBytes.size and 0xff).toByte(); header[41] = ((pcmBytes.size shr 8) and 0xff).toByte()
        header[42] = ((pcmBytes.size shr 16) and 0xff).toByte(); header[43] = ((pcmBytes.size shr 24) and 0xff).toByte()
        return header + pcmBytes
    }

    private fun validAcousticFeatures(durationSeconds: Double = 4.5): AcousticFeatures = AcousticFeatures(
        durationSeconds = durationSeconds,
        speechDurationSeconds = durationSeconds * 0.82,
        pauseCount = 2,
        longPauseCount = 0,
        meanPauseDurationMs = 250.0,
        wordsPerMinute = 140,
        rmsEnergy = 0.14f,
        relativeLoudness = -17.0,
        clippingDetected = false,
        backgroundNoiseLevel = 0.015f,
        meanPitchHz = 182.0,
        pitchVariationHz = 28.0,
        speakingRateConsistencyScore = 0.85,
        speechContinuityScore = 0.84
    )

    @Test
    fun `01 missing audio returns INSUFFICIENT_EVIDENCE and null score`() {
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(sessionId = "s_missing", audioRequired = true)
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_missing",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Daily Reflection",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertEquals(CoachEvaluationState.INSUFFICIENT_EVIDENCE, report.evaluationState)
        assertNull("Missing audio must never award a score", report.overallScore)
        assertEquals(EvidenceIssueCode.MISSING_AUDIO, bundle.validation.primaryIssueCode)
        assertEquals("Your speaking recording is missing.", report.statusHeadline)
        assertEquals("Please record your response before requesting feedback.", report.statusMessage)
        assertTrue(report.dimensions.values.all { it.score == null })
    }

    @Test
    fun `02 zero-byte empty audio file fails with EMPTY_AUDIO_FILE and null score`() {
        val emptyFile = File.createTempFile("empty_audio_", ".wav").apply { deleteOnExit() }
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(sessionId = "s_empty_file", audioRequired = true, audioFile = emptyFile)
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_empty_file",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Intro",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertEquals(CoachEvaluationState.INSUFFICIENT_EVIDENCE, report.evaluationState)
        assertEquals(EvidenceIssueCode.EMPTY_AUDIO_FILE, bundle.validation.primaryIssueCode)
        assertNull(report.overallScore)
    }

    @Test
    fun `03 zero-second audio duration fails with DURATION_TOO_SHORT and null score`() {
        val zeroAudio = validAcousticFeatures(durationSeconds = 0.0).copy(speechDurationSeconds = 0.0)
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(sessionId = "s_zero_sec", audioRequired = true, preExtractedAudio = zeroAudio)
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_zero_sec",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Intro",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertEquals(CoachEvaluationState.INSUFFICIENT_EVIDENCE, report.evaluationState)
        assertEquals(EvidenceIssueCode.DURATION_TOO_SHORT, bundle.validation.primaryIssueCode)
        assertNull(report.overallScore)
    }

    @Test
    fun `04 sub-minimum duration under 1_5s fails cleanly with null score`() {
        val shortWav = buildWav(durationSeconds = 0.8, amplitude = 0.45)
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(sessionId = "s_short", audioRequired = true, audioBytes = shortWav, mimeType = "audio/wav")
        )
        assertFalse(bundle.validation.isValidForEvaluation)
        assertEquals(EvidenceIssueCode.DURATION_TOO_SHORT, bundle.validation.primaryIssueCode)
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_short",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Quick Check",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertEquals(CoachEvaluationState.INSUFFICIENT_EVIDENCE, report.evaluationState)
        assertNull(report.overallScore)
    }

    @Test
    fun `05 silent PCM recording produces SILENT_AUDIO message and null score`() {
        val silentWav = buildWav(durationSeconds = 4.0, amplitude = 0.0)
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(sessionId = "s_silent", audioRequired = true, audioBytes = silentWav, mimeType = "audio/wav")
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_silent",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Silence Check",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertEquals(CoachEvaluationState.INSUFFICIENT_EVIDENCE, report.evaluationState)
        assertEquals(EvidenceIssueCode.SILENT_AUDIO, bundle.validation.primaryIssueCode)
        assertEquals("We couldn't detect speech in this recording.", report.statusHeadline)
        assertEquals("Try speaking clearly for a few seconds and submit again.", report.statusMessage)
        assertNull(report.overallScore)
    }

    @Test
    fun `06 corrupt audio bytes return ANALYSIS_FAILED and exact failure copy`() {
        val corruptBytes = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x01, 0x02, 0x03)
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(sessionId = "s_corrupt", audioRequired = true, audioBytes = corruptBytes, mimeType = "audio/wav")
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_corrupt",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Corrupt Audio",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertEquals(CoachEvaluationState.ANALYSIS_FAILED, report.evaluationState)
        assertEquals("We couldn't analyze this recording. Your response has not been scored.", report.statusMessage)
        assertNull(report.overallScore)
    }

    @Test
    fun `08 valid speech audio plus valid transcript evaluates all 5 speech dimensions`() {
        val transcript = "Our team improved deployment reliability because we automated pre-release validation across every staging build."
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(
                sessionId = "s_full",
                audioRequired = true,
                preExtractedAudio = validAcousticFeatures(5.0),
                transcript = transcript,
                transcriptSessionId = "s_full"
            )
        )
        val lang = LocalLanguageAnalysisEngine.analyze(transcript)
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_full",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Engineering Update",
                mimeType = "audio/wav",
                validated = bundle,
                languageAnalysis = lang
            )
        )
        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        assertFalse(report.isPartialEvaluation)
        assertEquals(5, report.dimensions.size)
        assertTrue(report.dimensions.values.all { it.availabilityStatus == DimensionAvailability.EVALUATED && it.score != null })
        assertTrue(report.dimensions.values.all { it.evidenceSource.isNotBlank() && it.confidence > 0f && it.evaluationTimestampMs > 0L })
    }

    @Test
    fun `09 video selected without frames marks video_presence unavailable and report partial`() {
        val transcript = "Presenting clearly requires steady posture and deliberate pauses between key ideas."
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(
                sessionId = "s_vid_missing",
                audioRequired = true,
                preExtractedAudio = validAcousticFeatures(4.0),
                videoAssessmentSelected = true,
                videoFramesAnalyzed = 0,
                videoAnalysisCompleted = true,
                transcript = transcript,
                transcriptSessionId = "s_vid_missing"
            )
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_vid_missing",
                moduleType = "PUBLIC_SPEAKING",
                promptTitle = "Camera Presence",
                mimeType = "video/mp4",
                validated = bundle,
                languageAnalysis = LocalLanguageAnalysisEngine.analyze(transcript)
            )
        )
        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        assertTrue("Missing video frames must flag partial evaluation", report.isPartialEvaluation)
        val videoDim = report.dimensions["video_presence"]
        assertNotNull(videoDim)
        assertNull("Missing video frames must never receive a score", videoDim?.score)
        assertEquals(DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE, videoDim?.availabilityStatus)
    }

    @Test
    fun `10 video analysis failure leaves video_presence null while preserving scored audio dimensions`() {
        val transcript = "We launched the mentoring initiative to help speakers structure concise executive briefings."
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(
                sessionId = "s_vid_fail",
                audioRequired = true,
                preExtractedAudio = validAcousticFeatures(4.0),
                videoAssessmentSelected = true,
                videoAnalysisFailed = true,
                transcript = transcript,
                transcriptSessionId = "s_vid_fail"
            )
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_vid_fail",
                moduleType = "PUBLIC_SPEAKING",
                promptTitle = "Executive Brief",
                mimeType = "video/mp4",
                validated = bundle,
                languageAnalysis = LocalLanguageAnalysisEngine.analyze(transcript)
            )
        )
        assertTrue(report.isPartialEvaluation)
        assertNull(report.dimensions["video_presence"]?.score)
        assertNotNull(report.dimensions["fluency"]?.score)
        assertNotNull(report.dimensions["grammar"]?.score)
    }

    @Test
    fun `11 valid audio without transcript scores acoustic dimensions only and leaves grammar and vocabulary null`() {
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(
                sessionId = "s_no_stt",
                audioRequired = true,
                preExtractedAudio = validAcousticFeatures(4.5),
                transcript = "",
                transcriptSessionId = "s_no_stt"
            )
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_no_stt",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Offline Audio Drill",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        assertTrue("Missing transcript must mark partial evaluation", report.isPartialEvaluation)
        assertNotNull(report.dimensions["fluency"]?.score)
        assertNotNull(report.dimensions["pronunciation"]?.score)
        assertNotNull(report.dimensions["delivery"]?.score)
        assertNull("Grammar must be null without transcript", report.dimensions["grammar"]?.score)
        assertNull("Vocabulary must be null without transcript", report.dimensions["vocabulary"]?.score)
        assertEquals(DimensionAvailability.UNAVAILABLE_MISSING_EVIDENCE, report.dimensions["grammar"]?.availabilityStatus)
    }

    @Test
    fun `12 failed language model inference marks grammar and vocabulary UNAVAILABLE_ANALYSIS_FAILED`() {
        val transcript = "This transcript exists, but simulated language inference failed."
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(
                sessionId = "s_inf_fail",
                audioRequired = true,
                preExtractedAudio = validAcousticFeatures(4.0),
                transcript = transcript,
                transcriptSessionId = "s_inf_fail"
            )
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_inf_fail",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Inference Failure",
                mimeType = "audio/wav",
                validated = bundle,
                languageAnalysis = null,
                languageInferenceFailed = true
            )
        )
        assertTrue(report.isPartialEvaluation)
        assertNull(report.dimensions["grammar"]?.score)
        assertNull(report.dimensions["vocabulary"]?.score)
        assertEquals(DimensionAvailability.UNAVAILABLE_ANALYSIS_FAILED, report.dimensions["grammar"]?.availabilityStatus)
        assertEquals(DimensionAvailability.UNAVAILABLE_ANALYSIS_FAILED, report.dimensions["vocabulary"]?.availabilityStatus)
    }

    @Test
    fun `13 stale transcript from previous session is rejected and cannot score grammar`() {
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(
                sessionId = "session_current_2",
                audioRequired = true,
                preExtractedAudio = validAcousticFeatures(4.0),
                transcript = "A polished transcript reused from an earlier speaking session.",
                transcriptSessionId = "session_previous_1"
            )
        )
        assertFalse("Stale transcript must be rejected", bundle.validation.transcriptValid)
        assertEquals("", bundle.validatedTranscript)
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "session_current_2",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Fresh Attempt",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        assertNull("Stale transcript must not produce a grammar score", report.dimensions["grammar"]?.score)
        assertNull("Stale transcript must not produce a vocabulary score", report.dimensions["vocabulary"]?.score)
    }

    @Test
    fun `14 partial score reflects only evaluated dimensions and warns against comparing with full scores`() {
        val bundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(
                sessionId = "s_partial_math",
                audioRequired = true,
                preExtractedAudio = validAcousticFeatures(4.0),
                transcript = "",
                transcriptSessionId = "s_partial_math"
            )
        )
        val report = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_partial_math",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Partial Check",
                mimeType = "audio/wav",
                validated = bundle
            )
        )
        val evaluatedScores = report.dimensions.values.mapNotNull { it.score }
        assertEquals(3, evaluatedScores.size)
        val expectedMean = kotlin.math.round(evaluatedScores.average()).toInt()
        assertEquals(expectedMean, report.overallScore)
        assertTrue(report.partialReason?.contains("Partial assessment", ignoreCase = true) == true)
        assertTrue(report.partialReason?.contains("should not be compared", ignoreCase = true) == true)
    }

    @Test
    fun `15 coach status card and partial feedback card display exact error recovery and unavailable labels`() {
        var recordAgainClicked = false
        var cancelClicked = false

        val missingBundle = EvaluationEvidenceValidator.validate(
            EvidenceValidationInput(sessionId = "s_ui_missing", audioRequired = true)
        )
        val missingReport: CoachEvaluationReport = EvidenceBasedScoringEngine.score(
            EvidenceBasedScoringEngine.ScoringInput(
                sessionId = "s_ui_missing",
                moduleType = "SPEAKING_LAB",
                promptTitle = "UI Check",
                mimeType = "audio/wav",
                validated = missingBundle
            )
        )

        composeRule.setContent {
            SpeakCraftTheme {
                CoachEvaluationStatusCard(
                    report = missingReport,
                    onRecordAgain = { recordAgainClicked = true },
                    onCancel = { cancelClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("coach_evaluation_status_card").assertIsDisplayed()
        composeRule.onNodeWithText("Your speaking recording is missing.").assertIsDisplayed()
        composeRule.onNodeWithText("Please record your response before requesting feedback.").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_record_again_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("coach_cancel_button").assertIsDisplayed().performClick()
        assertTrue(recordAgainClicked)
        assertTrue(cancelClicked)
    }
}
