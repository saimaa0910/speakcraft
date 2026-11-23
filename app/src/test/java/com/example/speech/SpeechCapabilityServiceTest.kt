package com.example.speech

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.speech.tier.SpeechCapabilityService
import com.example.speech.tier.SpeechTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpeechCapabilityServiceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `detectTier selects T1 when offline recognizer and pipe are functional on API 36`() {
        // Under Robolectric, create service with pipeWorkingOverride = true
        val service = object : SpeechCapabilityService(context, isPipeWorkingOverride = true) {
            override fun isOfflineRecognitionSupported(): Boolean = true
        }

        val tier = service.detectTier(userConsentedToOnline = false, pipeDemoted = false)
        assertEquals(SpeechTier.T1_FULL_ON_DEVICE, tier)
        assertTrue(tier.hasTranscript)
        assertTrue(tier.hasAudioReplay)
        assertTrue(tier.isStrictlyOnDevice)
    }

    @Test
    fun `detectTier demotes to T2 when audio-source pipe fails or is demoted on API 36`() {
        val service = object : SpeechCapabilityService(context, isPipeWorkingOverride = false) {
            override fun isOfflineRecognitionSupported(): Boolean = true
        }

        val tier = service.detectTier(userConsentedToOnline = false, pipeDemoted = true)
        assertEquals(SpeechTier.T2_ON_DEVICE_MIC, tier)
        assertTrue(tier.hasTranscript)
        assertFalse(tier.hasAudioReplay)
        assertTrue(tier.isStrictlyOnDevice)
    }

    @Test
    fun `detectTier selects T3A when offline recognizer unavailable but user consented to online`() {
        val service = object : SpeechCapabilityService(context) {
            override fun isOfflineRecognitionSupported(): Boolean = false
        }

        val tier = service.detectTier(userConsentedToOnline = true, pipeDemoted = false)
        assertEquals(SpeechTier.T3A_ONLINE_CONSENTED, tier)
        assertTrue(tier.hasTranscript)
        assertFalse(tier.hasAudioReplay)
        assertFalse(tier.isStrictlyOnDevice)
    }

    @Test
    fun `detectTier selects T3B audio-only fallback when offline unavailable and no online consent`() {
        val service = object : SpeechCapabilityService(context) {
            override fun isOfflineRecognitionSupported(): Boolean = false
        }

        val tier = service.detectTier(userConsentedToOnline = false, pipeDemoted = false)
        assertEquals(SpeechTier.T3B_AUDIO_ONLY, tier)
        assertFalse(tier.hasTranscript)
        assertTrue(tier.hasAudioReplay)
        assertTrue(tier.isStrictlyOnDevice)
    }

    @Test
    fun `isLocaleSupported returns true for English target locales`() {
        val service = SpeechCapabilityService(context)
        assertTrue(service.isLocaleSupported(Locale.US))
        assertTrue(service.isLocaleSupported(Locale.UK))
        assertTrue(service.isLocaleSupported(Locale("en", "IN")))
        assertTrue(service.isLocaleSupported(Locale("en", "AU")))
        assertFalse(service.isLocaleSupported(Locale.FRENCH))
    }
}
