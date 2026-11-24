package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.FeatureSource
import com.example.speech.content.BuiltInPractice
import com.example.speech.content.PracticeCatalog
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.AttemptInput
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.session.AttemptOutcome
import com.example.speech.session.PracticeResult
import com.example.speech.session.StopReason
import com.example.speech.tier.SpeechTier
import com.example.ui.screens.CoachPracticeHome
import com.example.ui.screens.PracticeResultScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.CoachPracticeUiState
import com.example.ui.viewmodel.PracticeSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Coach practice screens render the engine's feedback on a phone-sized screen (EP-08). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class CoachPracticeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val catalog = PracticeCatalog(emptyList(), BuiltInPractice.ACTIVITIES)

    @Test
    fun `home lists quick practice and opens an activity`() {
        var opened: String? = null
        composeRule.setContent {
            SpeakCraftTheme {
                CoachPracticeHome(
                    state = CoachPracticeUiState(catalog = catalog, tier = SpeechTier.T2_ON_DEVICE_MIC),
                    onOpenActivity = { opened = it.id },
                    onOpenTechnique = {}
                )
            }
        }
        composeRule.onNodeWithText("Quick practice").assertIsDisplayed()
        composeRule.onNodeWithTag("tier_line").assertIsDisplayed()
        composeRule.onNodeWithText("Introduce yourself").performClick()
        assertEquals("quick-introduce-yourself", opened)
    }

    @Test
    fun `result shows strength, focus, mistakes, coach tips and transcript highlights`() {
        val activity = BuiltInPractice.ACTIVITIES.first()
        val features = AcousticFeatures(FeatureSource.PCM, 20_000, 15_000, emptyList(), List(50) { 0.1 + (it % 5) * 0.01 }, 0.005, 0.0, 500, 19_500)
        val input = AttemptInput.fromFeatures(
            attemptId = "att-ui",
            templateId = activity.templateId,
            features = features,
            transcript = "um so i think we basically moved the launch because uh the testing found um problems you know and he don't like delays",
            targetMinWords = activity.targetMinWords,
            targetMaxWords = activity.targetMaxWords
        )
        val evaluation = LocalIntelligencePipeline.evaluate(input) as AttemptEvaluationResult.Valid
        assertTrue(evaluation.issues.isNotEmpty())
        val result = PracticeResult(AttemptOutcome(evaluation, features, StopReason.USER, null, false), SpeechTier.T1_FULL_ON_DEVICE, null, false)
        var tryAgain = 0

        composeRule.setContent {
            SpeakCraftTheme {
                PracticeResultScreen(
                    state = CoachPracticeUiState(catalog = catalog, activity = activity, session = PracticeSessionState.Done(result)),
                    activity = activity,
                    result = result,
                    onAnswerSelfCheck = { _, _ -> },
                    onTryAgain = { tryAgain++ },
                    onNext = null,
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithTag("strength_card").assertIsDisplayed()
        composeRule.onNodeWithText(evaluation.feedback.strength.title).assertIsDisplayed()
        val list = composeRule.onNodeWithTag("practice_result")
        list.performScrollToNode(hasText("What to improve"))
        list.performScrollToNode(hasText(evaluation.issues.first().code.title))
        list.performScrollToNode(hasText("Coach tips"))
        list.performScrollToNode(hasText(evaluation.feedback.tips.first().title))
        list.performScrollToNode(hasTestTag("try_again"))
        composeRule.onNodeWithTag("try_again").performClick()
        assertEquals(1, tryAgain)
    }
}
