package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.PhoneGeminiBridge
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.CurriculumCatalog
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.ui.screens.AiMentorScreen
import com.example.ui.screens.ConversationSimulatorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.InteractiveCoachViewModel
import com.google.firebase.firestore.FirebaseFirestore
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

        composeRule.onNodeWithTag("mentor_open_gemini_button").assertIsDisplayed()
        composeRule.onNodeWithText("Coach me on speaking calmly").assertIsDisplayed()
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
}
