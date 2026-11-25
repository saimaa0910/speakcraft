package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.PendingStudioGeminiHandoff
import com.example.data.ai.PhoneGeminiBridge
import com.example.data.ai.StudioGeminiFeedbackEntry
import com.example.data.ai.StudioTaskType
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.ui.screens.AiMentorScreen
import com.example.ui.screens.ConversationSimulatorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.InteractiveCoachViewModel
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class PhoneGeminiBridgeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ensureTestFirebaseApp(ctx)
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun parseInboundIntent_handlesDeepLinksAndShortcuts() {
        val dailyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("speakcraft://daily_practice"))
        val dailyLink = PhoneGeminiBridge.parseInboundIntent(dailyIntent)
        assertNotNull(dailyLink)
        assertEquals("daily_practice", dailyLink?.targetRoute)
        assertNull(dailyLink?.sharedText)

        val vocabIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("speakcraft://app/vocabulary?prompt=concise")
        )
        val vocabLink = PhoneGeminiBridge.parseInboundIntent(vocabIntent)
        assertNotNull(vocabLink)
        assertEquals("vocabulary", vocabLink?.targetRoute)
        assertEquals("concise", vocabLink?.sharedText)

        val unknownIntent = Intent(Intent.ACTION_VIEW, Uri.parse("speakcraft://unsupported_screen"))
        val unknownLink = PhoneGeminiBridge.parseInboundIntent(unknownIntent)
        assertEquals("dashboard", unknownLink?.targetRoute)

        val externalWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/daily_practice"))
        assertNull(PhoneGeminiBridge.parseInboundIntent(externalWebIntent))
        assertNull(PhoneGeminiBridge.parseInboundIntent(null))
    }

    @Test
    fun parseInboundIntent_routesSharedPlainTextFromGeminiToAiMentor() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Summarize my project impact in three clear sentences.")
        }
        val link = PhoneGeminiBridge.parseInboundIntent(shareIntent)
        assertNotNull(link)
        assertEquals("ai_mentor", link?.targetRoute)
        assertEquals("Summarize my project impact in three clear sentences.", link?.sharedText)
    }

    @Test
    fun promptBuilders_includeLevelContextWithoutPii() {
        val mentorBlank = PhoneGeminiBridge.buildMentorPrompt("", "Intermediate")
        assertTrue(mentorBlank.contains("Intermediate"))
        assertTrue(mentorBlank.contains("PREP"))

        val mentorWithDraft = PhoneGeminiBridge.buildMentorPrompt("Opening a design review", "Advanced")
        assertTrue(mentorWithDraft.contains("Advanced"))
        assertTrue(mentorWithDraft.contains("Opening a design review"))

        val roleplayPrompt = PhoneGeminiBridge.buildConversationRoleplayPrompt("Job Interview", "Upper Intermediate")
        assertTrue(roleplayPrompt.contains("Job Interview"))
        assertTrue(roleplayPrompt.contains("Upper Intermediate"))

        val sampleWord = CurriculumCatalog.vocabularyBank.first()
        val vocabPrompt = PhoneGeminiBridge.buildVocabularyPrompt(sampleWord, sampleWord.level)
        assertTrue(vocabPrompt.contains(sampleWord.word))
        assertTrue(vocabPrompt.contains(sampleWord.level))
        assertFalse(vocabPrompt.contains("@"))
    }

    @Test
    fun inspectConnectionStatus_handlesDefaultDeviceStateSafely() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val status = PhoneGeminiBridge.inspectConnectionStatus(ctx)
        assertFalse(status.geminiAppInstalled)
        assertTrue(status.shareTargetEnabled)
        assertTrue(status.statusLabel.isNotBlank())
        assertTrue(status.detailMessage.isNotBlank())
    }

    @Test
    fun settingsScreen_displaysPhoneGeminiConnectionControlsWithoutCloudAiToggle() {
        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(),
                    userEmail = "learner@speakcraft.app",
                    isAdmin = false,
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onOpenAdminConsole = {},
                    onClearHistory = {},
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_cloud_ai_switch").assertDoesNotExist()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_open_gemini_app_button"))
        composeRule.onNodeWithTag("settings_open_gemini_app_button").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_launch_gemini_voice_button"))
        composeRule.onNodeWithTag("settings_launch_gemini_voice_button").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_ai_privacy_statement"))
        composeRule.onNodeWithTag("settings_ai_privacy_statement").assertIsDisplayed()
    }

    @Test
    fun aiMentorAndConversationScreens_exposePhoneGeminiHandoffAndConsumeDraft() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = InteractiveCoachViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            appContext = ctx,
            currentUserId = "test_phone_gemini"
        )
        var consumed = false

        composeRule.setContent {
            SpeakCraftTheme {
                AiMentorScreen(
                    coachVm = vm,
                    profile = UserProfile(userId = "test_phone_gemini", displayName = "Sruthi"),
                    sessions = emptyList(),
                    feedbackList = emptyList(),
                    initialDraftFromGemini = "Coach me on speaking calmly",
                    onConsumeInitialDraft = { consumed = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mentor_open_gemini_button").assertIsDisplayed()
        composeRule.onNodeWithTag("mentor_question_input")
            .assertIsDisplayed()
            .assertTextContains("Coach me on speaking calmly")
        assertTrue(consumed)
    }

    @Test
    fun conversationSimulatorScreen_exposesGeminiVoiceButton() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = InteractiveCoachViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            appContext = ctx,
            currentUserId = "test_conv_gemini"
        )
        val tts = TtsAndSpeechHelper(ctx)

        composeRule.setContent {
            SpeakCraftTheme {
                ConversationSimulatorScreen(
                    coachVm = vm,
                    ttsHelper = tts,
                    userLevel = "Intermediate",
                    ttsRate = 1.0f
                )
            }
        }

        composeRule.onNodeWithTag("conversation_gemini_voice_button").assertIsDisplayed()
    }

    @Test
    fun parseInboundIntent_routesSharedGeminiFeedbackBackToSpeakingLabWhenPendingHandoffExists() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        PhoneGeminiBridge.clearPendingStudioHandoff(ctx)

        PhoneGeminiBridge.savePendingStudioHandoff(
            ctx,
            PendingStudioGeminiHandoff(
                targetRoute = "speaking_lab",
                taskTypeName = StudioTaskType.DAILY_VIDEO.name,
                taskTitle = "Daily Video Recording (Open Speak)",
                mediaLabel = "Video (35s)",
                isVideo = true,
                sentAtMs = System.currentTimeMillis()
            )
        )

        val returnedFromGemini = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Strengths: Strong eye contact. Improve: Pause 1s before closing.")
        }
        val studioLink = PhoneGeminiBridge.parseInboundIntent(returnedFromGemini, ctx)
        assertNotNull(studioLink)
        assertEquals("speaking_lab", studioLink?.targetRoute)
        assertEquals("Strengths: Strong eye contact. Improve: Pause 1s before closing.", studioLink?.sharedText)
        assertNull(PhoneGeminiBridge.getPendingStudioHandoff(ctx))

        // Without pending studio handoff, plain share routes to ai_mentor
        val mentorLink = PhoneGeminiBridge.parseInboundIntent(returnedFromGemini, ctx)
        assertEquals("ai_mentor", mentorLink?.targetRoute)
    }

    @Test
    fun studioReviewPromptAndFeedbackPersistence_workForDailyVideoAndTopicTasks() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val dailyPrompt = PhoneGeminiBridge.buildStudioReviewPrompt(
            taskType = StudioTaskType.DAILY_VIDEO,
            taskTitle = "Daily Video Recording (Open Speak)",
            isVideo = true,
            transcript = "Today I shared a short reflection on leading our sprint review cleanly.",
            durationSeconds = 42
        )
        assertTrue(dailyPrompt.contains("1. Daily Video Recording"))
        assertTrue(dailyPrompt.contains("whatever they wanted"))
        assertTrue(dailyPrompt.contains("video recording"))
        assertTrue(dailyPrompt.contains("42s"))

        val topicPrompt = PhoneGeminiBridge.buildStudioReviewPrompt(
            taskType = StudioTaskType.SELECT_TOPIC,
            taskTitle = "STAR Interview",
            isVideo = false,
            transcript = "",
            durationSeconds = 30
        )
        assertTrue(topicPrompt.contains("2. Selected Topic Practice"))
        assertTrue(topicPrompt.contains("STAR Interview"))
        assertTrue(topicPrompt.contains("audio recording"))

        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("outbound", dailyPrompt))
        assertNull("Outbound prompt on clipboard must not be imported as Gemini reply", PhoneGeminiBridge.readGeminiFeedbackFromClipboard(ctx))

        clipboard.setPrimaryClip(ClipData.newPlainText("gemini_reply", "Gemini Review: Calm voice pacing; trim filler phrases."))
        assertEquals("Gemini Review: Calm voice pacing; trim filler phrases.", PhoneGeminiBridge.readGeminiFeedbackFromClipboard(ctx))

        val savedList = PhoneGeminiBridge.saveStudioFeedbackEntry(
            ctx,
            StudioGeminiFeedbackEntry(
                id = "test_fb_1",
                taskTypeLabel = StudioTaskType.DAILY_VIDEO.label,
                taskTitle = "Daily Video Recording (Open Speak)",
                mediaLabel = "Video (42s)",
                isVideo = true,
                feedbackText = "Gemini Review: Calm voice pacing; trim filler phrases.",
                source = "Gemini App Clipboard",
                receivedAtMs = 1_764_000_000_000L
            )
        )
        assertTrue(savedList.any { it.feedbackText.contains("Calm voice pacing") })
        assertTrue(PhoneGeminiBridge.loadStudioFeedbackHistory(ctx).any { it.feedbackText.contains("Calm voice pacing") })
    }

    @Test
    fun speakingLabScreen_supportsDailyVideoAndTopicTasksAndReceivesGeminiFeedback() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = SpeakingAndLabsViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            audioManager = AudioRecorderManager(ctx)
        )
        val clip = File(ctx.cacheDir, "studio_daily_test.mp4").apply { writeBytes(ByteArray(128) { 3 }) }
        vm.attachRecordedVideoFile(clip, 18)
        var consumed = false

        try {
            composeRule.setContent {
                SpeakCraftTheme {
                    SpeakingLabScreen(
                        speakingVm = vm,
                        pastSessions = emptyList(),
                        onDeleteSession = {},
                        incomingGeminiFeedback = "Gemini Feedback: Clear posture and strong opening hook.",
                        onConsumeIncomingGeminiFeedback = { consumed = true }
                    )
                }
            }
            composeRule.waitForIdle()
            assertTrue(consumed)

            composeRule.onNodeWithTag("studio_task_daily_video_chip").performScrollTo().assertIsDisplayed().assertIsSelected()
            composeRule.onNodeWithTag("studio_task_select_topics_chip").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("studio_daily_video_banner").performScrollTo().assertIsDisplayed()

            composeRule.onNodeWithTag("studio_forward_to_gemini_switch").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("send_recording_to_gemini_button").performScrollTo().assertIsDisplayed()

            composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("studio_gemini_feedback_card"))
            composeRule.onNodeWithTag("studio_gemini_feedback_card").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("studio_gemini_feedback_input")
                .performScrollTo()
                .assertIsDisplayed()
                .assertTextContains("Gemini Feedback: Clear posture and strong opening hook.")
            composeRule.onNodeWithTag("studio_saved_feedback_text_0")
                .performScrollTo()
                .assertIsDisplayed()
                .assertTextContains("Gemini Feedback: Clear posture and strong opening hook.")

            // Switch to Task 2
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("studio_two_tasks_panel"))
            composeRule.onNodeWithTag("studio_task_select_topics_chip").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("studio_task_select_topics_chip").assertIsSelected()
            composeRule.onNodeWithTag("studio_daily_video_banner").assertDoesNotExist()
        } finally {
            clip.delete()
        }
    }
}
