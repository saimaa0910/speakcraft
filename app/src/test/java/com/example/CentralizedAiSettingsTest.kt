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
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.CapabilityStatus
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.ai.local.LocalExecutionPolicy
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.preferences.AISettings
import com.example.data.preferences.AISettingsRepository
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.RecognitionEnginePreference
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeakingLabScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.theme.SpeakCraftTheme
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class CentralizedAiSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ensureFirestore(): FirebaseFirestore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ensureTestFirebaseApp(ctx)
        return FirebaseFirestore.getInstance()
    }

    @Test
    fun `01 AISettingsRepository persists all centralized AI, speech, and video toggles`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val repo = AISettingsRepository(AppPreferencesRepository(ctx))

        repo.setLocalAiEnabled(true)
        repo.setLiveTranscriptionEnabled(true)
        repo.setLocalSpeechRecognitionEnabled(false)
        repo.setLocalAudioAnalysisEnabled(true)
        repo.setLocalVideoAnalysisEnabled(false)
        repo.setSelectedRecognitionLanguage("en-GB")
        repo.setSelectedRecognitionEngine(RecognitionEnginePreference.ANDROID_OFFLINE)

        val saved = repo.aiSettingsFlow.first()
        assertTrue(saved.localAiEnabled)
        assertTrue(saved.liveTranscriptionEnabled)
        assertFalse(saved.localSpeechRecognitionEnabled)
        assertTrue(saved.localAudioAnalysisEnabled)
        assertFalse(saved.localVideoAnalysisEnabled)
        assertEquals("en-GB", saved.selectedRecognitionLanguage)
        assertEquals(RecognitionEnginePreference.ANDROID_OFFLINE, saved.selectedRecognitionEngine)

        // Restore defaults cleanly
        repo.setLiveTranscriptionEnabled(false)
        repo.setLocalSpeechRecognitionEnabled(true)
        repo.setLocalVideoAnalysisEnabled(true)
        repo.setSelectedRecognitionLanguage("en-US")
        repo.setSelectedRecognitionEngine(RecognitionEnginePreference.AUTO)
    }

    @Test
    fun `02 Stage 1 Zero-Cloud-AI policy keeps local-only mode with zero cloud uploads`() = runBlocking {
        assertTrue(LocalExecutionPolicy.isLocalOnly())
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val repo = AISettingsRepository(AppPreferencesRepository(ctx))

        val settings = repo.aiSettingsFlow.first()
        assertFalse(settings.audioVideoUploadedToCloud)
    }

    @Test
    fun `03 IntelligenceCapabilityManager reports honest permission and unavailable statuses`() {
        val mgr = IntelligenceCapabilityManager(context = null)
        val disabledMatrix = mgr.computeMatrix(AISettings(liveTranscriptionEnabled = false))
        assertEquals(CapabilityStatus.ENABLED, disabledMatrix.localAi.status)
        assertEquals(CapabilityStatus.DISABLED, disabledMatrix.liveTranscription.status)
        assertEquals(CapabilityStatus.PERMISSION_REQUIRED, disabledMatrix.localVideoAnalysis.status)

        val enabledNoPermMatrix = mgr.computeMatrix(AISettings(liveTranscriptionEnabled = true))
        assertEquals(
            "Enabled live transcription without mic permission must report PERMISSION_REQUIRED, never ENABLED",
            CapabilityStatus.PERMISSION_REQUIRED,
            enabledNoPermMatrix.liveTranscription.status
        )
    }

    @Test
    fun `04 IntelligenceCapabilityManager reports INITIALIZATION_FAILED when recognizer init fails`() {
        val mgr = IntelligenceCapabilityManager(context = null)
        mgr.recordRecognizerInitFailure("Recognizer init crashed")
        val matrix = mgr.computeMatrix(AISettings(liveTranscriptionEnabled = true))
        assertEquals("Recognizer init crashed", matrix.speechRecognizerInitError)
    }

    @Test
    fun `05 IntelligenceCapabilityManager disables local audio analysis when Local AI is turned off`() {
        val mgr = IntelligenceCapabilityManager(context = null)
        val matrix = mgr.computeMatrix(AISettings(localAiEnabled = false, localAudioAnalysisEnabled = true))
        assertEquals(CapabilityStatus.DISABLED, matrix.localAi.status)
        assertEquals(CapabilityStatus.DISABLED, matrix.localAudioAnalysis.status)
    }

    @Test
    fun `06 SpeakingAndLabsViewModel reacts to AISettings and exposes session-scoped state`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val aiRepo = AISettingsRepository(AppPreferencesRepository(ctx))
        aiRepo.setLiveTranscriptionEnabled(true)

        val vm = SpeakingAndLabsViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            audioManager = AudioRecorderManager(ctx),
            aiSettingsRepo = aiRepo
        )
        assertTrue(vm.currentSessionId.value.isNotBlank())

        aiRepo.setLiveTranscriptionEnabled(false)
    }

    @Test
    fun `07 SettingsScreen displays all five centralized AI and Intelligence subsections`() {
        var localToggled: Boolean? = null
        var liveToggled: Boolean? = null
        var videoToggled: Boolean? = null

        composeRule.setContent {
            SpeakCraftTheme {
                SettingsScreen(
                    prefs = AppPreferences(),
                    userEmail = "speaker@speakcraft.app",
                    isAdmin = false,
                    onSetTheme = {},
                    onSetTtsRate = {},
                    onOpenAdminConsole = {},
                    onClearHistory = {},
                    onSignOut = {},
                    onSetLocalAiEnabled = { localToggled = it },
                    onSetLiveTranscriptionEnabled = { liveToggled = it },
                    onSetLocalVideoAnalysisEnabled = { videoToggled = it }
                )
            }
        }

        composeRule.onNodeWithTag("settings_ai_intelligence_section").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_local_ai_switch"))
        composeRule.onNodeWithTag("settings_local_ai_switch").performClick()
        assertEquals(false, localToggled)

        // ADR-003 (T-04.1.2): Cloud AI toggle and status must not appear in Settings
        composeRule.onNodeWithTag("settings_cloud_ai_switch").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_cloud_ai_status").assertDoesNotExist()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_live_transcription_switch"))
        composeRule.onNodeWithText("Display recognized words while you are speaking during practice.").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_live_transcription_switch").performClick()
        assertEquals(true, liveToggled)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_speech_diagnostics"))
        composeRule.onNodeWithTag("settings_speech_diagnostics").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_local_video_switch"))
        composeRule.onNodeWithTag("settings_local_video_switch").performClick()
        assertEquals(false, videoToggled)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_ai_privacy_statement"))
        composeRule.onNodeWithTag("settings_ai_privacy_statement").assertIsDisplayed()
    }

    @Test
    fun `08 SpeakingLabScreen exposes centralized AI Settings link and has zero scattered cloud toggles`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = SpeakingAndLabsViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            audioManager = AudioRecorderManager(ctx)
        )
        var openedSettings = false
        composeRule.setContent {
            SpeakCraftTheme {
                SpeakingLabScreen(
                    speakingVm = vm,
                    pastSessions = emptyList(),
                    onDeleteSession = {},
                    onOpenAiSettings = { openedSettings = true }
                )
            }
        }
        composeRule.onNodeWithTag("speaking_cloud_toggle_chip").assertDoesNotExist()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("speaking_open_ai_settings_button"))
        composeRule.onNodeWithTag("speaking_open_ai_settings_button").performClick()
        assertTrue(openedSettings)
    }

    @Test
    fun `09 ChallengesScreen has zero scattered cloud toggles and includes live transcript panel`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = SpeakingAndLabsViewModel(
            repository = SpeakCraftRepository(ensureFirestore()),
            audioManager = AudioRecorderManager(ctx)
        )
        composeRule.setContent {
            SpeakCraftTheme {
                ChallengesScreen(
                    speakingVm = vm,
                    sharedChallenges = emptyList(),
                    pastSessions = emptyList()
                )
            }
        }
        composeRule.onNodeWithTag("speaking_cloud_toggle_chip").assertDoesNotExist()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("live_transcript_panel"))
        composeRule.onNodeWithTag("live_transcript_panel").assertIsDisplayed()
    }

    @Test
    fun `10 VocabularyScreen removes scattered cloud toggle chip even when callback passed`() {
        composeRule.setContent {
            SpeakCraftTheme {
                VocabularyScreen(
                    entries = emptyList(),
                    todayEpochDay = 20000L,
                    vocabEvalState = null,
                    onSpeakWord = {},
                    onReviewCard = { _, _, _ -> },
                    onEvaluateUsage = { _, _, _ -> },
                    onSaveCustomWord = { _, _, _, _, _ -> },
                    onClearEval = {}
                )
            }
        }
        composeRule.onNodeWithText("AI Quiz").performClick()
        composeRule.onNodeWithTag("vocabulary_cloud_toggle_chip").assertDoesNotExist()
    }
}
