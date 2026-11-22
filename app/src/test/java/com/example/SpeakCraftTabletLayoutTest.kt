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
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.RecommendationEngine
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.update.ReleaseUpdateRepository
import com.example.data.update.UpdateBackupManager
import com.example.ui.components.TutorialOverlay
import com.example.ui.components.TutorialOverlayCatalog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppUpdateScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import com.google.firebase.FirebaseApp
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
                    updateEvaluation = null,
                    showUpdateBanner = false,
                    isAdmin = false,
                    todayDateKey = "2026-11-18",
                    onDismissUpdateBanner = {},
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
    fun `app update screen on tablet displays check updates and backups side by side`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val repo = SpeakCraftRepository(db)
        val backupMgr = UpdateBackupManager(ctx)
        val vm = UpdateAndAdminViewModel(
            repository = repo,
            updateRepository = ReleaseUpdateRepository(ctx, backupMgr),
            backupManager = backupMgr,
            prefsRepo = AppPreferencesRepository(ctx),
            currentUserId = "tablet_user_1"
        )

        composeRule.setContent {
            SpeakCraftTheme {
                AppUpdateScreen(
                    vm = vm,
                    prefs = AppPreferences(),
                    onSetUpdateChannel = {}
                )
            }
        }
        composeRule.waitForIdle()

        val checkBtn = composeRule.onNodeWithTag("check_for_updates_button").assertIsDisplayed()
        val backupBtn = composeRule.onNodeWithTag("create_backup_button").assertIsDisplayed()

        assertTrue(
            "Expected check button on left (${checkBtn.getBoundsInRoot().left}) and backup on right (${backupBtn.getBoundsInRoot().left})",
            checkBtn.getBoundsInRoot().left < backupBtn.getBoundsInRoot().left
        )

        checkBtn.performClick()
        composeRule.waitForIdle()
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
    fun `settings screen on tablet shows updates and tutorials switches side by side`() {
        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(firstUseTutorialsEnabled = true),
                    userEmail = "tablet@example.com",
                    isAdmin = false,
                    updateStatusSummary = "Up to date",
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onSetUpdateChannel = {},
                    onOpenUpdater = {},
                    onCreateBackupNow = {},
                    onOpenAdminConsole = {},
                    onClearHistory = {},
                    onSignOut = {},
                    firstUseTutorialsEnabled = true
                )
            }
        }
        composeRule.waitForIdle()

        val checkUpdates = composeRule.onNodeWithTag("settings_check_updates_button").assertIsDisplayed().getBoundsInRoot()
        val tutorialSwitch = composeRule.onNodeWithTag("settings_first_use_tutorials_switch").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected updates card left (${checkUpdates.left}) < appearance/tutorials card left (${tutorialSwitch.left})",
            checkUpdates.left < tutorialSwitch.left
        )
    }

    @Test
    fun `speaking lab on tablet shows 2-pane studio with recent recordings beside capture controls`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val speakingVm = SpeakingAndLabsViewModel(
            SpeakCraftRepository(db),
            LocalIntelligenceOrchestrator(ctx),
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

        val prompt = composeRule.onNodeWithTag("speaking_prompt_input").assertIsDisplayed().getBoundsInRoot()
        val pastSessionsHeader = composeRule.onNodeWithText("Past Recorded Sessions").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "Expected capture studio left (${prompt.left}) < Past Recorded Sessions pane (${pastSessionsHeader.left})",
            prompt.left < pastSessionsHeader.left
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
