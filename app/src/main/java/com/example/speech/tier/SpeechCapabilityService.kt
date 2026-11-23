package com.example.speech.tier

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import com.example.core.logging.AppLogger
import java.util.Locale

private const val TAG = "SpeechCapabilityService"

/**
 * Inspects device platform capabilities and user consent to determine active [SpeechTier]
 * (CMP-24, ADR-009, ARC-05 §3, FS-12).
 */
open class SpeechCapabilityService(
    private val context: Context,
    private val isPipeWorkingOverride: Boolean? = null,
    private val isOfflineSupportedOverride: Boolean? = null
) {

    /**
     * Re-evaluates and resolves active [SpeechTier] based on API level, on-device recognizer
     * presence, user online consent, and previous pipe stability.
     *
     * @param userConsentedToOnline whether user consented to online fallback (SEC-01 §7.3)
     * @param pipeDemoted whether audio-source pipe has previously failed on this device (RSK-09)
     */
    fun detectTier(
        userConsentedToOnline: Boolean = false,
        pipeDemoted: Boolean = false
    ): SpeechTier {
        val sdkInt = Build.VERSION.SDK_INT
        val isOnDeviceAvailable = isOfflineRecognitionSupported()

        AppLogger.i(
            TAG,
            "Detecting speech tier: sdk=$sdkInt, offlineAvailable=$isOnDeviceAvailable, consent=$userConsentedToOnline, pipeDemoted=$pipeDemoted"
        )

        // T1 requires API >= 33, on-device recognizer, and functioning audio-source pipe
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU && isOnDeviceAvailable) {
            val pipeSupported = isPipeWorkingOverride ?: (!pipeDemoted)
            if (pipeSupported) {
                return SpeechTier.T1_FULL_ON_DEVICE
            }
            // Piping failure demotes device to T2 (ADR-009, RSK-09)
            AppLogger.w(TAG, "Audio-source pipe failed or demoted; falling back from T1 to T2")
            return SpeechTier.T2_ON_DEVICE_MIC
        }

        // T2 requires API >= 31 and on-device recognizer
        if (sdkInt >= Build.VERSION_CODES.S && isOnDeviceAvailable) {
            return SpeechTier.T2_ON_DEVICE_MIC
        }

        // T3: No offline recognizer available or API < 31
        return if (userConsentedToOnline) {
            SpeechTier.T3A_ONLINE_CONSENTED
        } else {
            SpeechTier.T3B_AUDIO_ONLY
        }
    }

    /**
     * Checks whether platform on-device speech recognition is available on this device.
     */
    open fun isOfflineRecognitionSupported(): Boolean {
        if (isOfflineSupportedOverride != null) {
            return isOfflineSupportedOverride
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false
        }
        return runCatching {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        }.getOrDefault(false)
    }

    /**
     * Checks if a specific locale is supported for recognition (API 33+ support checking).
     */
    fun isLocaleSupported(locale: Locale): Boolean {
        // En-US, En-GB, En-IN, En-AU are the core target locales (ARC-05 §4.3)
        val language = locale.language
        return language.equals("en", ignoreCase = true)
    }
}
