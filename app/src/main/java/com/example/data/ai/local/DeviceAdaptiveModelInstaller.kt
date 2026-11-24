package com.example.data.ai.local

import android.content.Context
import com.example.core.logging.AppLogger
import com.example.data.preferences.AISettingsRepository
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.RecognitionEnginePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "DeviceModelInstaller"

/**
 * Concrete deployment plan matching AI models to device hardware profile.
 */
data class DeviceModelPlan(
    val profile: DeviceProfile,
    val selectedSpeechModel: LocalModelId?,
    val selectedVideoModels: List<LocalModelId>,
    val recommendedEngine: RecognitionEnginePreference,
    val totalRequiredBytes: Long,
    val reason: String
)

/**
 * Observable lifecycle state for device-adaptive model installation.
 */
sealed interface ModelProvisioningState {
    data object Idle : ModelProvisioningState
    data class Assessing(val profile: DeviceProfile) : ModelProvisioningState
    data class Installing(val currentModel: LocalModelId, val progressPct: Int) : ModelProvisioningState
    data class Completed(val plan: DeviceModelPlan, val installedCount: Int, val summary: String) : ModelProvisioningState
    data class Failed(val reason: String, val fallbackActive: Boolean) : ModelProvisioningState
}

/**
 * Autonomous Device-Adaptive Model Provisioner & Installer.
 *
 * Automatically inspects device RAM, CPU/NPU, free storage, and Android API level to
 * install the optimal lightweight models for audio and video analysis:
 * - High-end (ADVANCED, ≥4 GB RAM, ≥300 MB free): Whisper Tiny EN (39MB) + full MediaPipe Face/Pose/Hand suite
 * - Mid-range (STANDARD, 2–4 GB RAM, ≥200 MB free): Moonshine Tiny INT8 (26MB) + MediaPipe Face/Pose suite
 * - Entry-level (BASIC, <2 GB RAM or low storage): Pure DSP audio + Android verified OS offline ASR + light face vision
 *
 * Strict zero-cloud-AI policy: all models run 100% locally on-device.
 */
class DeviceAdaptiveModelInstaller(
    private val context: Context? = null,
    private val modelManager: OnDeviceModelManager,
    private val capabilityManager: DeviceCapabilityManager,
    private val preferencesRepository: AppPreferencesRepository? = null,
    private val aiSettingsRepository: AISettingsRepository? = null
) {
    private val _state = MutableStateFlow<ModelProvisioningState>(ModelProvisioningState.Idle)
    val state: StateFlow<ModelProvisioningState> = _state.asStateFlow()

    /**
     * Determines the optimal model deployment plan tailored to the device's hardware profile.
     */
    fun determinePlan(report: DeviceCapabilityReport = capabilityManager.assess()): DeviceModelPlan {
        val ramMb = report.estimatedRamMb
        val storageMb = report.availableStorageMb
        val apiLevel = report.apiLevel
        val profile = report.profile

        // 1. Audio Analyser Speech Model Selection
        val selectedSpeechModel: LocalModelId?
        val recommendedEngine: RecognitionEnginePreference

        when {
            // ADVANCED tier: RAM >= 4GB and sufficient storage (>= 300MB) -> Whisper Tiny EN (39MB)
            profile == DeviceProfile.ADVANCED && storageMb >= 300L -> {
                selectedSpeechModel = LocalModelId.WHISPER_TINY_EN
                recommendedEngine = RecognitionEnginePreference.LOCAL_WHISPER
            }
            // STANDARD tier: RAM 2-4GB and sufficient storage (>= 200MB) -> Moonshine Tiny INT8 (26MB)
            profile == DeviceProfile.STANDARD && storageMb >= 200L -> {
                selectedSpeechModel = LocalModelId.MOONSHINE_TINY_INT8
                recommendedEngine = RecognitionEnginePreference.LOCAL_WHISPER
            }
            // BASIC tier or storage-constrained -> Zero extra download; use Android native offline recognizer
            else -> {
                selectedSpeechModel = null
                recommendedEngine = if (apiLevel >= 31) {
                    RecognitionEnginePreference.ANDROID_OFFLINE
                } else {
                    RecognitionEnginePreference.AUTO
                }
            }
        }

        // 2. Video Analyser Vision Models Selection
        val selectedVideoModels = mutableListOf<LocalModelId>()
        if (report.canRunMediaPipe) {
            when (profile) {
                DeviceProfile.BASIC -> {
                    selectedVideoModels += LocalModelId.MEDIAPIPE_FACE
                }
                DeviceProfile.STANDARD -> {
                    selectedVideoModels += LocalModelId.MEDIAPIPE_FACE
                    selectedVideoModels += LocalModelId.MEDIAPIPE_POSE
                }
                DeviceProfile.ADVANCED -> {
                    selectedVideoModels += LocalModelId.MEDIAPIPE_FACE
                    selectedVideoModels += LocalModelId.MEDIAPIPE_POSE
                    selectedVideoModels += LocalModelId.MEDIAPIPE_HAND
                }
            }
        }

        val speechBytes = selectedSpeechModel?.let { modelManager.downloadSize(it) } ?: 0L
        val videoBytes = selectedVideoModels.sumOf { modelManager.downloadSize(it) }
        val totalBytes = speechBytes + videoBytes

        val reason = buildString {
            append("Device profile: ${profile.name} (${ramMb}MB RAM, ${storageMb}MB free storage, API $apiLevel). ")
            if (selectedSpeechModel != null) {
                append("Configured for lightweight neural speech recognizer '${selectedSpeechModel.name}' (${speechBytes / (1024 * 1024)}MB). ")
            } else {
                append("Configured for native Android on-device speech engine to conserve memory. ")
            }
            append("Selected ${selectedVideoModels.size} video models (${selectedVideoModels.joinToString { it.name }}).")
        }

        return DeviceModelPlan(
            profile = profile,
            selectedSpeechModel = selectedSpeechModel,
            selectedVideoModels = selectedVideoModels,
            recommendedEngine = recommendedEngine,
            totalRequiredBytes = totalBytes,
            reason = reason
        )
    }

    /**
     * Installs and provisions all device-suitable models asynchronously.
     */
    suspend fun installSuitableModels(
        plan: DeviceModelPlan = determinePlan(),
        onProgress: ((Int, String) -> Unit)? = null
    ): Result<DeviceModelPlan> = withContext(Dispatchers.IO) {
        _state.value = ModelProvisioningState.Assessing(plan.profile)
        AppLogger.i(TAG, "Starting device-adaptive model installation: ${plan.reason}")

        var installedCount = 0
        val totalModels = (if (plan.selectedSpeechModel != null) 1 else 0) + plan.selectedVideoModels.size

        // Step 1: Provision video models
        for ((idx, videoModelId) in plan.selectedVideoModels.withIndex()) {
            val progress = if (totalModels > 0) ((idx * 100) / totalModels) else 0
            _state.value = ModelProvisioningState.Installing(videoModelId, progress)
            onProgress?.invoke(progress, "Installing video model: ${videoModelId.name}")

            val extracted = modelManager.extractBundledAssetIfPresent(videoModelId)
            if (extracted || modelManager.isInstalled(videoModelId)) {
                installedCount++
                AppLogger.d(TAG, "Video model $videoModelId verified and ready")
            } else {
                // Try downloading if not bundled in assets
                val downloaded = downloadModelIfAllowed(videoModelId)
                if (downloaded) installedCount++
            }
        }

        // Step 2: Provision speech model (if selected for this device tier)
        val speechModel = plan.selectedSpeechModel
        if (speechModel != null) {
            val progress = if (totalModels > 0) ((installedCount * 100) / totalModels) else 50
            _state.value = ModelProvisioningState.Installing(speechModel, progress)
            onProgress?.invoke(progress, "Setting up speech model: ${speechModel.name}")

            if (modelManager.isInstalled(speechModel)) {
                installedCount++
                AppLogger.d(TAG, "Speech model $speechModel already installed")
            } else {
                val extracted = modelManager.extractBundledAssetIfPresent(speechModel)
                if (extracted) {
                    installedCount++
                } else {
                    val downloaded = downloadModelIfAllowed(speechModel)
                    if (downloaded) {
                        installedCount++
                    } else {
                        AppLogger.w(TAG, "Speech model $speechModel setup deferred (offline/unavailable). Android OS recognizer will act as fallback.")
                    }
                }
            }
        }

        // Step 3: Automatically apply optimal settings to AISettings & AppPreferences
        runCatching {
            aiSettingsRepository?.setSelectedRecognitionEngine(plan.recommendedEngine)
            aiSettingsRepository?.setLocalVideoAnalysisEnabled(plan.selectedVideoModels.isNotEmpty())
            aiSettingsRepository?.setLocalSpeechRecognitionEnabled(true)
            aiSettingsRepository?.setLocalAudioAnalysisEnabled(true)
            preferencesRepository?.markInitialLaunchCompleted()
        }

        val summary = "Device optimization complete: ${plan.profile.name} tier active. $installedCount models ready."
        _state.value = ModelProvisioningState.Completed(plan, installedCount, summary)
        AppLogger.i(TAG, summary)
        onProgress?.invoke(100, summary)

        Result.success(plan)
    }

    /**
     * Automatically invoked on app install or first startup to ensure suitable models are ready.
     */
    suspend fun autoInstallOnFirstLaunch(): Boolean = withContext(Dispatchers.IO) {
        val prefs = runCatching { preferencesRepository?.preferencesFlow?.first() }.getOrNull()
        val alreadyCompleted = prefs?.hasCompletedInitialLaunch == true

        // If not completed or core models missing, run adaptive provisioning
        val report = capabilityManager.assess()
        val plan = determinePlan(report)
        val needsInstall = !alreadyCompleted || !areRequiredModelsInstalled(plan)

        if (needsInstall) {
            AppLogger.i(TAG, "Triggering automatic device-suitable model installation on launch...")
            val result = installSuitableModels(plan)
            result.isSuccess
        } else {
            AppLogger.d(TAG, "Device models already up to date for ${plan.profile.name} profile.")
            true
        }
    }

    private fun areRequiredModelsInstalled(plan: DeviceModelPlan): Boolean {
        for (videoModel in plan.selectedVideoModels) {
            if (!modelManager.isInstalled(videoModel)) return false
        }
        val speech = plan.selectedSpeechModel
        if (speech != null && !modelManager.isInstalled(speech)) {
            return false
        }
        return true
    }

    private fun downloadModelIfAllowed(id: LocalModelId): Boolean {
        val spec = modelManager.modelMetadata(id) ?: return false
        if (spec.downloadUrl.isBlank()) return false

        // Check LocalExecutionPolicy compliance
        if (LocalExecutionPolicy.isBlockedAiEndpoint(spec.downloadUrl)) {
            AppLogger.e(TAG, "Download URL ${spec.downloadUrl} violates LocalExecutionPolicy")
            return false
        }

        // Only download if sufficient storage is free (> 3x model size)
        if (modelManager.availableStorageBytes() < spec.fileSizeBytes * 3) {
            AppLogger.w(TAG, "Insufficient storage to download ${spec.displayName}")
            return false
        }

        return try {
            val url = URL(spec.downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 25_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "SpeakCraft-OnDevice-Installer/1.5.0")
            }

            if (connection.responseCode in 200..299) {
                connection.inputStream.use { stream ->
                    modelManager.installModelFromStream(id, stream)
                }
            } else {
                AppLogger.w(TAG, "Model download returned HTTP ${connection.responseCode} for $id")
                false
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Model download failed for $id: ${e.message}")
            false
        }
    }
}
