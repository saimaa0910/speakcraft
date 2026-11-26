package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.RecommendationEngine
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.ui.components.TutorialOverlay
import com.example.ui.components.TutorialOverlayCatalog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1024dp-h768dp")
class SpeakCraftTabletLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ensureTestFirebaseApp(ctx)
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun `tutorial overlay on tablet places benefits and usage steps side by side`() {
        val steps = TutorialOverlayCatalog.coreWelcomeTourSteps()

        composeRule.setContent {
            SpeakCraftTheme {
                TutorialOverlay(
                    visible = true,
                    steps = steps,
                    onDismiss = {},
                    onComplete = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tutorial_overlay_card").assertIsDisplayed()
        val benefitsNode = composeRule.onNodeWithText("Benefits — What You'll Improve").assertIsDisplayed()
        val usageNode = composeRule.onNodeWithText("How to Use It").assertIsDisplayed()

        val benefitsBounds = benefitsNode.getBoundsInRoot()
        val usageBounds = usageNode.getBoundsInRoot()
        assertTrue(
            "Expected Benefits left (${benefitsBounds.left}) < Usage left (${usageBounds.left}) on tablet",
            benefitsBounds.left < usageBounds.left
        )
    }

    @Test
    fun `dashboard on tablet renders 4-up metric tiles in a single row and hero beside routine`() {
        val recommendation = RecommendationEngine.recommendNextAction(
            profile = UserProfile(userId = "u_tab", displayName = "Morgan", streakDays = 7L),
            sessions = emptyList(),
            learningProgress = emptyList(),
            todayActivity = null
        )

        composeRule.setContent {
            SpeakCraftTheme {
                DashboardScreen(
                    profile = UserProfile(userId = "u_tab", displayName = "Morgan", streakDays = 7L),
                    sessions = emptyList(),
                    vocabulary = emptyList(),
                    dailyActivities = emptyList(),
                    learningProgress = emptyList(),
                    realWorldTasks = emptyList(),
                    recommendedNextAction = recommendation,
                    isAdmin = false,
                    todayDateKey = "2026-11-18",
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()

        val heroAction = composeRule.onNodeWithTag("recommended_action_button").assertIsDisplayed()
        val planTitle = composeRule.onNodeWithText("Today's Practice Plan").assertIsDisplayed()
        assertTrue(
            "Expected hero action left (${heroAction.getBoundsInRoot().left}) < practice plan left (${planTitle.getBoundsInRoot().left})",
            heroAction.getBoundsInRoot().left < planTitle.getBoundsInRoot().left
        )

        val hoursBounds = composeRule.onNodeWithText("Practice Hours").assertIsDisplayed().getBoundsInRoot()
        val wordsBounds = composeRule.onNodeWithText("Words Learned").assertIsDisplayed().getBoundsInRoot()
        val recordingsBounds = composeRule.onNodeWithText("Recordings").assertIsDisplayed().getBoundsInRoot()
        val lessonsBounds = composeRule.onNodeWithText("Lessons Completed").assertIsDisplayed().getBoundsInRoot()
        assertTrue(hoursBounds.left < wordsBounds.left)
        assertTrue(wordsBounds.left < recordingsBounds.left)
        assertTrue(recordingsBounds.left < lessonsBounds.left)
    }

    @Test
    fun `analytics and settings screens use 2-column layout on expanded tablet`() {
        composeRule.setContent {
            SpeakCraftTheme {
                AnalyticsScreen(
                    profile = UserProfile(userId = "u_tab"),
                    sessions = listOf(
                        SpeakingSession(id = "s_tab_1", promptTitle = "Boardroom Pitch", fluencyScore = 82L)
                    ),
                    dailyActivities = emptyList(),
                    onNavigateToMentor = {}
                )
            }
        }
        composeRule.waitForIdle()

        val strongest = composeRule.onNodeWithText("Strongest Skill").assertIsDisplayed().getBoundsInRoot()
        val pace = composeRule.onNodeWithText("Weekly Pace Avg").assertIsDisplayed().getBoundsInRoot()
        assertTrue(strongest.left < pace.left)

        val trend = composeRule.onNodeWithText("Weekly Composite Speech Trend (1 sessions)").assertIsDisplayed().getBoundsInRoot()
        val objective = composeRule.onNodeWithText("Objective Speech Skills (Rolling Average)").assertIsDisplayed().getBoundsInRoot()
        assertTrue(trend.left < objective.left)
    }

    @Test
    fun `settings screen on tablet shows app info and tutorials switches side by side`() {
        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(firstUseTutorialsEnabled = true),
                    userEmail = "tablet@example.com",
                    isAdmin = false,
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onOpenAdminConsole = {},
                    onClearHistory = {},
                    onSignOut = {},
                    firstUseTutorialsEnabled = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_check_updates_button").assertDoesNotExist()
        val appInfoCard = composeRule.onNodeWithTag("settings_app_info_card").assertIsDisplayed().getBoundsInRoot()
        val tutorialSwitch = composeRule.onNodeWithTag("settings_first_use_tutorials_switch").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected app info card left (${appInfoCard.left}) < appearance/tutorials card left (${tutorialSwitch.left})",
            appInfoCard.left < tutorialSwitch.left
        )
    }

    @Test
    fun `speaking lab on tablet shows 2-pane studio without inline past recorded sessions`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val speakingVm = SpeakingAndLabsViewModel(
            SpeakCraftRepository(db),
            AudioRecorderManager(ctx)
        )
        val fakeVideo = File(ctx.cacheDir, "tablet_clip.mp4").apply { writeBytes(ByteArray(128) { 2 }) }
        speakingVm.attachRecordedVideoFile(fakeVideo, 15)

        composeRule.setContent {
            SpeakCraftTheme {
                SpeakingLabScreen(
                    speakingVm = speakingVm,
                    pastSessions = emptyList(),
                    onDeleteSession = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Past Recorded Sessions").assertDoesNotExist()
        val prompt = composeRule.onNodeWithTag("speaking_prompt_input").assertIsDisplayed().getBoundsInRoot()
        val archiveCard = composeRule.onNodeWithTag("studio_profile_recordings_card").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected capture studio left (${prompt.left}) < right archive/feedback pane (${archiveCard.left})",
            prompt.left < archiveCard.left
        )
    }

    @Test
    fun `launch overlay on tablet renders 144dp emblem and centered editorial typography`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SpeakCraftTheme(darkTheme = true) {
                com.example.ui.components.SpeakCraftLaunchOverlay(
                    visible = true,
                    cadence = com.example.ui.components.LaunchCadence.FIRST_LAUNCH,
                    reducedMotion = false,
                    onFinished = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(2260)
        composeRule.waitForIdle()

        val emblemBounds = composeRule.onNodeWithTag("launch_brand_emblem").assertIsDisplayed().getBoundsInRoot()
        val emblemWidth = emblemBounds.right - emblemBounds.left
        assertTrue("Expected tablet emblem >= 144.dp, got $emblemWidth", emblemWidth >= 144.dp)
        composeRule.onNodeWithTag("launch_sound_wave").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_wordmark").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_brand_tagline").assertIsDisplayed()
    }
}
