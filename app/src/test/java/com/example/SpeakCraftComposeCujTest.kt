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
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiCoachService
import com.example.data.ai.RecommendationEngine
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.ApplicationRelease
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.update.UpdateBackupManager
import com.example.data.update.UpdateEvaluation
import com.example.data.youtube.YouTubeIntelligenceClient
import com.example.ui.components.FeedbackReportCard
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AppUpdateScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class SpeakCraftComposeCujTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(ctx)
        }
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun `dashboard renders recommended next action, update banner, and 15 modules`() {
        var navigatedRoute = ""
        val release = ApplicationRelease(
            id = "1_1_0",
            versionCode = 2L,
            versionName = "1.1.0",
            channel = "stable",
            releaseNotes = "Added faster pause rhythm calibration.",
            enabled = true
        )
        val evaluation = UpdateEvaluation(
            currentVersionCode = 1L,
            currentVersionName = "1.0.0",
            latestRelease = release,
            isUpdateAvailable = true,
            isMandatory = false,
            statusSummary = "Update v1.1.0 available"
        )
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
                    updateEvaluation = evaluation,
                    showUpdateBanner = true,
                    isAdmin = false,
                    todayDateKey = "2026-11-18",
                    onDismissUpdateBanner = {},
                    onNavigate = { navigatedRoute = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("update_banner_card").assertIsDisplayed()
        composeRule.onNodeWithTag("recommended_action_button").assertIsDisplayed().performClick()
        assertTrue(navigatedRoute.isNotBlank())
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
    fun `lesson detail displays creator attribution badge and marks lesson completed`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val speakingVm = SpeakingAndLabsViewModel(
            SpeakCraftRepository(db),
            GeminiCoachService(),
            AudioRecorderManager(ctx)
        )
        var toggledId = ""
        var toggledState = false

        composeRule.setContent {
            SpeakCraftTheme {
                LessonDetailScreen(
                    lessonId = "lesson_vinh_3stage_audit",
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

        composeRule.onNodeWithText("Creator Framework (@askvinh)").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("lesson_complete_toggle"))
        composeRule.onNodeWithTag("lesson_complete_toggle").performClick()
        assertEquals("lesson_vinh_3stage_audit", toggledId)
        assertTrue(toggledState)
    }

    @Test
    fun `admin console blocks unauthorized user`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val repo = SpeakCraftRepository(db)
        val backupMgr = UpdateBackupManager(ctx)
        val vm = UpdateAndAdminViewModel(
            repository = repo,
            updateRepository = com.example.data.update.ReleaseUpdateRepository(ctx, backupMgr),
            backupManager = backupMgr,
            prefsRepo = AppPreferencesRepository(ctx),
            youtubeClient = YouTubeIntelligenceClient(),
            geminiCoach = GeminiCoachService(),
            currentUserId = "regular_user_1"
        )

        composeRule.setContent {
            SpeakCraftTheme {
                AdminDashboardScreen(
                    vm = vm,
                    isAdmin = false,
                    currentUserId = "regular_user_1",
                    youtubeApiKey = ""
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Admin Access Restricted").assertIsDisplayed()
    }

    @Test
    fun `app update screen renders update check and backup controls`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val repo = SpeakCraftRepository(db)
        val backupMgr = UpdateBackupManager(ctx)
        val vm = UpdateAndAdminViewModel(
            repository = repo,
            updateRepository = com.example.data.update.ReleaseUpdateRepository(ctx, backupMgr),
            backupManager = backupMgr,
            prefsRepo = AppPreferencesRepository(ctx),
            youtubeClient = YouTubeIntelligenceClient(),
            geminiCoach = GeminiCoachService(),
            currentUserId = "regular_user_1"
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
        composeRule.onNodeWithTag("check_for_updates_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("create_backup_button").performScrollTo().assertIsDisplayed()
    }
}
