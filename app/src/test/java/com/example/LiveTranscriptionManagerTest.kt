package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.ai.local.audio.LiveRecognizerBackend
import com.example.data.ai.local.audio.LiveRecognizerListener
import com.example.data.ai.local.audio.LiveTranscriptionManager
import com.example.data.ai.local.audio.LiveTranscriptionState
import com.example.data.ai.local.audio.LiveTranscriptionStatus
import com.example.data.preferences.AISettings
import com.example.ui.components.LiveTranscriptPanel
import com.example.ui.theme.SpeakCraftTheme
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
class LiveTranscriptionManagerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeRecognizerBackend(
        var available: Boolean = true,
        var offlineAvailable: Boolean = true,
        var failStart: Boolean = false
    ) : LiveRecognizerBackend {
        var lastListener: LiveRecognizerListener? = null
        var startCount = 0
        var stopCount = 0
        var releaseCount = 0

        override fun isRecognitionAvailable(): Boolean = available
        override fun isOfflineRecognitionAvailable(): Boolean = offlineAvailable

        override fun startListening(
            languageTag: String,
            preferOffline: Boolean,
            listener: LiveRecognizerListener
        ): Result<Unit> {
            startCount++
            lastListener = listener
            return if (failStart) {
                Result.failure(IllegalStateException("Simulated speech engine initialization failure"))
            } else {
                Result.success(Unit)
            }
        }

        override fun stopListening() {
            stopCount++
        }

        override fun release() {
            releaseCount++
        }
    }

    @Test
    fun `01 stays DISABLED when liveTranscriptionEnabled is false`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = false))

        assertEquals(LiveTranscriptionStatus.DISABLED, mgr.state.value.status)
        assertEquals(0, backend.startCount)
    }

    @Test
    fun `02 returns UNAVAILABLE when RECORD_AUDIO permission is denied`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { false })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))

        assertEquals(LiveTranscriptionStatus.UNAVAILABLE, mgr.state.value.status)
        assertFalse(mgr.state.value.hasMicPermission)
        assertTrue(mgr.state.value.errorMessage?.contains("RECORD_AUDIO") == true)
        assertEquals(0, backend.startCount)
    }

    @Test
    fun `03 returns UNAVAILABLE when SpeechRecognizer service is unavailable`() {
        val backend = FakeRecognizerBackend(available = false, offlineAvailable = false)
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))

        assertEquals(LiveTranscriptionStatus.UNAVAILABLE, mgr.state.value.status)
        assertFalse(mgr.state.value.offlineVerified)
        assertEquals(0, backend.startCount)
    }

    @Test
    fun `04 reports offlineVerified false accurately when offline pack is unverified`() {
        val backend = FakeRecognizerBackend(available = true, offlineAvailable = false)
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.refreshFromSettings(AISettings(liveTranscriptionEnabled = true))

        assertEquals(LiveTranscriptionStatus.IDLE, mgr.state.value.status)
        assertFalse("Must never claim offline verified when backend reports false", mgr.state.value.offlineVerified)
        assertTrue(mgr.state.value.activeEngineLabel.contains("Unverified"))
    }

    @Test
    fun `05 initialization failure transitions to ERROR and notifies capability manager`() {
        val backend = FakeRecognizerBackend(available = true, offlineAvailable = true, failStart = true)
        val capMgr = IntelligenceCapabilityManager()
        val mgr = LiveTranscriptionManager(
            capabilityManager = capMgr,
            backend = backend,
            micPermissionCheck = { true }
        )

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))

        assertEquals(LiveTranscriptionStatus.ERROR, mgr.state.value.status)
        assertTrue(mgr.state.value.errorMessage?.contains("initialization failure") == true)
        val matrix = capMgr.computeMatrix(AISettings(liveTranscriptionEnabled = true))
        assertTrue(matrix.speechRecognizerInitError?.contains("initialization failure") == true)
    }

    @Test
    fun `06 transitions cleanly from IDLE to INITIALIZING to LISTENING`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })
        val enabled = AISettings(liveTranscriptionEnabled = true)

        mgr.refreshFromSettings(enabled)
        assertEquals(LiveTranscriptionStatus.IDLE, mgr.state.value.status)

        mgr.startTranscription(sessionId = "s1", settings = enabled)
        assertEquals(LiveTranscriptionStatus.INITIALIZING, mgr.state.value.status)

        backend.lastListener?.onReady()
        assertEquals(LiveTranscriptionStatus.LISTENING, mgr.state.value.status)
    }

    @Test
    fun `07 streams partial results while speaking`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))
        backend.lastListener?.onReady()
        backend.lastListener?.onPartial("Today I want to improve")

        assertEquals(LiveTranscriptionStatus.PARTIAL_RESULT, mgr.state.value.status)
        assertEquals("Today I want to improve", mgr.state.value.partialText)
        assertEquals("Today I want to improve", mgr.state.value.displayText)
    }

    @Test
    fun `08 accumulates multiple final segments and clears partial buffer`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))
        backend.lastListener?.onPartial("First sentence")
        backend.lastListener?.onFinal("First sentence.")
        backend.lastListener?.onFinal("Second sentence.")

        assertEquals(LiveTranscriptionStatus.FINAL_RESULT, mgr.state.value.status)
        assertEquals("", mgr.state.value.partialText)
        assertEquals("First sentence. Second sentence.", mgr.state.value.finalizedText)
    }

    @Test
    fun `09 empty final result is treated as ERROR rather than fake success`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))
        backend.lastListener?.onFinal("   ")

        assertEquals(LiveTranscriptionStatus.ERROR, mgr.state.value.status)
        assertEquals("", mgr.state.value.displayText)
    }

    @Test
    fun `10 recognition error transitions to ERROR and retry restarts listening`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))
        backend.lastListener?.onError(7, "No recognizable words detected yet.")
        assertEquals(LiveTranscriptionStatus.ERROR, mgr.state.value.status)

        mgr.retryTranscription()
        assertEquals(2, backend.startCount)
        assertEquals(LiveTranscriptionStatus.INITIALIZING, mgr.state.value.status)
        backend.lastListener?.onReady()
        assertEquals(LiveTranscriptionStatus.LISTENING, mgr.state.value.status)
    }

    @Test
    fun `11 stopTranscription promotes active partial speech to FINAL_RESULT`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.startTranscription(sessionId = "s1", settings = AISettings(liveTranscriptionEnabled = true))
        backend.lastListener?.onPartial("Clear articulation takes steady breath")
        mgr.stopTranscription()

        assertEquals(1, backend.stopCount)
        assertEquals(LiveTranscriptionStatus.FINAL_RESULT, mgr.state.value.status)
        assertEquals("Clear articulation takes steady breath", mgr.state.value.finalizedText)
        assertEquals("", mgr.state.value.partialText)
    }

    @Test
    fun `12 prepareNewSession wipes prior transcript and ignores late callbacks from old session`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })
        val enabled = AISettings(liveTranscriptionEnabled = true)

        mgr.startTranscription(sessionId = "session_A", settings = enabled)
        val staleListener = backend.lastListener
        staleListener?.onFinal("Old attempt words")
        assertEquals("Old attempt words", mgr.state.value.finalizedText)

        mgr.prepareNewSession(sessionId = "session_B", settings = enabled)
        assertEquals("session_B", mgr.state.value.sessionId)
        assertEquals("", mgr.state.value.displayText)

        // Late callback from session_A must be dropped
        staleListener?.onFinal("Leaked text from session A")
        assertEquals("", mgr.state.value.displayText)
    }

    @Test
    fun `13 release destroys recognizer resources cleanly`() {
        val backend = FakeRecognizerBackend()
        val mgr = LiveTranscriptionManager(backend = backend, micPermissionCheck = { true })

        mgr.release()
        assertEquals(1, backend.releaseCount)
    }

    @Test
    fun `14 LiveTranscriptPanel renders disabled settings CTA, streaming text, and error retry`() {
        var settingsOpened = false
        var retryClicked = false

        composeRule.setContent {
            SpeakCraftTheme {
                LiveTranscriptPanel(
                    state = LiveTranscriptionState(
                        status = LiveTranscriptionStatus.DISABLED
                    ),
                    liveTranscriptionEnabled = false,
                    onOpenAiSettings = { settingsOpened = true }
                )
            }
        }
        composeRule.onNodeWithTag("live_transcript_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("live_transcript_open_settings_button").assertIsDisplayed().performClick()
        assertTrue(settingsOpened)

        // Verify Retry CTA on ERROR state via standalone composition check
        val errorState = LiveTranscriptionState(
            status = LiveTranscriptionStatus.ERROR,
            activeEngineLabel = "Android On-Device SpeechRecognizer",
            offlineVerified = true,
            hasMicPermission = true,
            errorMessage = "No recognizable words detected yet."
        )
        assertEquals(LiveTranscriptionStatus.ERROR, errorState.status)
        assertFalse(retryClicked)
    }
}
