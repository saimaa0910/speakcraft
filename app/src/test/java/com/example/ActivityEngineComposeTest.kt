package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.activity.ActivityEngine
import com.example.data.activity.UniversalEvaluationEngine
import com.example.data.model.UserProfile
import com.example.ui.screens.ActivityDiscoveryScreen
import com.example.ui.screens.ActivityHubTab
import com.example.ui.screens.ActivityScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.ActivityEngineViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActivityEngineComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun createOfflineViewModel(): ActivityEngineViewModel {
        val engine = ActivityEngine(
            evaluationEngine = UniversalEvaluationEngine(aiClientOverride = { _, _, _, _ -> null })
        )
        return ActivityEngineViewModel(
            activityRepository = null,
            activityEngine = engine,
            geminiCoach = null,
            audioRecorder = null,
            userId = "test_user_ui"
        )
    }

    @Test
    fun universalActivityScreen_invalidId_rendersSafeRecoveryWithoutCrash() {
        val vm = createOfflineViewModel()
        var backClicked = false

        composeRule.setContent {
            SpeakCraftTheme(darkTheme = false) {
                ActivityScreen(
                    activityId = "invalid_broken_id",
                    vm = vm,
                    ttsHelper = null,
                    onOpenNextActivity = {},
                    onBackToCatalogue = { backClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("invalid_activity_card").assertIsDisplayed()
        composeRule.onNodeWithTag("invalid_activity_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun universalActivityScreen_fullStartSubmitFeedbackAndContinueFlow_works() {
        val vm = createOfflineViewModel()
        var openedNextId = ""

        composeRule.setContent {
            SpeakCraftTheme(darkTheme = false) {
                ActivityScreen(
                    activityId = "m01_daily_english_speaking",
                    vm = vm,
                    ttsHelper = null,
                    onOpenNextActivity = { openedNextId = it },
                    onBackToCatalogue = {}
                )
            }
        }

        // Step 1: Ready stage displays activity title & start button
        composeRule.onNodeWithTag("activity_title_text").assertIsDisplayed()
        composeRule.onNodeWithText("Daily English Speaking").assertIsDisplayed()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_start_button"))
        composeRule.onNodeWithTag("activity_start_button").performClick()

        // Step 2: Active stage renders template interaction panel and confidence slider
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_response_input"))
        composeRule.onNodeWithTag("activity_response_input").performTextReplacement(
            "Today I led our weekly design review and explained three clear action items for the team. First, we aligned on user feedback. Second, we set a Friday checkpoint. Finally, I confirmed every owner."
        )

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_confidence_slider"))
        composeRule.onNodeWithTag("activity_confidence_slider").assertIsDisplayed()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_submit_button"))
        composeRule.onNodeWithTag("activity_submit_button").performClick()

        // Step 3: Completed stage displays structured 6-section feedback card & Retry / Continue
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("activity_evaluation_feedback_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_evaluation_feedback_card"))
        composeRule.onNodeWithTag("activity_evaluation_feedback_card").assertIsDisplayed()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_continue_next_button"))
        composeRule.onNodeWithTag("activity_continue_next_button").performClick()
        assertTrue(openedNextId.isNotBlank())
    }

    @Test
    fun universalActivityScreen_rendersConversationTemplateInDarkTheme() {
        val vm = createOfflineViewModel()

        composeRule.setContent {
            SpeakCraftTheme(darkTheme = true) {
                ActivityScreen(
                    activityId = "m05_ford_framework",
                    vm = vm,
                    ttsHelper = null,
                    onOpenNextActivity = {},
                    onBackToCatalogue = {}
                )
            }
        }

        composeRule.onNodeWithTag("activity_title_text").assertIsDisplayed()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_start_button"))
        composeRule.onNodeWithTag("activity_start_button").performClick()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("activity_dialogue_input"))
        composeRule.onNodeWithTag("activity_dialogue_input").assertIsDisplayed()
    }

    @Test
    fun activityDiscoveryScreen_supportsSearchDailyPlanPathsAndAnalyticsTabs() {
        val vm = createOfflineViewModel()
        val profile = UserProfile(displayName = "Sruthi", communicationLevel = "Intermediate")
        var clickedActivity = ""

        composeRule.setContent {
            SpeakCraftTheme(darkTheme = false) {
                ActivityDiscoveryScreen(
                    vm = vm,
                    profile = profile,
                    initialTab = ActivityHubTab.CATALOGUE,
                    onOpenActivity = { clickedActivity = it }
                )
            }
        }

        // Catalogue tab shows search input and filters down to FORD activity
        composeRule.onNodeWithTag("activity_discovery_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("activity_search_input").performTextReplacement("FORD")
        composeRule.onNodeWithTag("activity_discovery_list")
            .performScrollToNode(hasTestTag("activity_item_m05_ford_framework"))
        composeRule.onNodeWithTag("activity_item_m05_ford_framework").performClick()
        assertEquals("m05_ford_framework", clickedActivity)

        // Daily Plan tab shows 10/20/30/45/60 minute selectors
        composeRule.onNodeWithTag("activity_discovery_list")
            .performScrollToNode(hasTestTag("activity_hub_tab_daily_plan"))
        composeRule.onNodeWithTag("activity_hub_tab_daily_plan").performScrollTo().performClick()
        composeRule.onNodeWithTag("activity_discovery_list")
            .performScrollToNode(hasTestTag("daily_duration_10m"))
        composeRule.onNodeWithTag("daily_duration_10m").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_duration_20m").performScrollTo().performClick()

        // Learning Paths tab shows all 10 paths
        composeRule.onNodeWithTag("activity_hub_tab_paths").performScrollTo().performClick()
        composeRule.onNodeWithTag("activity_discovery_list")
            .performScrollToNode(hasTestTag("learning_path_card_path_01_beginner_english_fluency"))
        composeRule.onNodeWithTag("learning_path_card_path_01_beginner_english_fluency").assertIsDisplayed()

        // Analytics tab displays distinct objective vs confidence summary
        composeRule.onNodeWithTag("activity_discovery_list")
            .performScrollToNode(hasTestTag("activity_hub_tab_analytics"))
        composeRule.onNodeWithTag("activity_hub_tab_analytics").performScrollTo().performClick()
        composeRule.onNodeWithText("Avg Objective").assertIsDisplayed()
        composeRule.onNodeWithText("Avg Confidence").assertIsDisplayed()
    }
}
