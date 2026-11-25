package com.example.data.ai.local

import com.example.data.preferences.RecognitionEnginePreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeviceAdaptiveModelInstallerTest {

    private val modelManager = OnDeviceModelManager(context = null)
    private val capabilityManager = DeviceCapabilityManager(context = null)
    private val installer = DeviceAdaptiveModelInstaller(
        context = null,
        modelManager = modelManager,
        capabilityManager = capabilityManager
    )

    @Test
    fun `basic device profile selects native offline ASR and lightweight face model`() {
        val basicReport = DeviceCapabilityReport(
            profile = DeviceProfile.BASIC,
            apiLevel = 28,
            estimatedRamMb = 1536L, // 1.5 GB RAM
            availableStorageMb = 4096L,
            hasNpu = false,
            thermalStatus = 0,
            batteryPct = 85,
            canRunWhisper = true,
            canRunMediaPipe = true,
            canRunConcurrently = false
        )

        val plan = installer.determinePlan(basicReport)

        assertEquals(DeviceProfile.BASIC, plan.profile)
        assertNull("BASIC tier should not provision heavy neural speech model", plan.selectedSpeechModel)
        assertEquals(RecognitionEnginePreference.AUTO, plan.recommendedEngine)
        assertEquals(listOf(LocalModelId.MEDIAPIPE_FACE), plan.selectedVideoModels)
        assertTrue("Total download bytes should be small (< 5 MB)", plan.totalRequiredBytes < 5_000_000L)
        assertTrue(plan.reason.contains("BASIC"))
    }

    @Test
    fun `standard device profile selects face plus pose vision models and the platform recogniser`() {
        val standardReport = DeviceCapabilityReport(
            profile = DeviceProfile.STANDARD,
            apiLevel = 31,
            estimatedRamMb = 3072L, // 3 GB RAM
            availableStorageMb = 4096L,
            hasNpu = false,
            thermalStatus = 0,
            batteryPct = 90,
            canRunWhisper = true,
            canRunMediaPipe = true,
            canRunConcurrently = true
        )

        val plan = installer.determinePlan(standardReport)

        assertEquals(DeviceProfile.STANDARD, plan.profile)
        assertNull("No local ASR model is ever selected (FND-AI-03)", plan.selectedSpeechModel)
        assertEquals(RecognitionEnginePreference.ANDROID_OFFLINE, plan.recommendedEngine)
        assertTrue(plan.selectedVideoModels.contains(LocalModelId.MEDIAPIPE_FACE))
        assertTrue(plan.selectedVideoModels.contains(LocalModelId.MEDIAPIPE_POSE))
        assertFalse(plan.selectedVideoModels.contains(LocalModelId.MEDIAPIPE_HAND))
    }

    @Test
    fun `advanced device profile selects complete vision suite and never a local speech model`() {
        val advancedReport = DeviceCapabilityReport(
            profile = DeviceProfile.ADVANCED,
            apiLevel = 34,
            estimatedRamMb = 6144L, // 6 GB RAM
            availableStorageMb = 10240L,
            hasNpu = true,
            thermalStatus = 0,
            batteryPct = 100,
            canRunWhisper = true,
            canRunMediaPipe = true,
            canRunConcurrently = true
        )

        val plan = installer.determinePlan(advancedReport)

        assertEquals(DeviceProfile.ADVANCED, plan.profile)
        assertNull("No local ASR model is ever selected (FND-AI-03)", plan.selectedSpeechModel)
        assertEquals(RecognitionEnginePreference.ANDROID_OFFLINE, plan.recommendedEngine)
        assertTrue(plan.selectedVideoModels.contains(LocalModelId.MEDIAPIPE_FACE))
        assertTrue(plan.selectedVideoModels.contains(LocalModelId.MEDIAPIPE_POSE))
        assertTrue(plan.selectedVideoModels.contains(LocalModelId.MEDIAPIPE_HAND))
    }

    @Test
    fun `storage constrained device demotes speech model to preserve device space`() {
        val lowStorageReport = DeviceCapabilityReport(
            profile = DeviceProfile.ADVANCED,
            apiLevel = 33,
            estimatedRamMb = 6144L,
            availableStorageMb = 120L, // Only 120MB free storage (< 300MB threshold)
            hasNpu = true,
            thermalStatus = 0,
            batteryPct = 75,
            canRunWhisper = true,
            canRunMediaPipe = true,
            canRunConcurrently = true
        )

        val plan = installer.determinePlan(lowStorageReport)

        assertNull("Speech model should be omitted when storage is constrained", plan.selectedSpeechModel)
        assertEquals(RecognitionEnginePreference.ANDROID_OFFLINE, plan.recommendedEngine)
    }

    @Test
    fun `no profile ever selects a local ASR engine and a stored LOCAL_WHISPER preference behaves as automatic`() {
        for (profile in DeviceProfile.entries) {
            for (api in listOf(26, 30, 31, 34)) {
                val plan = installer.determinePlan(
                    DeviceCapabilityReport(
                        profile = profile, apiLevel = api, estimatedRamMb = 8192L, availableStorageMb = 20_000L,
                        hasNpu = true, thermalStatus = 0, batteryPct = 100, canRunWhisper = true,
                        canRunMediaPipe = true, canRunConcurrently = true
                    )
                )
                assertNull(plan.selectedSpeechModel)
                assertTrue(plan.recommendedEngine != RecognitionEnginePreference.LOCAL_WHISPER)
            }
        }
        assertEquals(RecognitionEnginePreference.AUTO, RecognitionEnginePreference.fromStored("LOCAL_WHISPER"))
        assertEquals(RecognitionEnginePreference.AUTO, RecognitionEnginePreference.fromStored(null))
        assertEquals(RecognitionEnginePreference.ANDROID_OFFLINE, RecognitionEnginePreference.fromStored("ANDROID_OFFLINE"))
        assertFalse(RecognitionEnginePreference.LOCAL_WHISPER in RecognitionEnginePreference.SELECTABLE)
    }

    @Test
    fun `installSuitableModels executes and completes with plan snapshot`() = runBlocking {
        val basicReport = DeviceCapabilityReport(
            profile = DeviceProfile.BASIC,
            apiLevel = 28,
            estimatedRamMb = 1536L,
            availableStorageMb = 4096L,
            hasNpu = false,
            thermalStatus = 0,
            batteryPct = 85,
            canRunWhisper = true,
            canRunMediaPipe = true,
            canRunConcurrently = false
        )

        val plan = installer.determinePlan(basicReport)
        val result = installer.installSuitableModels(plan)

        assertTrue(result.isSuccess)
        val completedPlan = result.getOrThrow()
        assertEquals(DeviceProfile.BASIC, completedPlan.profile)

        val currentState = installer.state.value
        assertTrue(currentState is ModelProvisioningState.Completed)
        val completed = currentState as ModelProvisioningState.Completed
        assertEquals(DeviceProfile.BASIC, completed.plan.profile)
        assertTrue(completed.summary.contains("BASIC"))
    }
}
