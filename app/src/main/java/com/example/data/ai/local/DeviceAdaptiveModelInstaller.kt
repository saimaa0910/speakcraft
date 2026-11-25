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
    /** Always `null`: no local speech model is ever selected; transcripts come from the platform recogniser. */
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
 * Inspects the device profile and Android API level to install the vision models suited to it:
 * - ADVANCED: MediaPipe Face/Pose/Hand · STANDARD: Face/Pose · BASIC: Face only.
 *
 * Speech recognition always uses the platform recogniser: no local ASR model is selected or installed, and the
 * recognition engine is never switched to a local model (FND-AI-03 — the former Whisper/Moonshine path was
 * fabricated and is removed).
 *
 * Safety rules (AI-SEC-19, threat THR-37, TC-AI-021; ARC-06):
 * - a model is downloaded only if its SHA-256 is pinned in the app, and the file is verified after download
 *   (deleted on mismatch); a model without a pinned hash is never downloaded;
 * - downloads happen only on an unmetered network;
 * - default settings are applied once, on the first provisioning, so later launches never override user choices;
 * - provisioning has its own completion marker and never touches the first-launch flag used by the launch screen.
 */
class DeviceAdaptiveModelInstaller(
    private val context: Context? = null,
    private val modelManager: OnDeviceModelManager,
    private val capabilityManager: DeviceCapabilityManager,
    private val preferencesRepository: AppPreferencesRepository? = null,
    private val aiSettingsRepository: AISettingsRepository? = null,
    private val downloader: ModelDownloader = HttpModelDownloader,
    private val isUnmeteredNetwork: () -> Boolean = { defaultIsUnmetered(context) }
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

        // 1. Speech: never a local ASR model. Transcripts come only from the platform recogniser (FND-AI-03: the
        // app-managed Whisper/Moonshine recogniser was fabricated and is removed; ADR-009).
        val selectedSpeechModel: LocalModelId? = null
        val recommendedEngine = if (apiLevel >= 31) {
            RecognitionEnginePreference.ANDROID_OFFLINE
        } else {
            RecognitionEnginePreference.AUTO
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

        val totalBytes = selectedVideoModels.sumOf { modelManager.downloadSize(it) }

        val reason = buildString {
            append("Device profile: ${profile.name} (${ramMb}MB RAM, ${storageMb}MB free storage, API $apiLevel). ")
            append("Speech recognition uses the device's own recogniser. ")
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
        onProgress: ((Int, String) -> Unit)? = null,
        /** True only on the first provisioning, so later runs never override the user's settings. */
        applyDefaultSettings: Boolean = true
    ): Result<DeviceModelPlan> = withContext(Dispatchers.IO) {
        _state.value = ModelProvisioningState.Assessing(plan.profile)
        AppLogger.i(TAG, "Starting device-adaptive model installation: ${plan.reason}")

        var installedCount = 0
        val totalModels = plan.selectedVideoModels.size

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

        // Step 2: Apply default settings once (first provisioning only). The recognition engine is left as the user
        // chose (default AUTO, which already prefers the platform's on-device recogniser when it is available).
        if (applyDefaultSettings) {
            runCatching {
                aiSettingsRepository?.setLocalVideoAnalysisEnabled(plan.selectedVideoModels.isNotEmpty())
                aiSettingsRepository?.setLocalAudioAnalysisEnabled(true)
                preferencesRepository?.markModelProvisioningCompleted()
            }
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
        // Own marker: the first-launch flag belongs to the launch screen and must not be changed here.
        val alreadyCompleted = prefs?.modelProvisioningCompleted == true

        // If not completed or core models missing, run adaptive provisioning
        val report = capabilityManager.assess()
        val plan = determinePlan(report)
        val needsInstall = !alreadyCompleted || !areRequiredModelsInstalled(plan)

        if (needsInstall) {
            AppLogger.i(TAG, "Triggering automatic device-suitable model installation on launch...")
            val result = installSuitableModels(plan, applyDefaultSettings = !alreadyCompleted)
            result.isSuccess
        } else {
            AppLogger.d(TAG, "Device models already up to date for ${plan.profile.name} profile.")
            true
        }
    }

    /** Only models that can actually be installed count: bundled ones and downloads with a pinned hash. */
    private fun areRequiredModelsInstalled(plan: DeviceModelPlan): Boolean =
        plan.selectedVideoModels
            .filter { isInstallable(it) }
            .all { modelManager.isInstalled(it) }

    private fun isInstallable(id: LocalModelId): Boolean {
        val spec = modelManager.modelMetadata(id) ?: return false
        return spec.bundledInAssets || spec.sha256Hash.isNotBlank()
    }

    private suspend fun downloadModelIfAllowed(id: LocalModelId): Boolean {
        val spec = modelManager.modelMetadata(id) ?: return false
        if (spec.downloadUrl.isBlank()) return false

        // AI-SEC-19: never download a model whose SHA-256 is not pinned in the app.
        if (spec.sha256Hash.isBlank()) {
            AppLogger.i(TAG, "Not downloading $id: no pinned SHA-256")
            return false
        }
        // Large files only on Wi-Fi or another unmetered network.
        if (!isUnmeteredNetwork()) {
            AppLogger.i(TAG, "Not downloading $id on a metered or unknown network")
            return false
        }

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

        val installed = try {
            val stream = downloader.open(spec.downloadUrl) ?: return false
            stream.use { modelManager.installModelFromStream(id, it) }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Model download failed for $id: ${e.message}")
            false
        }
        if (!installed) return false

        // Verify against the pinned hash before anything can load the file; delete it on mismatch.
        if (!modelManager.verifyIntegrity(id)) {
            AppLogger.w(TAG, "Downloaded $id failed its SHA-256 check; file removed")
            modelManager.deleteModel(id)
            return false
        }
        return true
    }
}

/** Opens a model download; replaceable in tests. Returns `null` for a non-2xx response. */
fun interface ModelDownloader {
    fun open(url: String): java.io.InputStream?
}

/** HTTPS download with timeouts. */
object HttpModelDownloader : ModelDownloader {
    override fun open(url: String): java.io.InputStream? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 25_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SpeakCraft-OnDevice-Installer")
        }
        return if (connection.responseCode in 200..299) connection.inputStream else {
            AppLogger.w(TAG, "Model download returned HTTP ${connection.responseCode}")
            connection.disconnect()
            null
        }
    }
}

/** True on Wi-Fi or another unmetered network; false when unknown (no context). */
private fun defaultIsUnmetered(context: Context?): Boolean {
    val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
    return runCatching { cm.activeNetwork != null && !cm.isActiveNetworkMetered }.getOrDefault(false)
}
