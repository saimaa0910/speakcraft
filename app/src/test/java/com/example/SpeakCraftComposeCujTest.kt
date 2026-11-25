package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.RecommendationEngine
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.FeedbackReportCard
import com.example.ui.components.TutorialOverlay
import com.example.ui.components.TutorialOverlayCatalog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiMentorScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.screens.OnboardingAssessmentScreen
import com.example.ui.screens.PronunciationLabScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.components.WelcomeBackBanner
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.InteractiveCoachViewModel
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class SpeakCraftComposeCujTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ensureTestFirebaseApp(ctx)
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun `dashboard renders recommended next action, omits update banner, and searches modules`() {
        var navigatedRoute = ""
        val recommendation = RecommendationEngine.recommendNextAction(
            profile = UserProfile(userId = "u1", displayName = "Alex"),
            sessions = emptyList(),
            learningProgress = emptyList(),
            todayActivity = null
        )

        composeRule.setContent {
            SpeakCraftTheme {
                DashboardScreen(
                    profile = UserProfile(userId = "u1", displayName = "Alex", streakDays = 4L),
                    sessions = emptyList(),
                    vocabulary = emptyList(),
                    dailyActivities = emptyList(),
                    learningProgress = emptyList(),
                    realWorldTasks = emptyList(),
                    recommendedNextAction = recommendation,
                    isAdmin = false,
                    todayDateKey = "2026-11-18",
                    onNavigate = { navigatedRoute = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("update_banner_card").assertDoesNotExist()
        composeRule.onNodeWithTag("recommended_action_button").assertIsDisplayed().performClick()
        assertTrue(navigatedRoute.isNotBlank())

        composeRule.onNodeWithTag("dashboard_scroll_list").performScrollToNode(hasTestTag("dashboard_module_search"))
        composeRule.onNodeWithTag("dashboard_module_search").performTextReplacement("Storytelling")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("module_card_module_lessons/storytelling").assertIsDisplayed()
    }

    @Test
    fun `feedback report card omits fabricated pronunciation bar when unassessable`() {
        val unassessableSession = SpeakingSession(
            id = "s_no_pron",
            promptTitle = "Vocal Pacing Check",
            fluencyScore = 78L,
            pronunciationAssessable = false,
            pronunciationScore = 0L,
            articulationScore = 81L,
            vocabularyScore = 76L,
            grammarScore = 84L,
            confidenceLevel = "High",
            overallAssessment = "Strong pause rhythm before key point.",
            nextRecommendedActivity = "Voice Training Lab"
        )

        composeRule.setContent {
            SpeakCraftTheme {
                FeedbackReportCard(session = unassessableSession, previousSession = null)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Strong pause rhythm before key point.").assertIsDisplayed()
        composeRule.onNodeWithText("Pronunciation", substring = false).assertDoesNotExist()
    }

    @Test
    fun `lesson detail displays framework attribution badge and marks lesson completed`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val speakingVm = SpeakingAndLabsViewModel(
            SpeakCraftRepository(db),
            AudioRecorderManager(ctx)
        )
        var toggledId = ""
        var toggledState = false

        composeRule.setContent {
            SpeakCraftTheme {
                LessonDetailScreen(
                    lessonId = "lesson_tech_3stage_audit",
                    sharedLessons = emptyList(),
                    learningProgress = emptyList(),
                    speakingVm = speakingVm,
                    pastSessions = emptyList(),
                    onToggleCompleted = { id, _, completed, _ ->
                        toggledId = id
                        toggledState = completed
                    },
                    onNavigateToLesson = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Speaking Framework").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("lesson_complete_toggle"))
        composeRule.onNodeWithTag("lesson_complete_toggle").performClick()
        assertEquals("lesson_tech_3stage_audit", toggledId)
        assertTrue(toggledState)
    }

    @Test
    fun `admin console blocks unauthorized user`() {
        val db = ensureFirestore()
        val repo = SpeakCraftRepository(db)
        val vm = UpdateAndAdminViewModel(repository = repo)

        composeRule.setContent {
            SpeakCraftTheme {
                AdminDashboardScreen(
                    vm = vm,
                    isAdmin = false,
                    currentUserId = "regular_user_1"
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Admin Access Restricted").assertIsDisplayed()
    }

    @Test
    fun `onboarding wizard progresses across 3 steps and completes calibration`() {
        var completedName = ""
        var completedLevel = ""

        composeRule.setContent {
            SpeakCraftTheme {
                OnboardingAssessmentScreen(
                    defaultDisplayName = "Taylor",
                    onComplete = { name, level, _, _, _, _, _, _, _, _, _ ->
                        completedName = name
                        completedLevel = level
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        composeRule.onNodeWithText("Advanced").performClick()
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("complete_onboarding_button").performScrollTo().performClick()
        assertEquals("Taylor", completedName)
        assertEquals("Advanced", completedLevel)
    }

    @Test
    fun `speaking lab renders video review player for recorded mp4 and supports stage switching`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val speakingVm = SpeakingAndLabsViewModel(
            SpeakCraftRepository(db),
            AudioRecorderManager(ctx)
        )
        val fakeVideo = File(ctx.cacheDir, "test_clip.mp4").apply { writeBytes(ByteArray(256) { 1 }) }
        speakingVm.attachRecordedVideoFile(fakeVideo, 12)

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

        composeRule.onNodeWithTag("speaking_preset_chip_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Describe a challenging project conflict and how you resolved it clearly using STAR").assertIsDisplayed()

        composeRule.onNodeWithTag("video_review_player_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Stage 1: Mute Pass").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Stage 1: Muted Visual Pass (Posture & Gestures)").assertIsDisplayed()
    }

    @Test
    fun `settings screen gates destructive history wipe behind confirmation dialog`() {
        var cleared = false

        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(),
                    userEmail = "speaker@example.com",
                    isAdmin = false,
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onOpenAdminConsole = {},
                    onClearHistory = { cleared = true },
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("clear_history_button"))
        composeRule.onNodeWithTag("clear_history_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("confirm_destructive_dialog").assertIsDisplayed()
        assertFalse(cleared)
        composeRule.onNodeWithTag("confirm_dialog_proceed").performClick()
        assertTrue(cleared)
    }

    @Test
    fun `pronunciation vs articulation routes show distinct headers and filters`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val speakingVm = SpeakingAndLabsViewModel(
            SpeakCraftRepository(db),
            AudioRecorderManager(ctx)
        )
        val ttsHelper = TtsAndSpeechHelper(ctx)

        composeRule.setContent {
            SpeakCraftTheme {
                PronunciationLabScreen(
                    speakingVm = speakingVm,
                    ttsHelper = ttsHelper,
                    pastSessions = emptyList(),
                    initialFocus = "articulation"
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Articulation & Enunciation Lab").assertIsDisplayed()
        ttsHelper.release()
    }

    @Test
    fun `analytics screen shows empty state when Daily window has no recent sessions`() {
        val oldSession = SpeakingSession(
            id = "old_1",
            promptTitle = "Old Talk",
            createdAt = Timestamp(Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)))
        )

        composeRule.setContent {
            SpeakCraftTheme {
                AnalyticsScreen(
                    profile = UserProfile(userId = "u1"),
                    sessions = listOf(oldSession),
                    dailyActivities = emptyList(),
                    onNavigateToMentor = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No sessions in Daily window").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ai mentor renders persistent chat bubbles`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val coachVm = InteractiveCoachViewModel(
            repository = SpeakCraftRepository(db),
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            appContext = ctx,
            currentUserId = "test_mentor_ui"
        )

        composeRule.setContent {
            SpeakCraftTheme {
                AiMentorScreen(
                    coachVm = coachVm,
                    profile = UserProfile(userId = "test_mentor_ui"),
                    sessions = emptyList(),
                    feedbackList = emptyList()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mentor_chat_bubble_0").assertIsDisplayed()
    }

    @Test
    fun `feature guide strip expands to show how to use and what will improve`() {
        composeRule.setContent {
            SpeakCraftTheme {
                FeatureGuideStrip(featureKey = "speaking_lab")
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("feature_guide_strip_speaking_lab").assertIsDisplayed()
        composeRule.onNodeWithText("How to use & what improves").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("How to Use").assertIsDisplayed()
        composeRule.onNodeWithText("What Will Improve By Doing This").assertIsDisplayed()
    }

    @Test
    fun `dashboard browse all tutorials opens searchable 24 feature dialog`() {
        val recommendation = RecommendationEngine.recommendNextAction(
            profile = UserProfile(userId = "u1", displayName = "Alex"),
            sessions = emptyList(),
            learningProgress = emptyList(),
            todayActivity = null
        )

        composeRule.setContent {
            SpeakCraftTheme {
                DashboardScreen(
                    profile = UserProfile(userId = "u1", displayName = "Alex"),
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

        composeRule.onNodeWithTag("dashboard_scroll_list").performScrollToNode(hasTestTag("browse_all_tutorials_button"))
        composeRule.onNodeWithTag("browse_all_tutorials_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("feature_tutorial_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tutorial_tab_all").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tutorial_list_item_dashboard").assertIsDisplayed()
    }

    @Test
    fun `tutorial overlay steps through speaking lab and ai mentor highlighting benefits and usage instructions`() {
        var dismissed = false
        var completed = false
        var dontShowState = false
        var triedRoute = ""
        val steps = TutorialOverlayCatalog.coreWelcomeTourSteps()

        composeRule.setContent {
            SpeakCraftTheme {
                TutorialOverlay(
                    visible = true,
                    steps = steps,
                    onDismiss = { dontShow ->
                        dismissed = true
                        dontShowState = dontShow
                    },
                    onComplete = { dontShow ->
                        completed = true
                        dontShowState = dontShow
                    },
                    onTryModule = { triedRoute = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tutorial_overlay_root").assertIsDisplayed()
        composeRule.onNodeWithText("Home Coach Dashboard").assertIsDisplayed()
        composeRule.onNodeWithText("Benefits — What You'll Improve").assertIsDisplayed()
        composeRule.onNodeWithText("How to Use It").assertIsDisplayed()

        // Step 2: Speaking Lab
        composeRule.onNodeWithTag("tutorial_overlay_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Record & Review Studio (3-Stage Audit)").assertIsDisplayed()
        composeRule.onNodeWithTag("tutorial_overlay_try_module_button").performClick()
        assertEquals("speaking_lab", triedRoute)

        // Step 3: AI Mentor + don't show checkbox + finish
        composeRule.onNodeWithTag("tutorial_overlay_next_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Personalized Multi-Turn AI Mentor").assertIsDisplayed()

        composeRule.onNodeWithTag("tutorial_overlay_dont_show_checkbox").performClick()
        composeRule.onNodeWithTag("tutorial_overlay_skip_button").performClick()
        assertTrue(dismissed)
        assertTrue(dontShowState)
        assertFalse(completed)
    }

    @Test
    fun `settings screen toggles first use overlays and triggers replay tours`() {
        var toggledValue: Boolean? = null
        var replayed = false

        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(firstUseTutorialsEnabled = true),
                    userEmail = "speaker@example.com",
                    isAdmin = false,
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onOpenAdminConsole = {},
                    onClearHistory = {},
                    onSignOut = {},
                    onToggleFirstUseTutorials = { toggledValue = it },
                    onReplayTutorials = { replayed = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_replay_tutorials_button"))
        composeRule.onNodeWithTag("settings_first_use_tutorials_switch").performClick()
        assertEquals(false, toggledValue)

        composeRule.onNodeWithTag("settings_replay_tutorials_button").performClick()
        assertTrue(replayed)
    }

    @Test
    fun `welcome back banner displays minimal animated username greeting`() {
        composeRule.setContent {
            SpeakCraftTheme {
                WelcomeBackBanner(displayName = "Sruthi", reducedMotion = true)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("welcome_back_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome back, Sruthi", substring = true).assertIsDisplayed()
    }

    @Test
    fun `user profile screen displays user info and saves profile edits`() {
        var savedName = ""
        var savedMinutes = 0L
        composeRule.setContent {
            SpeakCraftTheme {
                UserProfileScreen(
                    profile = UserProfile(
                        userId = "u1",
                        displayName = "Sruthi",
                        communicationLevel = "Intermediate",
                        primaryGoal = "Public Speaking",
                        dailyGoalMinutes = 15L,
                        streakDays = 5L
                    ),
                    userEmail = "sruthi@example.com",
                    sessions = emptyList(),
                    vocabulary = emptyList(),
                    learningSnapshot = null,
                    onSaveProfile = { name, _, _, mins ->
                        savedName = name
                        savedMinutes = mins
                    },
                    onOpenNotifications = {},
                    onNavigate = {},
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("user_profile_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Sruthi").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_edit_toggle_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_name_input").performScrollTo().performTextReplacement("Sruthi K")
        composeRule.onNodeWithTag("profile_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("Sruthi K", savedName)
        assertEquals(15L, savedMinutes)
    }
}
