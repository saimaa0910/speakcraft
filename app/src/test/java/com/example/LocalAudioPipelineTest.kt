package com.example

import com.example.data.ai.local.audio.AudioFeatureExtractionEngine
import com.example.data.ai.local.audio.FillerWordDetectionEngine
import com.example.data.ai.local.audio.PronunciationAnalysisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class LocalAudioPipelineTest {

    @Test
    fun filler_word_detection_flags_exact_fillers_and_respects_word_boundaries() {
        val transcriptWithFillers = "I um think that you know basically we should like speak clearly."
        val result = FillerWordDetectionEngine.detect(transcriptWithFillers)

        assertTrue(result.totalFillerCount >= 4)
        assertTrue(result.detectedFillers.containsKey("um"))
        assertTrue(result.detectedFillers.containsKey("you know"))
        assertTrue(result.detectedFillers.containsKey("basically"))
        assertTrue(result.detectedFillers.containsKey("like"))

        // Word boundary check: "likely", "unlike", "umbrella" should NOT be flagged as fillers
        val nonFillerTranscript = "He is likely riding a bicycle with an umbrella and it is unlike anything else."
        val nonFillerResult = FillerWordDetectionEngine.detect(nonFillerTranscript)
        assertEquals(0, nonFillerResult.totalFillerCount)
        assertTrue(nonFillerResult.detectedFillers.isEmpty())

        // Case insensitivity
        val upperTranscript = "UM UH LIKE BASICALLY"
        val upperResult = FillerWordDetectionEngine.detect(upperTranscript)
        assertEquals(4, upperResult.totalFillerCount)
    }

    @Test
    fun pronunciation_analysis_computes_accuracy_and_maintains_honesty_notice() {
        val engine = PronunciationAnalysisEngine()

        // Exact match
        val perfect = engine.analyze(
            recognizedTranscript = "The quick brown fox jumps over the lazy dog",
            expectedText = "The quick brown fox jumps over the lazy dog"
        )
        assertTrue(perfect.transcriptMatch)
        assertEquals(1.0f, perfect.wordAccuracy, 0.01f)
        assertEquals("High", perfect.intelligibilityEstimate)
        assertTrue(perfect.likelyMispronounced.isEmpty())

        // Partial match with missing words
        val partial = engine.analyze(
            recognizedTranscript = "The quick brown cat jumps over dog",
            expectedText = "The quick brown fox jumps over the lazy dog"
        )
        assertFalse(partial.transcriptMatch)
        assertTrue(partial.wordAccuracy < 1.0f)
        assertTrue(partial.likelyMispronounced.contains("fox") || partial.likelyMispronounced.contains("lazy"))

        // Honest limitation notice must ALWAYS be present without claiming exact phoneme accuracy
        assertTrue(perfect.limitations.any { it.contains("phoneme", ignoreCase = true) })
        assertTrue(partial.limitations.any { it.contains("phoneme", ignoreCase = true) })
    }

    @Test
    fun audio_feature_extraction_computes_rms_vad_and_clipping() {
        val engine = AudioFeatureExtractionEngine()
        val sampleRate = 16000

        // 1 second of silence
        val silenceSamples = ShortArray(sampleRate) { 0 }
        val silenceFeatures = engine.extract(silenceSamples, sampleRate, transcript = "")

        assertEquals(1.0, silenceFeatures.durationSeconds, 0.05)
        assertEquals(0.0f, silenceFeatures.rmsEnergy, 0.001f)
        assertFalse(silenceFeatures.clippingDetected)
        assertEquals(0, silenceFeatures.wordCount)
        assertEquals(0, silenceFeatures.wordsPerMinute)

        // 1 second 200 Hz sine wave (speech fundamental frequency range)
        val sineSamples = ShortArray(sampleRate) { i ->
            val angle = 2.0 * Math.PI * 200.0 * i / sampleRate
            (16000.0 * sin(angle)).toInt().toShort()
        }
        val sineFeatures = engine.extract(sineSamples, sampleRate, transcript = "Hello world from SpeakCraft")

        assertTrue(sineFeatures.rmsEnergy > 0.1f)
        assertEquals(4, sineFeatures.wordCount)
        assertTrue(sineFeatures.wordsPerMinute > 0)
        assertFalse(sineFeatures.clippingDetected)

        // 0.5 second clipping signal
        val clippedSamples = ShortArray(sampleRate / 2) { 32700.toShort() }
        val clippedFeatures = engine.extract(clippedSamples, sampleRate, transcript = "")
        assertTrue(clippedFeatures.clippingDetected)
    }
}
