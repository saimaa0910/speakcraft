package com.example.data.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.data.ai.local.DeviceCapabilityManager
import com.example.data.ai.local.DeviceProfile
import com.example.data.ai.local.OfflineAIAvailabilityManager
import com.example.data.preferences.AISettings
import com.example.data.preferences.AISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow

enum class CapabilityStatus(val label: String) {
    ENABLED("Enabled"),
    DISABLED("Disabled"),
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    PERMISSION_REQUIRED("Permission Required"),
    MODEL_REQUIRED("Model Required"),
    INITIALIZATION_FAILED("Initialization Failed")
}

data class CapabilityDescriptor(
    val key: String,
    val title: String,
    val status: CapabilityStatus,
    val detail: String,
    val offlineCapable: Boolean
)

data class IntelligenceCapabilityMatrix(
    val localAi: CapabilityDescriptor,
    val liveTranscription: CapabilityDescriptor,
    val localSpeechRecognition: CapabilityDescriptor,
    val localAudioAnalysis: CapabilityDescriptor,
    val localVideoAnalysis: CapabilityDescriptor,
    val hasRecordAudioPermission: Boolean,
    val hasCameraPermission: Boolean,
    val offlineSpeechAvailable: Boolean,
    val activeRecognitionEngineLabel: String,
    /** Platform recogniser status; the app ships no speech model of its own (FND-AI-03). */
    val onDeviceSpeechStatus: String,
    val mediaPipeStatus: String,
    val speechRecognizerInitError: String?,
    val deviceProfile: DeviceProfile = DeviceProfile.STANDARD,
    val deviceModelSummary: String = "Optimized for this device"
)

/**
 * Single authoritative evaluator for what AI, Speech, Audio DSP, Video Vision, and Cloud
 * capabilities are actually enabled, permitted, initialized, or unavailable on this device.
 *
 * Guarantees Rule #10: Never mark an unavailable feature as enabled or available.
 */
class IntelligenceCapabilityManager(
    private val context: Context? = null,
    private val offlineAvailabilityManager: OfflineAIAvailabilityManager? = null
) {
    private val runtimeInitError = MutableStateFlow<String?>(null)

    fun recordRecognizerInitFailure(reason: String?) {
        runtimeInitError.value = reason?.takeIf { it.isNotBlank() }
    }

    fun observeMatrix(settingsRepository: AISettingsRepository): Flow<IntelligenceCapabilityMatrix> =
        combine(settingsRepository.aiSettingsFlow, runtimeInitError) { settings, initErr ->
            computeMatrix(settings = settings, runtimeRecognizerError = initErr)
        }

    fun hasAudioPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun hasCameraPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun isAndroidOnDeviceSpeechSupported(): Boolean {
        val ctx = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx) }.getOrDefault(false)
        } else {
            false
        }
    }

    fun isAndroidAnySpeechRecognizerAvailable(): Boolean {
        val ctx = context ?: return false
        return runCatching { SpeechRecognizer.isRecognitionAvailable(ctx) }.getOrDefault(false)
    }

    fun computeMatrix(
        settings: AISettings,
        runtimeRecognizerError: String? = runtimeInitError.value
    ): IntelligenceCapabilityMatrix {
        val micPerm = hasAudioPermission()
        val camPerm = hasCameraPermission()
        // Transcripts come only from the platform recogniser (FND-AI-03: the app-managed Whisper/Moonshine path
        // was fabricated and is removed); a stored LOCAL_WHISPER preference behaves as AUTO.
        val androidOnDevice = isAndroidOnDeviceSpeechSupported()
        val androidAnyAsr = isAndroidAnySpeechRecognizerAvailable()
        val offlineSpeechAvailable = androidOnDevice

        val mediaPipeStatusObj = offlineAvailabilityManager?.getStatus()
        val faceModelReady = mediaPipeStatusObj?.canAnalyzeVideo ?: (context != null)

        val localAiDescriptor = CapabilityDescriptor(
            key = "local_ai",
            title = "Local Intelligence",
            status = if (settings.localAiEnabled) CapabilityStatus.ENABLED else CapabilityStatus.DISABLED,
            detail = if (settings.localAiEnabled) {
                "Rule-based grammar, vocabulary, acoustic DSP, and coaching insights active on-device without a cloud LLM."
            } else {
                "Disabled by user in AI & Intelligence settings."
            },
            offlineCapable = true
        )

        val engineLabel = when {
            androidOnDevice -> "Android On-Device SpeechRecognizer"
            androidAnyAsr -> "Android SpeechRecognizer (device-dependent offline pack)"
            else -> "Unavailable (No speech recognizer ready)"
        }

        val effectiveErr = runtimeRecognizerError ?: this.runtimeInitError.value
        val liveTranscriptionDescriptor = when {
            !settings.liveTranscriptionEnabled -> CapabilityDescriptor(
                key = "live_transcription",
                title = "Live Transcription",
                status = CapabilityStatus.DISABLED,
                detail = "Turn on in Settings → AI & Intelligence → Speech & Transcription.",
                offlineCapable = offlineSpeechAvailable
            )
            !micPerm -> CapabilityDescriptor(
                key = "live_transcription",
                title = "Live Transcription",
                status = CapabilityStatus.PERMISSION_REQUIRED,
                detail = "Microphone permission (RECORD_AUDIO) is required before live transcription can start.",
                offlineCapable = offlineSpeechAvailable
            )
            effectiveErr != null -> CapabilityDescriptor(
                key = "live_transcription",
                title = "Live Transcription",
                status = CapabilityStatus.INITIALIZATION_FAILED,
                detail = effectiveErr,
                offlineCapable = offlineSpeechAvailable
            )
            !androidAnyAsr -> CapabilityDescriptor(
                key = "live_transcription",
                title = "Live Transcription",
                status = CapabilityStatus.UNAVAILABLE,
                detail = "No speech recognition service is available on this device. Practice feedback uses the audio only.",
                offlineCapable = false
            )
            else -> CapabilityDescriptor(
                key = "live_transcription",
                title = "Live Transcription",
                status = CapabilityStatus.ENABLED,
                detail = if (offlineSpeechAvailable) {
                    "Ready ($engineLabel • Offline verified)."
                } else {
                    "Enabled via $engineLabel (Offline pack not verified on this API level; audio DSP still runs offline)."
                },
                offlineCapable = offlineSpeechAvailable
            )
        }

        // Platform capability only: the app ships no recogniser of its own.
        val localSpeechRecDescriptor = if (androidOnDevice) {
            CapabilityDescriptor(
                key = "local_speech_recognition",
                title = "On-Device Speech Recognition",
                status = CapabilityStatus.AVAILABLE,
                detail = "This device's speech recogniser can transcribe on the device ($engineLabel).",
                offlineCapable = true
            )
        } else {
            CapabilityDescriptor(
                key = "local_speech_recognition",
                title = "On-Device Speech Recognition",
                status = CapabilityStatus.UNAVAILABLE,
                detail = "No verified on-device recogniser on this device. Live transcripts may need the device's online speech service; without a transcript, feedback uses the audio only.",
                offlineCapable = false
            )
        }

        val audioAnalysisDescriptor = CapabilityDescriptor(
            key = "local_audio_analysis",
            title = "Local Audio Analysis (DSP)",
            status = if (settings.localAudioAnalysisEnabled && settings.localAiEnabled) {
                CapabilityStatus.ENABLED
            } else {
                CapabilityStatus.DISABLED
            },
            detail = if (settings.localAudioAnalysisEnabled && settings.localAiEnabled) {
                "Deterministic loudness, pitch, pause cadence, clipping, and speech-silence DSP active offline."
            } else {
                "Disabled in AI & Intelligence settings."
            },
            offlineCapable = true
        )

        val videoAnalysisDescriptor = when {
            !settings.localVideoAnalysisEnabled -> CapabilityDescriptor(
                key = "local_video_analysis",
                title = "Video Intelligence (Camera/Pose)",
                status = CapabilityStatus.DISABLED,
                detail = "Disabled in settings. Voice-only exercises do not require video.",
                offlineCapable = true
            )
            !camPerm -> CapabilityDescriptor(
                key = "local_video_analysis",
                title = "Video Intelligence (Camera/Pose)",
                status = CapabilityStatus.PERMISSION_REQUIRED,
                detail = "Camera permission is required only for video-assessed practice.",
                offlineCapable = faceModelReady
            )
            !faceModelReady -> CapabilityDescriptor(
                key = "local_video_analysis",
                title = "Video Intelligence (Camera/Pose)",
                status = CapabilityStatus.MODEL_REQUIRED,
                detail = "Bundled MediaPipe landmarker models could not be initialized.",
                offlineCapable = false
            )
            else -> CapabilityDescriptor(
                key = "local_video_analysis",
                title = "Video Intelligence (Camera/Pose)",
                status = CapabilityStatus.AVAILABLE,
                detail = "MediaPipe face, posture, and hand gesture analysis ready for video exercises.",
                offlineCapable = true
            )
        }

        val onDeviceSpeechStatusText = when {
            androidOnDevice -> "Platform on-device recognition available"
            androidAnyAsr -> "Platform recogniser only (on-device mode not verified)"
            else -> "No speech recogniser on this device"
        }

        val mediaPipeStatusText = if (faceModelReady) {
            "Bundled MediaPipe tasks ready"
        } else {
            "Unavailable on this context"
        }

        val profile = offlineAvailabilityManager?.getStatus()?.activeProfile
            ?: DeviceCapabilityManager(context).getProfile()
        val deviceSummary = "Hardware Profile: ${profile.name} • Selected Engine: $engineLabel • Vision: $mediaPipeStatusText"

        return IntelligenceCapabilityMatrix(
            localAi = localAiDescriptor,
            liveTranscription = liveTranscriptionDescriptor,
            localSpeechRecognition = localSpeechRecDescriptor,
            localAudioAnalysis = audioAnalysisDescriptor,
            localVideoAnalysis = videoAnalysisDescriptor,
            hasRecordAudioPermission = micPerm,
            hasCameraPermission = camPerm,
            offlineSpeechAvailable = offlineSpeechAvailable,
            activeRecognitionEngineLabel = engineLabel,
            onDeviceSpeechStatus = onDeviceSpeechStatusText,
            mediaPipeStatus = mediaPipeStatusText,
            speechRecognizerInitError = effectiveErr,
            deviceProfile = profile,
            deviceModelSummary = deviceSummary
        )
    }
}
