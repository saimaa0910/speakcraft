package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.activity.ActivityAttemptRecord
import com.example.data.activity.ActivityCatalogue
import com.example.data.ai.RecommendationEngine
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.learning.DeviceRecordingsScanner
import com.example.data.learning.LearningProgressManager
import com.example.data.learning.LocalActivityAttempt
import com.example.data.learning.PersistedLearningSnapshot
import com.example.data.learning.PracticeCompletion
import com.example.data.learning.SavedPracticeRecording
import com.example.data.learning.SavedSpeakingRecording
import com.example.data.learning.UnifiedRecordedSession
import com.example.data.learning.UnifiedRecordingsCatalog
import com.example.data.learningpaths.DayLessonProgressRecord
import com.example.data.learningpaths.LearningPathCurriculumRegistry
import com.example.data.learningpaths.LearningPathsUserState
import com.example.data.learningpaths.PathEnrollmentRecord
import com.example.data.media.AudioRecorderManager
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserProfileRecordingsPolicyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(
                ctx,
                FirebaseOptions.Builder()
                    .setProjectId("speakcraft-test")
                    .setApplicationId("1:123:android:abc")
                    .setApiKey("test-key")
                    .build()
            )
        }
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun userProfileScreen_recordingsTab_aggregatesFiltersSearchesAndDeletesAllAppSources() {
        val lpProgram = LearningPathCurriculumRegistry.allPrograms.first()
        val lpLesson = lpProgram.modules.first().lessons.first()
        val sampleActivity = ActivityCatalogue.allActivities.first()

        val sessions = listOf(
            SpeakingSession(
                id = "sess_studio_1",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Executive Standup Video Take",
                durationSeconds = 42L,
                localMediaPath = "/tmp/studio_video.mp4",
                mediaType = "video/mp4",
                overallAssessment = "Strong vocal projection and clear takeaway."
            ),
            SpeakingSession(
                id = "sess_pron_2",
                moduleType = "PRONUNCIATION",
                promptTitle = "TH Contrast Minimal Pairs",
                durationSeconds = 18L,
                localMediaPath = "/tmp/pron_audio.m4a",
                mediaType = "audio/mp4",
                overallAssessment = "Clean alveolar contrast."
            )
        )

        val snapshot = PersistedLearningSnapshot(
            userId = "u_rec_1",
            practiceRecordings = listOf(
                SavedPracticeRecording(
                    id = "coach_rec_1",
                    activityId = "qp_pause",
                    title = "Coach Intentional Pause Drill",
                    timestampMs = 1_700_000_200_000L,
                    durationSeconds = 30,
                    localMediaPath = "/tmp/coach_audio.m4a",
                    score = 88,
                    passed = true,
                    summary = "Effective 1-second pause before main point."
                )
            ),
            activityAttempts = listOf(
                LocalActivityAttempt(
                    id = "act_att_1",
                    activityId = sampleActivity.activityId,
                    moduleId = sampleActivity.moduleId,
                    dateKey = "2026-11-26",
                    timestampMs = 1_700_000_100_000L,
                    durationSeconds = 55L,
                    objectiveScore = 84,
                    passed = true,
                    wordsPerMinute = 138,
                    recordedMediaPath = "/tmp/activity_audio.m4a"
                )
            )
        )

        val lpState = LearningPathsUserState(
            userId = "u_rec_1",
            enrollments = mapOf(
                lpProgram.programId to PathEnrollmentRecord(
                    programId = lpProgram.programId,
                    programTitle = lpProgram.title,
                    dayRecords = mapOf(
                        lpLesson.dayNumber to DayLessonProgressRecord(
                            lessonId = lpLesson.lessonId,
                            dayNumber = lpLesson.dayNumber,
                            completed = true,
                            lastScore = 91,
                            lastAudioPath = "/tmp/lp_day1.m4a",
                            audioDurationSeconds = 25,
                            coachSummary = "Well paced lesson response.",
                            lastUpdatedEpochMs = 1_700_000_300_000L
                        )
                    )
                )
            )
        )

        val deletedItems = mutableListOf<UnifiedRecordedSession>()

        composeRule.setContent {
            SpeakCraftTheme {
                UserProfileScreen(
                    profile = UserProfile(userId = "u_rec_1", displayName = "Aarav"),
                    userEmail = "aarav@example.com",
                    sessions = sessions,
                    vocabulary = emptyList(),
                    learningSnapshot = snapshot,
                    learningPathsState = lpState,
                    onSaveProfile = { _, _, _, _ -> },
                    onOpenNotifications = {},
                    onNavigate = {},
                    onSignOut = {},
                    onDeleteRecordedSession = { deletedItems += it }
                )
            }
        }
        composeRule.waitForIdle()

        // Overview tab displays total unified count = 5 and switches directly to Recordings tab on tap
        composeRule.onNodeWithTag("profile_tab_recordings").assertIsDisplayed()
        composeRule.onNodeWithText("Recordings (5)").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_recordings_stat_card").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_recordings_tab_content").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasText("Coach Intentional Pause Drill"))
        composeRule.onNodeWithText("Coach Intentional Pause Drill").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasText("Day ${lpLesson.dayNumber}: ${lpLesson.title}"))
        composeRule.onNodeWithText("Day ${lpLesson.dayNumber}: ${lpLesson.title}").assertIsDisplayed()

        // Filter by Coach Practice
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasTestTag("profile_recordings_filter_coach_practice"))
        composeRule.onNodeWithTag("profile_recordings_filter_coach_practice").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasText("Coach Intentional Pause Drill"))
        composeRule.onNodeWithText("Coach Intentional Pause Drill").assertIsDisplayed()
        composeRule.onNodeWithText("Day ${lpLesson.dayNumber}: ${lpLesson.title}").assertDoesNotExist()

        // Switch back to All and delete Coach Practice recording with confirmation
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasTestTag("profile_recordings_filter_all"))
        composeRule.onNodeWithTag("profile_recordings_filter_all").performClick()
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasTestTag("profile_recordings_search_input"))
        composeRule.onNodeWithTag("profile_recordings_search_input").performTextReplacement("Coach Intentional")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasTestTag("profile_delete_recording_coach_coach_rec_1"))
        composeRule.onNodeWithTag("profile_delete_recording_coach_coach_rec_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Delete Recording?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        assertEquals(1, deletedItems.size)
        assertEquals("coach_rec_1", deletedItems.first().backedPracticeRecordingId)
    }

    @Test
    fun speakingLabAndDashboard_doNotDisplayPastRecordedSessions_andRouteToProfileRecordings() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = ensureFirestore()
        val speakingVm = SpeakingAndLabsViewModel(
            SpeakCraftRepository(db),
            AudioRecorderManager(ctx)
        )
        val hiddenSessions = listOf(
            SpeakingSession(
                id = "hidden_clip_1",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Secret Past Clip Only In Profile",
                durationSeconds = 35L,
                localMediaPath = "/tmp/hidden_clip.m4a",
                overallAssessment = "Summary that must only show under Profile Recordings."
            )
        )

        var openedProfileRecordings = false

        composeRule.setContent {
            SpeakCraftTheme {
                SpeakingLabScreen(
                    speakingVm = speakingVm,
                    pastSessions = hiddenSessions,
                    onDeleteSession = {},
                    onOpenProfileRecordings = { openedProfileRecordings = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Past Recorded Sessions").assertDoesNotExist()
        composeRule.onNodeWithText("Secret Past Clip Only In Profile").assertDoesNotExist()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("studio_open_profile_recordings_button"))
        composeRule.onNodeWithTag("studio_open_profile_recordings_button").performClick()
        assertTrue(openedProfileRecordings)

        // Verify catalog helper still surfaces the hidden clip cleanly
        val catalogItems = UnifiedRecordingsCatalog.collectAll(hiddenSessions, null, null)
        assertEquals(1, catalogItems.size)
        assertEquals("Secret Past Clip Only In Profile", catalogItems.first().title)
    }

    @Test
    fun dashboardScreen_doesNotRenderLatestRecordingSummary_andLinksKpiToProfileRecordings() {
        val sessions = listOf(
            SpeakingSession(
                id = "dash_clip_1",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Boardroom Pitch Recording",
                durationSeconds = 40L,
                localMediaPath = "/tmp/boardroom.m4a",
                overallAssessment = "Sharp executive opener."
            )
        )
        var targetRoute = ""

        composeRule.setContent {
            SpeakCraftTheme {
                DashboardScreen(
                    profile = UserProfile(userId = "u1", displayName = "Maya"),
                    sessions = sessions,
                    vocabulary = emptyList(),
                    dailyActivities = emptyList(),
                    learningProgress = emptyList(),
                    realWorldTasks = emptyList(),
                    recommendedNextAction = RecommendationEngine.recommendNextAction(null, sessions, emptyList(), null),
                    isAdmin = false,
                    todayDateKey = "2026-11-26",
                    onNavigate = { targetRoute = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Latest Recording Review").assertDoesNotExist()
        composeRule.onNodeWithText("Boardroom Pitch Recording").assertDoesNotExist()
        composeRule.onNodeWithTag("dashboard_scroll_list")
            .performScrollToNode(hasTestTag("dashboard_recordings_kpi_card"))
        composeRule.onNodeWithTag("dashboard_recordings_kpi_card").performClick()
        assertEquals("profile_recordings", targetRoute)
    }

    @Test
    fun learningProgressManager_archivesAndDeletesCoachAndActivityRecordings() {
        val tempDir = File.createTempFile("lpm_rec", "").apply {
            delete()
            mkdirs()
        }
        try {
            val manager = LearningProgressManager(storageDir = tempDir)
            manager.loadForUser("u_arch", UserProfile(userId = "u_arch", displayName = "Ken"), emptyList())

            val act = ActivityCatalogue.allActivities.first()
            manager.recordActivityAttempt(
                activity = act,
                attempt = ActivityAttemptRecord(
                    id = "att_rec_1",
                    activityId = act.activityId,
                    objectiveScore = 82L,
                    passed = true,
                    recordedMediaPath = "/tmp/act_saved.m4a"
                ),
                attemptId = "att_rec_1"
            )
            manager.recordPracticeCompletion(
                PracticeCompletion(
                    attemptId = "cp_rec_1",
                    activityId = "qp_1",
                    title = "One Minute Pitch",
                    techniqueIds = listOf("tech_1"),
                    templateId = "TPL-PITCH",
                    timestampMs = 1_700_000_500_000L,
                    evaluationVersion = "1.0",
                    valid = true,
                    score = 90,
                    passed = true,
                    localMediaPath = "/tmp/cp_saved.mp4",
                    hasVideo = true,
                    durationSeconds = 45
                )
            )

            val all = UnifiedRecordingsCatalog.collectAll(
                speakingSessions = emptyList(),
                learningSnapshot = manager.state.value,
                learningPathsState = null
            )
            assertEquals(2, all.size)
            assertTrue(all.any { it.backedPracticeRecordingId == "cp_rec_1" && it.isVideo })
            assertTrue(all.any { it.backedActivityAttemptId == "att_rec_1" && !it.isVideo })

            manager.deletePracticeRecording("cp_rec_1")
            manager.deleteActivityAttemptMedia("att_rec_1")

            val afterDelete = UnifiedRecordingsCatalog.collectAll(
                speakingSessions = emptyList(),
                learningSnapshot = manager.state.value,
                learningPathsState = null
            )
            assertEquals(0, afterDelete.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun deviceScannerAndSpeakingArchive_recoverStrippedPathsAndSurfaceOfflineOrPreexistingClips() {
        val tempRoot = File.createTempFile("dev_rec_scan", "").apply {
            delete()
            mkdirs()
        }
        try {
            val lpmDir = File(tempRoot, "lpm").apply { mkdirs() }
            val recordingsDir = File(tempRoot, "recordings").apply { mkdirs() }
            val practiceDir = File(tempRoot, "practice").apply { mkdirs() }

            // Pre-existing or offline recordings saved on device storage
            val offlineStudioAudio = File(recordingsDir, "rec_1001.m4a").apply { writeBytes(ByteArray(256) { 1 }) }
            val legacyUnindexedVideo = File(recordingsDir, "speakcraft_vid_1002.mp4").apply { writeBytes(ByteArray(512) { 2 }) }
            val practiceAttemptDir = File(practiceDir, "cp_legacy_9").apply { mkdirs() }
            File(practiceAttemptDir, "audio.m4a").apply { writeBytes(ByteArray(128) { 3 }) }
            val claimedPracticeReplay = File(File(practiceDir, "cp_claimed_1").apply { mkdirs() }, "replay.mp4").apply {
                writeBytes(ByteArray(256) { 4 })
            }

            val scanned = DeviceRecordingsScanner.scanDirectories(recordingsDir, practiceDir)
            assertEquals(4, scanned.size)

            val manager = LearningProgressManager(storageDir = lpmDir)
            manager.loadForUser("u_offline", UserProfile(userId = "u_offline", displayName = "Aria"), emptyList())
            manager.recordSpeakingRecording(
                SavedSpeakingRecording(
                    id = "cloud_doc_stripped",
                    moduleType = "SPEAKING_LAB",
                    promptTitle = "Daily Video Recording",
                    timestampMs = 1_700_000_000_000L,
                    durationSeconds = 22,
                    localMediaPath = offlineStudioAudio.absolutePath,
                    mediaType = "audio/mp4",
                    transcript = "My local preserved transcript",
                    overallAssessment = "Measured delivery with steady pace.",
                    evaluated = true,
                    fluencyScore = 84
                )
            )
            manager.recordSpeakingRecording(
                SavedSpeakingRecording(
                    id = "offline_only_take",
                    moduleType = "PUBLIC_SPEAKING",
                    promptTitle = "Un-analyzed Keynote Take",
                    timestampMs = 1_700_000_100_000L,
                    durationSeconds = 15,
                    localMediaPath = "/tmp/offline_keynote.m4a",
                    mediaType = "audio/mp4",
                    evaluated = false
                )
            )
            manager.recordPracticeCompletion(
                PracticeCompletion(
                    attemptId = "cp_claimed_1",
                    activityId = "qp_2",
                    title = "Clarity Drill",
                    techniqueIds = listOf("tech_1"),
                    templateId = "TPL-CLARITY",
                    timestampMs = 1_700_000_200_000L,
                    evaluationVersion = "1.0",
                    valid = true,
                    score = 88,
                    passed = true,
                    localMediaPath = claimedPracticeReplay.absolutePath,
                    hasVideo = true,
                    durationSeconds = 19
                )
            )

            // Firestore emits session WITHOUT localMediaPath or transcript (stripped by T-04.2.1 security policy)
            val strippedFirestoreSession = SpeakingSession(
                id = "cloud_doc_stripped",
                moduleType = "SPEAKING_LAB",
                promptTitle = "Daily Video Recording",
                durationSeconds = 22L,
                localMediaPath = "",
                transcript = "",
                overallAssessment = "Measured delivery with steady pace."
            )

            val all = UnifiedRecordingsCatalog.collectAll(
                speakingSessions = listOf(strippedFirestoreSession),
                learningSnapshot = manager.state.value,
                learningPathsState = null,
                deviceMediaFiles = scanned
            )

            // Expected:
            // 1) strippedFirestoreSession paired with offlineStudioAudio (recovering path + transcript)
            // 2) offline_only_take (local-only SavedSpeakingRecording)
            // 3) cp_claimed_1 (Coach Practice claimed replay)
            // 4) legacyUnindexedVideo (discovered unclaimed video file)
            // 5) cp_legacy_9/audio.m4a (discovered unclaimed Coach Practice file)
            assertEquals(5, all.size)

            val paired = all.first { it.backedSpeakingSession?.id == "cloud_doc_stripped" }
            assertEquals(offlineStudioAudio.absolutePath, paired.localMediaPath)
            assertEquals("My local preserved transcript", paired.transcript)
            assertEquals("cloud_doc_stripped", paired.backedSpeakingRecordingId)

            assertTrue(all.any { it.title == "Un-analyzed Keynote Take" && it.backedSpeakingRecordingId == "offline_only_take" })
            assertTrue(all.any { it.localMediaPath == legacyUnindexedVideo.absolutePath && it.isVideo && it.backedDeviceFilePath == legacyUnindexedVideo.absolutePath })
            assertTrue(all.any { it.backedDeviceFilePath?.contains("cp_legacy_9") == true })

            // Overview shortcut opens Recordings tab & renders the offline + discovered recordings
            composeRule.setContent {
                SpeakCraftTheme {
                    UserProfileScreen(
                        profile = UserProfile(userId = "u_offline", displayName = "Aria"),
                        userEmail = "aria@example.com",
                        sessions = listOf(strippedFirestoreSession),
                        vocabulary = emptyList(),
                        learningSnapshot = manager.state.value,
                        onSaveProfile = { _, _, _, _ -> },
                        onOpenNotifications = {},
                        onNavigate = {},
                        onSignOut = {},
                        deviceMediaFiles = scanned
                    )
                }
            }
            composeRule.waitForIdle()
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("profile_overview_recordings_link"))
            composeRule.onNodeWithTag("profile_overview_recordings_link").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("profile_recordings_tab_content").assertIsDisplayed()
            composeRule.onNodeWithTag("profile_recordings_tab_content")
                .performScrollToNode(hasText("Un-analyzed Keynote Take"))
            composeRule.onNodeWithText("Un-analyzed Keynote Take").assertIsDisplayed()
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun speakingAndLabsViewModel_archivesVideoCaptureIntoLearningProgressManager() {
        val tempRoot = File.createTempFile("vm_arch", "").apply {
            delete()
            mkdirs()
        }
        try {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            val db = ensureFirestore()
            val repo = SpeakCraftRepository(db)
            val audioMgr = AudioRecorderManager(ctx)
            val lpm = LearningProgressManager(storageDir = File(tempRoot, "lpm").apply { mkdirs() })
            lpm.loadForUser("u_vm", UserProfile(userId = "u_vm", displayName = "Sam"), emptyList())

            val vm = SpeakingAndLabsViewModel(
                repository = repo,
                audioManager = audioMgr,
                learningProgressManager = lpm
            )
            vm.setRecordingContext("SPEAKING_LAB", "Daily Video Recording (Open Speak)")
            val videoFile = File(tempRoot, "speakcraft_vid_999.mp4").apply {
                writeBytes(ByteArray(1024) { 7 })
            }
            vm.attachRecordedVideoFile(videoFile, 28)

            // Allow viewModelScope coroutine on main looper to complete
            composeRule.waitForIdle()

            val archived = lpm.state.value.speakingRecordings
            assertEquals(1, archived.size)
            assertEquals("Daily Video Recording (Open Speak)", archived.first().promptTitle)
            assertEquals(videoFile.absolutePath, archived.first().localMediaPath)
            assertEquals("video/mp4", archived.first().mediaType)
            assertEquals(28, archived.first().durationSeconds)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun userProfileScreen_overviewQuickLinkOpensRecordingsTab() {
        composeRule.setContent {
            SpeakCraftTheme {
                UserProfileScreen(
                    profile = UserProfile(userId = "u_ov", displayName = "Noah"),
                    userEmail = "noah@example.com",
                    sessions = listOf(
                        SpeakingSession(
                            id = "s_ov_1",
                            moduleType = "SPEAKING_LAB",
                            promptTitle = "Morning Check-In Recording",
                            durationSeconds = 18L,
                            localMediaPath = "/tmp/morning.m4a",
                            overallAssessment = "Steady tone."
                        )
                    ),
                    vocabulary = emptyList(),
                    learningSnapshot = PersistedLearningSnapshot(userId = "u_ov"),
                    onSaveProfile = { _, _, _, _ -> },
                    onOpenNotifications = {},
                    onNavigate = {},
                    onSignOut = {},
                    initialTab = 0
                )
            }
        }
        composeRule.waitForIdle()
        // Recordings session list must NOT appear on Overview tab
        composeRule.onNodeWithText("Morning Check-In Recording").assertDoesNotExist()
        // Tap Quick Link in Overview -> opens Recordings tab and reveals session
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("profile_overview_recordings_link"))
        composeRule.onNodeWithTag("profile_overview_recordings_link").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_recordings_tab_content").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_recordings_tab_content")
            .performScrollToNode(hasText("Morning Check-In Recording"))
    }
}
