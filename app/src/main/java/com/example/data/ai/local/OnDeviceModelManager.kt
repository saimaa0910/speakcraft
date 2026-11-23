package com.example.data.ai.local

import android.content.Context
import android.os.StatFs
import com.example.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

private const val TAG = "LightweightModelMgr"

// ---------------------------------------------------------------------------
// Model identifiers (Stage 1.2: Strictly lightweight models only — Zero LLMs)
// ---------------------------------------------------------------------------

enum class LocalModelId {
    WHISPER_TINY_EN,
    MOONSHINE_TINY_INT8,
    MEDIAPIPE_FACE,
    MEDIAPIPE_POSE,
    MEDIAPIPE_HAND
}

// ---------------------------------------------------------------------------
// Model specification
// ---------------------------------------------------------------------------

/**
 * Immutable descriptor for an on-device AI model.
 *
 * @param id               Enum key for this model.
 * @param displayName      Human-readable name shown in UI.
 * @param fileName         File name used on-disk / in assets.
 * @param version          Semantic version or revision string of the model weights.
 * @param fileSizeBytes    Expected size in bytes.
 * @param sha256Hash       Hex-encoded SHA-256 checksum for integrity verification.
 * @param downloadUrl      URL from which the model can be fetched if not bundled.
 * @param license          SPDX license identifier.
 * @param minApiLevel      Minimum Android SDK version.
 * @param minRamMb         Minimum device RAM required in megabytes.
 * @param bundledInAssets  True when the model ships inside the APK's assets folder.
 */
data class LocalModelSpec(
    val id: LocalModelId,
    val displayName: String,
    val fileName: String,
    val version: String = "1.0.0",
    val fileSizeBytes: Long,
    val sha256Hash: String = "",
    val downloadUrl: String,
    val license: String,
    val minApiLevel: Int = 24,
    val minRamMb: Int = 256,
    val bundledInAssets: Boolean = false
)

// ---------------------------------------------------------------------------
// Lightweight Model Manager
// ---------------------------------------------------------------------------

/**
 * Manages lightweight on-device AI model lifecycles: discovery, verification,
 * integrity checks, deletion, unloading, and corrupt-model recovery.
 *
 * **Stage 1.2 Guarantee:** No large language models (>100MB) are permitted in this catalog.
 */
open class OnDeviceModelManager(protected val context: Context? = null) {

    // -----------------------------------------------------------------------
    // Catalogue (All models <= 39 MB download, zero LLMs)
    // -----------------------------------------------------------------------

    val modelCatalogue: Map<LocalModelId, LocalModelSpec> = mapOf(

        LocalModelId.WHISPER_TINY_EN to LocalModelSpec(
            id = LocalModelId.WHISPER_TINY_EN,
            displayName = "Whisper Tiny (English INT8/Float16)",
            fileName = "whisper-tiny-en.tflite",
            version = "2023.1",
            fileSizeBytes = 39_000_000L,
            sha256Hash = "",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
            license = "MIT",
            minApiLevel = 24,
            minRamMb = 512,
            bundledInAssets = false
        ),

        LocalModelId.MOONSHINE_TINY_INT8 to LocalModelSpec(
            id = LocalModelId.MOONSHINE_TINY_INT8,
            displayName = "Moonshine Tiny (English INT8)",
            fileName = "moonshine-tiny-int8.tflite",
            version = "1.0.0",
            fileSizeBytes = 26_000_000L,
            sha256Hash = "",
            downloadUrl = "https://huggingface.co/usefulsensors/moonshine/resolve/main/moonshine-tiny-int8.tflite",
            license = "Apache-2.0",
            minApiLevel = 24,
            minRamMb = 384,
            bundledInAssets = false
        ),

        LocalModelId.MEDIAPIPE_FACE to LocalModelSpec(
            id = LocalModelId.MEDIAPIPE_FACE,
            displayName = "MediaPipe Face Landmarker",
            fileName = "face_landmarker.task",
            version = "0.10.14",
            fileSizeBytes = 1_000_000L,
            sha256Hash = "",
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task",
            license = "Apache-2.0",
            minApiLevel = 24,
            minRamMb = 256,
            bundledInAssets = true
        ),

        LocalModelId.MEDIAPIPE_POSE to LocalModelSpec(
            id = LocalModelId.MEDIAPIPE_POSE,
            displayName = "MediaPipe Pose Landmarker Lite",
            fileName = "pose_landmarker_lite.task",
            version = "0.10.14",
            fileSizeBytes = 5_000_000L,
            sha256Hash = "",
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task",
            license = "Apache-2.0",
            minApiLevel = 24,
            minRamMb = 256,
            bundledInAssets = true
        ),

        LocalModelId.MEDIAPIPE_HAND to LocalModelSpec(
            id = LocalModelId.MEDIAPIPE_HAND,
            displayName = "MediaPipe Hand Landmarker",
            fileName = "hand_landmarker.task",
            version = "0.10.14",
            fileSizeBytes = 2_000_000L,
            sha256Hash = "",
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task",
            license = "Apache-2.0",
            minApiLevel = 24,
            minRamMb = 256,
            bundledInAssets = true
        )
    )

    // -----------------------------------------------------------------------
    // Directory
    // -----------------------------------------------------------------------

    fun modelsDir(): File = context?.let {
        File(it.filesDir, "ai_models").also { dir -> dir.mkdirs() }
    } ?: File(System.getProperty("java.io.tmpdir") ?: ".", "ai_models").also { dir -> dir.mkdirs() }

    // -----------------------------------------------------------------------
    // Installation queries
    // -----------------------------------------------------------------------

    fun isInstalled(id: LocalModelId): Boolean {
        val spec = modelCatalogue[id] ?: return false
        return if (spec.bundledInAssets) {
            true
        } else {
            File(modelsDir(), spec.fileName).exists()
        }
    }

    fun isAvailable(id: LocalModelId): Boolean = isInstalled(id)

    fun getModelFile(id: LocalModelId): File? {
        val spec = modelCatalogue[id] ?: return null
        if (spec.bundledInAssets) return null
        val file = File(modelsDir(), spec.fileName)
        return if (file.exists()) file else null
    }

    fun modelMetadata(id: LocalModelId): LocalModelSpec? = modelCatalogue[id]

    fun modelVersion(id: LocalModelId): String = modelCatalogue[id]?.version ?: "unknown"

    fun downloadSize(id: LocalModelId): Long = modelCatalogue[id]?.fileSizeBytes ?: 0L

    // -----------------------------------------------------------------------
    // Integrity & Recovery
    // -----------------------------------------------------------------------

    suspend fun verifyIntegrity(id: LocalModelId): Boolean = withContext(Dispatchers.IO) {
        val spec = modelCatalogue[id] ?: return@withContext false
        if (spec.bundledInAssets) return@withContext true
        if (spec.sha256Hash.isBlank()) return@withContext true

        val file = getModelFile(id) ?: return@withContext false

        try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            actualHash.equals(spec.sha256Hash, ignoreCase = true)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Integrity check failed for ${spec.fileName}: ${e.message}")
            false
        }
    }

    /**
     * If a model file is corrupted, deletes it so the user can re-download cleanly.
     */
    suspend fun recoverCorruptModel(id: LocalModelId): Boolean = withContext(Dispatchers.IO) {
        val valid = verifyIntegrity(id)
        if (!valid) {
            AppLogger.w(TAG, "Corrupt model detected for $id; removing file for recovery")
            deleteModel(id)
        } else {
            true
        }
    }

    // -----------------------------------------------------------------------
    // Deletion & Unloading
    // -----------------------------------------------------------------------

    suspend fun deleteModel(id: LocalModelId): Boolean = withContext(Dispatchers.IO) {
        val spec = modelCatalogue[id] ?: return@withContext false
        if (spec.bundledInAssets) return@withContext false

        val file = File(modelsDir(), spec.fileName)
        if (!file.exists()) return@withContext true
        file.delete()
    }

    /**
     * Unload notification hook for active sessions.
     */
    fun unloadModel(id: LocalModelId) {
        AppLogger.d(TAG, "Model $id unloaded from memory")
    }

    // -----------------------------------------------------------------------
    // Storage helpers
    // -----------------------------------------------------------------------

    fun getStorageRequiredBytes(id: LocalModelId): Long =
        modelCatalogue[id]?.fileSizeBytes ?: 0L

    fun availableStorageBytes(): Long {
        val statFs = StatFs(modelsDir().absolutePath)
        return statFs.availableBlocksLong * statFs.blockSizeLong
    }
}

/**
 * Explicit alias to [OnDeviceModelManager] for Stage 1.2 naming conventions.
 */
class LightweightModelManager(context: Context? = null) : OnDeviceModelManager(context)
