package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.media.TtsAndSpeechHelper
import com.example.data.model.UserProfile
import com.example.data.preferences.AppPreferences
import com.example.ui.components.InlineAiMentorCard
import com.example.ui.screens.AiMentorScreen
import com.example.ui.screens.ConversationSimulatorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.InteractiveCoachViewModel
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
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
class HybridAiComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ensureTestFirebaseApp(ctx)
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun `settings screen centralizes live transcription toggle and omits removed cloud ai toggle`() {
        var liveTranscriptionToggledTo: Boolean? = null

        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(liveTranscriptionEnabled = false),
                    userEmail = "test@speakcraft.app",
                    isAdmin = false,
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onOpenAdminConsole = {},
                    onClearHistory = {},
                    onSignOut = {},
                    onSetLiveTranscriptionEnabled = { liveTranscriptionToggledTo = it }
                )
            }
        }

        composeRule.onNodeWithTag("settings_cloud_ai_switch").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_cloud_ai_status").assertDoesNotExist()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_live_transcription_switch"))
        composeRule.onNodeWithTag("settings_live_transcription_switch").assertIsDisplayed().performClick()
        assertEquals(true, liveTranscriptionToggledTo)
    }

    @Test
    fun `ai mentor screen removes scattered cloud toggle and keeps role chips`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = InteractiveCoachViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            appContext = ctx,
            currentUserId = "test_mentor_ui"
        )

        composeRule.setContent {
            SpeakCraftTheme {
                AiMentorScreen(
                    coachVm = vm,
                    profile = UserProfile(userId = "test_mentor_ui", displayName = "Sruthi"),
                    sessions = emptyList(),
                    feedbackList = emptyList()
                )
            }
        }

        composeRule.onNodeWithTag("mentor_cloud_toggle_chip").assertDoesNotExist()
        composeRule.onNodeWithTag("mentor_role_chip_mentor").assertIsDisplayed()
        composeRule.onNodeWithTag("mentor_role_chip_interviewer").performClick()
        assertEquals("interviewer", vm.selectedMentorRole.value)
    }

    @Test
    fun `conversation simulator removes scattered cloud toggle and toggles voice mode`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = InteractiveCoachViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            appContext = ctx,
            currentUserId = "test_conv_ui"
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

        composeRule.onNodeWithTag("conversation_cloud_toggle_chip").assertDoesNotExist()
        composeRule.onNodeWithTag("conversation_voice_live_toggle").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("conversation_live_call_button").assertIsDisplayed()
    }

    @Test
    fun `speaking lab removes scattered cloud toggle and links to centralized ai settings`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = SpeakingAndLabsViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            localOrchestrator = LocalIntelligenceOrchestrator(ctx),
            audioManager = AudioRecorderManager(ctx)
        )

        composeRule.setContent {
            SpeakCraftTheme {
                SpeakingLabScreen(
                    speakingVm = vm,
                    pastSessions = emptyList(),
                    onDeleteSession = {}
                )
            }
        }

        composeRule.onNodeWithTag("speaking_cloud_toggle_chip").assertDoesNotExist()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("live_transcript_panel"))
        composeRule.onNodeWithTag("live_transcript_panel").assertIsDisplayed()
    }

    @Test
    fun `dashboard inline ai mentor card displays local ai badge and dispatches prompt`() {
        var dispatchedPrompt = ""
        composeRule.setContent {
            SpeakCraftTheme {
                InlineAiMentorCard(
                    onOpenMentor = {},
                    onAskPrompt = { dispatchedPrompt = it }
                )
            }
        }

        composeRule.onNodeWithText("Local AI").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_ai_mentor_input").performTextInput("Rewrite my intro pitch")
        composeRule.onNodeWithTag("dashboard_ai_mentor_send").performClick()
        assertEquals("Rewrite my intro pitch", dispatchedPrompt)

        composeRule.onNodeWithText("Create my 7-day plan").performClick()
        assertTrue(dispatchedPrompt.contains("7-day", ignoreCase = true))
    }
}
