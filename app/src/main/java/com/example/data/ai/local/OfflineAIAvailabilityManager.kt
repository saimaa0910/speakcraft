package com.example.data.ai.local

import android.content.Context

// ---------------------------------------------------------------------------
// Availability status snapshot (Stage 1.2: Lightweight On-Device AI)
// ---------------------------------------------------------------------------

/**
 * Immutable snapshot describing which AI capabilities are currently available on this device.
 *
 * @param canTranscribeSpeech      True when Whisper or Moonshine is installed.
 * @param canAnalyzeVideo          True when MediaPipe models are available.
 * @param canGenerateLocalFeedback Always true in Stage 1.2 — deterministic coaching requires zero models!
 * @param canRunSLM                Deprecated in Stage 1.2: always false (zero large LLMs).
 * @param activeProfile            The current [DeviceProfile] driving capability decisions.
 * @param missingModels            Optional lightweight models not yet installed.
 * @param limitationNotices        Human-readable strings describing why certain features are optional/limited.
 */
data class AIAvailabilityStatus(
    val canTranscribeSpeech: Boolean,
    val canAnalyzeVideo: Boolean,
    val canGenerateLocalFeedback: Boolean,
    @Deprecated("Removed in Stage 1.2 — no large LLMs used")
    val canRunSLM: Boolean = false,
    val activeProfile: DeviceProfile,
    val missingModels: List<LocalModelId>,
    val limitationNotices: List<String>
)

// ---------------------------------------------------------------------------
// Manager
// ---------------------------------------------------------------------------

/**
 * Aggregates [OnDeviceModelManager] and [DeviceCapabilityManager] into a single query point.
 */
class OfflineAIAvailabilityManager(
    private val context: Context? = null,
    private val modelManager: OnDeviceModelManager,
    private val capabilityManager: DeviceCapabilityManager
) {

    /**
     * Builds an [AIAvailabilityStatus] snapshot.
     */
    fun getStatus(): AIAvailabilityStatus {
        val report = capabilityManager.assess()
        val profile = report.profile
        val notices = mutableListOf<String>()
        val missingModels = mutableListOf<LocalModelId>()

        // ------------------------------------------------------------------ //
        // Speech transcription: Whisper Tiny (~39MB) or Moonshine (~26MB)     //
        // ------------------------------------------------------------------ //
        val whisperInstalled = modelManager.isInstalled(LocalModelId.WHISPER_TINY_EN)
        val moonshineInstalled = modelManager.isInstalled(LocalModelId.MOONSHINE_TINY_INT8)
        val canTranscribeSpeech = report.canRunWhisper && (whisperInstalled || moonshineInstalled)

        if (!canTranscribeSpeech) {
            missingModels += LocalModelId.WHISPER_TINY_EN
            notices += "Speech recognition is optional (~39 MB). Audio-only signal metrics (pace, pauses, volume, pitch) and speaking drills work fully without downloading any model."
        }

        // ------------------------------------------------------------------ //
        // Computer vision: MediaPipe Face, Pose Lite, Hand                   //
        // ------------------------------------------------------------------ //
        val faceInstalled = modelManager.isInstalled(LocalModelId.MEDIAPIPE_FACE)
        val poseInstalled = modelManager.isInstalled(LocalModelId.MEDIAPIPE_POSE)
        val handInstalled = modelManager.isInstalled(LocalModelId.MEDIAPIPE_HAND)
        val canAnalyzeVideo = report.canRunMediaPipe && faceInstalled && poseInstalled && handInstalled

        if (!report.canRunMediaPipe) {
            notices += "Video posture analysis limited: device requires Android 7.0+ (API 24+) and â‰¥ 256 MB free RAM."
        } else if (!canAnalyzeVideo) {
            if (!faceInstalled) missingModels += LocalModelId.MEDIAPIPE_FACE
            if (!poseInstalled) missingModels += LocalModelId.MEDIAPIPE_POSE
            if (!handInstalled) missingModels += LocalModelId.MEDIAPIPE_HAND
            notices += "Some visual landmark models are missing; video feedback will use self-review checklist cues."
        }

        // ------------------------------------------------------------------ //
        // Local Feedback Engine: Always available, 0 MB required!             //
        // ------------------------------------------------------------------ //
        val canGenerateLocalFeedback = true

        return AIAvailabilityStatus(
            canTranscribeSpeech = canTranscribeSpeech,
            canAnalyzeVideo = canAnalyzeVideo,
            canGenerateLocalFeedback = canGenerateLocalFeedback,
            canRunSLM = false,
            activeProfile = profile,
            missingModels = missingModels,
            limitationNotices = notices
        )
    }
}
