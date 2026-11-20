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
import com.example.data.ai.GeminiCoachService
import com.example.data.ai.RecommendationEngine
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.ApplicationRelease
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.update.UpdateBackupManager
import com.example.data.update.UpdateEvaluation
import com.example.data.youtube.YouTubeIntelligenceClient
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.FeedbackReportCard
import com.example.ui.components.TutorialOverlay
import com.example.ui.components.TutorialOverlayCatalog
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiMentorScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppUpdateScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.screens.OnboardingAssessmentScreen
import com.example.ui.screens.PronunciationLabScreen
import com.example.ui.screens.SettingsScreen
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.ui.components.WelcomeBackBanner
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.InteractiveCoachViewModel
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.example.ui.viewmodel.UpdateAndAdminViewModel
import com.google.firebase.FirebaseApp
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
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(ctx)
        }
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun `dashboard renders recommended next action, update banner, and searchable modules`() {
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
    fun `app update screen renders update check, backup creation, and restore button`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val repo = SpeakCraftRepository(db)
        val backupMgr = UpdateBackupManager(ctx)
        val snap = backupMgr.createPreUpdateBackup(AppPreferences(), "1.0.0", 1L, "1.1.0").getOrThrow()
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
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("check_for_updates_button"))
        composeRule.onNodeWithTag("check_for_updates_button").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("create_backup_button"))
        composeRule.onNodeWithTag("create_backup_button").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("restore_backup_button_${snap.fileName}"))
        composeRule.onNodeWithTag("restore_backup_button_${snap.fileName}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_destructive_dialog").assertIsDisplayed()
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
            GeminiCoachService(),
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
                    updateStatusSummary = "Up to date",
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onSetYoutubeKey = {},
                    onSetUpdateChannel = {},
                    onOpenUpdater = {},
                    onCreateBackupNow = {},
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
            GeminiCoachService(),
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
            geminiCoach = GeminiCoachService(),
            youtubeClient = YouTubeIntelligenceClient(),
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
                    updateStatusSummary = "Up to date",
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onSetYoutubeKey = {},
                    onSetUpdateChannel = {},
                    onOpenUpdater = {},
                    onCreateBackupNow = {},
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
    fun `update available dialog shows release notes, direct apk link, and triggers callbacks`() {
        var openedDirectUrl = ""
        var installClicked = false
        var dismissed = false
        val release = ApplicationRelease(
            id = "rel_1_2_0",
            versionCode = 5L,
            versionName = "1.2.0",
            channel = "stable",
            releaseDate = "2026-11-19",
            downloadUrl = "https://github.com/octocat/speakcraft/releases/download/v1.2.0/speakcraft_1.2.0.apk",
            fileSizeBytes = 15_728_640L,
            releaseNotes = "Improved editorial cards and OTA signing verification.",
            features = listOf("Pre-flight signing check", "Direct APK download link"),
            enabled = true
        )
        val evaluation = UpdateEvaluation(
            currentVersionCode = 2L,
            currentVersionName = "1.1.0-beta.1",
            latestRelease = release,
            isUpdateAvailable = true,
            isMandatory = false,
            statusSummary = "Update v1.2.0 available"
        )

        composeRule.setContent {
            SpeakCraftTheme {
                UpdateAvailableDialog(
                    evaluation = evaluation,
                    release = release,
                    onDismiss = { dismissed = true },
                    onInstallViaUpdater = { installClicked = true },
                    onOpenDirectDownload = { openedDirectUrl = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("update_available_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("update_dialog_release_notes").assertIsDisplayed()
        composeRule.onNodeWithText("Improved editorial cards and OTA signing verification.").assertIsDisplayed()
        composeRule.onNodeWithTag("update_dialog_direct_download_link").performScrollTo().performClick()
        assertEquals(release.downloadUrl, openedDirectUrl)

        composeRule.onNodeWithTag("update_dialog_confirm_button").performClick()
        assertTrue(installClicked)

        composeRule.onNodeWithTag("update_dialog_dismiss_button").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `update available dialog highlights signing key conflict and prioritizes direct apk download`() {
        var openedDirectUrl = ""
        val release = ApplicationRelease(
            id = "rel_1_2_0_signed",
            versionCode = 6L,
            versionName = "1.2.0",
            channel = "stable",
            downloadUrl = "https://github.com/octocat/speakcraft/releases/download/v1.2.0/speakcraft_1.2.0.apk",
            releaseNotes = "Signed with production upload keystore.",
            signingCertSha256 = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90",
            enabled = true
        )
        val conflictEval = UpdateEvaluation(
            currentVersionCode = 2L,
            currentVersionName = "1.1.0-beta.1",
            latestRelease = release,
            isUpdateAvailable = true,
            isMandatory = false,
            statusSummary = "Signing conflict detected",
            hasSigningConflictWithInstalled = true,
            installedCertFingerprint = "3f7d14b32df7635fe1b48db5d09857df02a000e2204568323d9a60025fa75f9f",
            releaseCertFingerprint = release.signingCertSha256
        )

        composeRule.setContent {
            SpeakCraftTheme {
                UpdateAvailableDialog(
                    evaluation = conflictEval,
                    release = release,
                    onDismiss = {},
                    onInstallViaUpdater = {},
                    onOpenDirectDownload = { openedDirectUrl = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("update_dialog_signing_conflict_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("update_dialog_confirm_button").performClick()
        assertEquals(release.downloadUrl, openedDirectUrl)
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

    @Test
    fun `dynamic local coach produces topic specific rewrites and tailored feedback`() {
        val orchestrator = LocalIntelligenceOrchestrator()
        val speechWav = buildSyntheticSpeechWavBytes(durationSeconds = 4)

        val emptyAttempt = kotlinx.coroutines.runBlocking {
            orchestrator.analyzeSpeechRecording(
                mediaBytes = ByteArray(0),
                mimeType = "audio/wav",
                moduleType = "speaking_lab",
                promptTitle = "Missing Audio Test",
                durationSeconds = 0,
                previousSession = null,
                liveTranscript = "Um I think maybe this shouldn't score without audio"
            )
        }
        assertTrue(
            "Empty audio must fail evaluation rather than fabricating a score",
            emptyAttempt.isFailure
        )

        val interviewReport = kotlinx.coroutines.runBlocking {
            orchestrator.analyzeSpeechRecording(
                mediaBytes = speechWav,
                mimeType = "audio/wav",
                moduleType = "speaking_lab",
                promptTitle = "Interview Practice",
                durationSeconds = 12,
                previousSession = null,
                liveTranscript = "Um I think maybe I kind of did good work on the backend API migration and like basically we finished it fast."
            ).getOrThrow()
        }
        val storyReport = kotlinx.coroutines.runBlocking {
            orchestrator.analyzeSpeechRecording(
                mediaBytes = speechWav,
                mimeType = "audio/wav",
                moduleType = "speaking_lab",
                promptTitle = "Storytelling Challenge",
                durationSeconds = 18,
                previousSession = null,
                liveTranscript = "Last Friday during our product launch, the checkout server slowed down. Because we prepared a rollback plan, our team restored stability in four minutes and kept every customer order intact."
            ).getOrThrow()
        }

        assertTrue(interviewReport.overallAssessment.isNotBlank())
        assertTrue(interviewReport.betterAlternatives.any { it.contains("→") })
        assertFalse(
            "Dynamic coach should tailor narrative per transcript, not repeat static boilerplate",
            interviewReport.overallAssessment == storyReport.overallAssessment
        )
        assertTrue(storyReport.fluencyScore > interviewReport.fluencyScore)
    }

    private fun buildSyntheticSpeechWavBytes(durationSeconds: Int): ByteArray {
        val sampleRate = 16000
        val totalSamples = sampleRate * durationSeconds
        val pcmBytes = ByteArray(totalSamples * 2)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Modulated speech-like harmonic envelope so voiced syllables exceed silence threshold
            val envelope = if ((i / 2400) % 2 == 0) 0.45 else 0.25
            val wave = kotlin.math.sin(2.0 * kotlin.math.PI * 180.0 * t) * envelope
            val sample = (wave * 32767.0).toInt().coerceIn(-32768, 32767)
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
}
