package com.example.data.ai.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Model download safety (AI-SEC-19, threat THR-37, TC-AI-021): only models with a pinned SHA-256 are
 * downloaded, a downloaded model without a pinned hash is never trusted, and the pinned hashes of the
 * bundled models match the files shipped in the APK.
 */
class ModelDownloadIntegrityTest {

    private val modelManager = OnDeviceModelManager(context = null)
    private val capabilityManager = DeviceCapabilityManager(context = null)

    private val advanced = DeviceCapabilityReport(
        profile = DeviceProfile.ADVANCED,
        apiLevel = 34,
        estimatedRamMb = 6144L,
        availableStorageMb = 10240L,
        hasNpu = true,
        thermalStatus = 0,
        batteryPct = 100,
        canRunWhisper = true,
        canRunMediaPipe = true,
        canRunConcurrently = true
    )

    @Test
    fun tcInt001_modelsWithoutAPinnedHash_areNeverDownloaded() = runBlocking {
        val requested = mutableListOf<String>()
        val installer = DeviceAdaptiveModelInstaller(
            modelManager = modelManager,
            capabilityManager = capabilityManager,
            downloader = ModelDownloader { url -> requested += url; null },
            isUnmeteredNetwork = { true }
        )
        val plan = installer.determinePlan(advanced)
        assertNull("No local ASR model is ever selected (FND-AI-03)", plan.selectedSpeechModel)

        val result = installer.installSuitableModels(plan)

        assertTrue(result.isSuccess)
        assertTrue("no unpinned model may be fetched, got $requested", requested.isEmpty())
        assertFalse(modelManager.isInstalled(LocalModelId.WHISPER_TINY_EN))
        assertTrue(installer.state.value is ModelProvisioningState.Completed)
    }

    @Test
    fun tcInt002_aDownloadedModelWithoutAPinnedHash_failsTheIntegrityCheck() = runBlocking {
        val spec = modelManager.modelCatalogue.getValue(LocalModelId.WHISPER_TINY_EN)
        assertTrue("Whisper has no pinned hash yet", spec.sha256Hash.isBlank())
        val file = File(modelManager.modelsDir(), spec.fileName)
        try {
            file.writeBytes(ByteArray(1024) { it.toByte() })
            assertFalse(modelManager.verifyIntegrity(LocalModelId.WHISPER_TINY_EN))
        } finally {
            file.delete()
        }
    }

    @Test
    fun tcInt003_pinnedHashesOfBundledModels_matchTheShippedFiles() {
        val moduleDir = File(System.getProperty("user.dir") ?: ".").let { if (File(it, "src/main/assets").exists()) it else File(it, "app") }
        for (id in listOf(LocalModelId.MEDIAPIPE_FACE, LocalModelId.MEDIAPIPE_POSE)) {
            val spec = modelManager.modelCatalogue.getValue(id)
            val asset = File(moduleDir, "src/main/assets/models/${spec.fileName}")
            val sha = MessageDigest.getInstance("SHA-256").digest(asset.readBytes()).joinToString("") { "%02x".format(it) }
            assertEquals("pinned hash of ${spec.fileName}", spec.sha256Hash, sha)
            assertEquals("size of ${spec.fileName}", spec.fileSizeBytes, asset.length())
        }
    }
}
