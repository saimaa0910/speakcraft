package com.example

import com.example.data.ai.local.LocalModelId
import com.example.data.ai.local.OnDeviceModelManager
import com.example.data.ai.local.audio.SpeechRecognitionAvailability
import com.example.data.ai.local.audio.SpeechRecognitionManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpeechRecognitionManagerTest {

    @Test
    fun availability_check_defaults_to_audio_only_fallback_when_no_models_installed() {
        val manager = SpeechRecognitionManager(context = null, modelManager = null)
        val availability: SpeechRecognitionAvailability = manager.checkAvailability()

        assertFalse(availability.hasPrimaryRecognizer)
        assertFalse(availability.hasSecondaryOfflineRecognizer)
        assertEquals(null, availability.primaryModelId)
        assertEquals("audio-only-fallback", availability.activeRecognizerMode)
        assertTrue(availability.userFacingStatus.contains("audio-only", ignoreCase = true))
    }

    @Test
    fun transcribe_missing_file_returns_safe_failure_result() = runTest {
        val manager = SpeechRecognitionManager(context = null, modelManager = null)
        val missingFile = File("non_existent_audio_file.wav")

        val result = manager.transcribe(missingFile)
        assertNotNull(result)
        assertEquals("", result.transcript)
        assertEquals(0, result.wordCount)
        assertEquals("none", result.recognitionMode)
        assertTrue(result.limitationNotice.contains("not found", ignoreCase = true))
    }

    @Test
    fun audio_only_fallback_handles_audio_files_without_crashing() = runTest {
        // Create an empty temporary file representing recorded audio
        val tempAudio = File.createTempFile("test_audio_", ".m4a")
        tempAudio.deleteOnExit()

        val manager = SpeechRecognitionManager(context = null, modelManager = null)
        val result = manager.transcribe(tempAudio)

        assertNotNull(result)
        assertEquals("", result.transcript)
        assertEquals(0, result.wordCount)
        assertEquals("audio-only", result.recognitionMode)
        assertTrue(result.limitationNotice.contains("Audio volume, pauses", ignoreCase = true))
        // Verify privacy guarantee: no remote upload notice
        assertFalse(result.limitationNotice.contains("cloud", ignoreCase = true))
    }

    @Test
    fun model_integrity_validation_passes_when_no_corrupt_files_exist() = runTest {
        val modelManager = OnDeviceModelManager(context = null)
        val manager = SpeechRecognitionManager(context = null, modelManager = modelManager)

        val isValid = manager.validateModelIntegrity()
        assertTrue(isValid)
    }
}
