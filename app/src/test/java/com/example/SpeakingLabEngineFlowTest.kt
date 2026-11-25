package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.data.evaluation.CoachEvaluationState
import com.example.data.evaluation.LabAttemptEvaluator
import com.example.data.evaluation.LabPracticeTarget
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.UiState
import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.FeatureSource
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.ui.components.FeedbackReportCard
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import java.io.File

/**
 * The practice-lab screens' view model evaluates through the local intelligence engine and the feedback card
 * shows the engine's measured metrics — never a legacy score ring for free speaking.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class SpeakingLabEngineFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ensureTestFirebaseApp(ctx)
        return FirebaseFirestore.getInstance()
    }

    private fun features(durationMs: Long, voicedMs: Long) = AcousticFeatures(
        source = FeatureSource.PCM,
        durationMs = durationMs,
        voicedDurationMs = voicedMs,
        pauses = emptyList(),
        activeFrameRms = List((voicedMs / 20).toInt()) { 0.1 },
        noiseFloorRms = 0.005,
        clippingRatio = 0.0,
        firstVoicedMs = if (voicedMs > 0) 300L else null,
        lastVoicedMs = if (voicedMs > 0) durationMs - 300L else null
    )

    private fun viewModel(measured: AcousticFeatures?): Pair<SpeakingAndLabsViewModel, File> {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = SpeakingAndLabsViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            audioManager = AudioRecorderManager(ctx),
            recordingAnalyzer = { measured },
            evaluationDispatcher = Dispatchers.Unconfined
        )
        val clip = File(ctx.cacheDir, "engine_flow_clip.mp4").apply { writeBytes(ByteArray(256) { 1 }) }
        vm.attachRecordedVideoFile(clip, 20)
        shadowOf(Looper.getMainLooper()).idle()
        return vm to clip
    }

    private val transcript =
        "Yesterday my manager asked about the release and he don't think we are ready yet so I explained our " +
            "testing plan step by step and showed the results from last week which made the whole team feel " +
            "calmer about the launch date"

    @Test
    fun challengeAttempt_isEvaluatedByTheEngine_andTheCardShowsMeasuredMetrics() {
        val (vm, clip) = viewModel(features(20_000, 16_000))
        vm.submitForAiAnalysis(
            moduleType = "DAILY_CHALLENGE",
            promptTitle = "PREP Opinion Sprint",
            liveTranscriptOverride = transcript
        )
        shadowOf(Looper.getMainLooper()).idle()

        val report = vm.evaluationReport.value!!
        assertEquals(CoachEvaluationState.COMPLETED, report.evaluationState)
        val engine = report.engineResult as AttemptEvaluationResult.Valid
        assertEquals("TPL-STRUCT", engine.templateId) // PREP prompt → structure check
        assertNull(report.overallScore)
        val session = (vm.analysisState.value as UiState.Success).data
        assertEquals(LocalIntelligencePipeline.EVALUATION_VERSION, LabAttemptEvaluator.evaluationVersionOf(session))
        assertEquals(clip.absolutePath, session.localMediaPath)

        composeRule.setContent {
            SpeakCraftTheme { FeedbackReportCard(session = session, previousSession = null, evaluationReport = report) }
        }
        composeRule.onNodeWithTag("coach_engine_measurements").assertIsDisplayed()
        composeRule.onNodeWithTag("coach_engine_metric_M-STRUCT").assertIsDisplayed()
        composeRule.onNodeWithText("/100").assertDoesNotExist()
        composeRule.onNodeWithTag("feedback_tab_metrics").assertIsDisplayed()
        clip.delete()
    }

    @Test
    fun readAloudAttempt_showsTheEnginesRecognisedAsIntendedScore() {
        val (vm, clip) = viewModel(features(4_000, 3_000))
        vm.submitForAiAnalysis(
            moduleType = "PRONUNCIATION",
            promptTitle = "Sibilants",
            practiceTarget = LabPracticeTarget.readAloud("She sells sea shells by the sea shore"),
            liveTranscriptOverride = "she sells sea shells by the sea shore"
        )
        shadowOf(Looper.getMainLooper()).idle()

        val report = vm.evaluationReport.value!!
        assertEquals(100, report.overallScore)
        val session = (vm.analysisState.value as UiState.Success).data
        composeRule.setContent {
            SpeakCraftTheme { FeedbackReportCard(session = session, previousSession = null, evaluationReport = report) }
        }
        composeRule.onNodeWithTag("coach_engine_score").assertIsDisplayed()
        composeRule.onNodeWithText("Recognised as intended").assertIsDisplayed()
        clip.delete()
    }

    @Test
    fun inconclusiveAttempt_isNotSaved_andShowsTheEnginesAdvice() {
        val (vm, clip) = viewModel(features(1_000, 800))
        vm.submitForAiAnalysis(moduleType = "SPEAKING_LAB", promptTitle = "Team Update", liveTranscriptOverride = transcript)
        shadowOf(Looper.getMainLooper()).idle()

        val report = vm.evaluationReport.value!!
        assertEquals(CoachEvaluationState.INSUFFICIENT_EVIDENCE, vm.evaluationState.value)
        assertNull(report.overallScore)
        assertNull(report.session)
        assertTrue(vm.analysisState.value is UiState.Error)
        assertTrue((report.engineResult as AttemptEvaluationResult.Inconclusive).advice.advice in report.statusMessage)
        clip.delete()
    }

    @Test
    fun unreadableRecording_isNotEvaluated() {
        val (vm, clip) = viewModel(null)
        vm.submitForAiAnalysis(moduleType = "SPEAKING_LAB", promptTitle = "Team Update")
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(CoachEvaluationState.ANALYSIS_FAILED, vm.evaluationState.value)
        assertNull(vm.evaluationReport.value!!.overallScore)
        clip.delete()
    }
}
