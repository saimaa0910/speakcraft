package com.example

import com.example.data.ai.local.DeviceCapabilityManager
import com.example.data.ai.local.DeviceProfile
import com.example.data.ai.local.LocalExecutionPolicy
import com.example.data.ai.local.LocalModelId
import com.example.data.ai.local.OfflineAIAvailabilityManager
import com.example.data.ai.local.OnDeviceModelManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LocalAIFoundationTest {

    @Test
    fun local_execution_policy_is_local_only() {
        assertEquals("LOCAL_ONLY", LocalExecutionPolicy.POLICY)
        assertTrue(LocalExecutionPolicy.isLocalOnly())
    }

    @Test
    fun local_execution_policy_blocks_cloud_ai_domains() {
        val blockedDomains = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models",
            "https://firebaseml.googleapis.com/v1/projects",
            "https://api.openai.com/v1/chat/completions",
            "https://api.anthropic.com/v1/messages",
            "https://api.groq.com/openai/v1/chat/completions",
            "https://huggingface.co/api/models"
        )

        blockedDomains.forEach { url ->
            val isBlocked = LocalExecutionPolicy.isBlockedAiEndpoint(url)
            assertTrue("Expected $url to be flagged as blocked AI endpoint", isBlocked)
        }

        // Allowed domains (Firebase Auth, Firestore, GitHub releases)
        val allowedUrls = listOf(
            "https://identitytoolkit.googleapis.com/v1/accounts",
            "https://firestore.googleapis.com/v1/projects",
            "https://api.github.com/repos/test/releases"
        )
        allowedUrls.forEach { url ->
            val isBlocked = LocalExecutionPolicy.isBlockedAiEndpoint(url)
            assertFalse("Expected $url to be allowed (non-AI endpoint)", isBlocked)
        }
    }

    @Test
    fun on_device_model_catalogue_contains_all_required_specs() {
        val manager = OnDeviceModelManager(context = null)
        val catalogue = manager.modelCatalogue

        assertEquals(5, catalogue.size)
        assertTrue(catalogue.containsKey(LocalModelId.WHISPER_TINY_EN))
        assertTrue(catalogue.containsKey(LocalModelId.MOONSHINE_TINY_INT8))
        assertTrue(catalogue.containsKey(LocalModelId.MEDIAPIPE_FACE))
        assertTrue(catalogue.containsKey(LocalModelId.MEDIAPIPE_POSE))
        assertTrue(catalogue.containsKey(LocalModelId.MEDIAPIPE_HAND))

        val whisper = catalogue[LocalModelId.WHISPER_TINY_EN]!!
        assertEquals("whisper-tiny-en.tflite", whisper.fileName)
        assertTrue(whisper.fileSizeBytes in 30_000_000..50_000_000)
        assertFalse(whisper.bundledInAssets)

        val moonshine = catalogue[LocalModelId.MOONSHINE_TINY_INT8]!!
        assertEquals("moonshine-tiny-int8.tflite", moonshine.fileName)
        assertTrue(moonshine.fileSizeBytes in 20_000_000..35_000_000)
        assertFalse(moonshine.bundledInAssets)

        val face = catalogue[LocalModelId.MEDIAPIPE_FACE]!!
        assertEquals("face_landmarker.task", face.fileName)
        assertTrue(face.bundledInAssets)

        val pose = catalogue[LocalModelId.MEDIAPIPE_POSE]!!
        assertEquals("pose_landmarker_lite.task", pose.fileName)
        assertTrue(pose.bundledInAssets)

        val hand = catalogue[LocalModelId.MEDIAPIPE_HAND]!!
        assertEquals("hand_landmarker.task", hand.fileName)
        assertTrue(hand.bundledInAssets)

        // Stage 1.2 Invariant: No large models (>100MB) in the catalog
        catalogue.values.forEach { spec ->
            assertTrue("Model ${spec.id} must be lightweight (<100MB)", spec.fileSizeBytes < 100_000_000L)
        }
    }

    @Test
    fun device_capability_profiles_tier_correctly() {
        val manager = DeviceCapabilityManager(context = null)

        // Basic: low RAM or old API
        assertEquals(DeviceProfile.BASIC, manager.computeProfile(totalRamMb = 1500, apiLevel = 24))
        assertEquals(DeviceProfile.BASIC, manager.computeProfile(totalRamMb = 6000, apiLevel = 23))

        // Standard: 2GB-4GB or API < 30
        assertEquals(DeviceProfile.STANDARD, manager.computeProfile(totalRamMb = 3000, apiLevel = 28))
        assertEquals(DeviceProfile.STANDARD, manager.computeProfile(totalRamMb = 6000, apiLevel = 28))

        // Advanced: >=4GB and API >= 30
        assertEquals(DeviceProfile.ADVANCED, manager.computeProfile(totalRamMb = 6000, apiLevel = 33))
        assertEquals(DeviceProfile.ADVANCED, manager.computeProfile(totalRamMb = 4096, apiLevel = 30))
    }

    @Test
    fun offline_ai_availability_evaluates_correctly() {
        val modelManager = OnDeviceModelManager(context = null)
        val capManager = DeviceCapabilityManager(context = null)
        val availManager = OfflineAIAvailabilityManager(
            context = null,
            modelManager = modelManager,
            capabilityManager = capManager
        )

        val status = availManager.getStatus()
        assertNotNull(status)
        assertNotNull(status.activeProfile)
        assertNotNull(status.missingModels)
        assertNotNull(status.limitationNotices)
        // With uninstalled models, missingModels should contain download-on-demand models
        assertTrue(status.missingModels.contains(LocalModelId.WHISPER_TINY_EN))
    }
}
