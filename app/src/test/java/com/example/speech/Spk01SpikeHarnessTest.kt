package com.example.speech

import com.example.speech.recognition.RecognitionError
import com.example.speech.spike.Spk01BenchmarkRunner
import com.example.speech.tier.SpeechTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Spk01SpikeHarnessTest {

    private val runner = Spk01BenchmarkRunner()

    @Test
    fun `computeWordErrorRate returns 0 for identical texts`() {
        val text = "the presentation was clear and concise"
        val wer = runner.computeWordErrorRate(text, text)
        assertEquals(0.0f, wer, 0.0001f)
    }

    @Test
    fun `computeWordErrorRate computes correct substitutions, insertions and deletions`() {
        val reference = "explain the core architectural concepts"
        // 1 substitution ("main" instead of "core"), 1 omission ("concepts" omitted)
        val hypothesis = "explain the main architectural"

        val wer = runner.computeWordErrorRate(hypothesis, reference)
        // 2 errors out of 4 reference words = 0.50
        assertEquals(0.50f, wer, 0.01f)
    }

    @Test
    fun `testAudioSourcePipeFeasibility succeeds on API 36`() {
        val isFeasible = runner.testAudioSourcePipeFeasibility()
        assertTrue("ParcelFileDescriptor audio-source pipe must be feasible on API 36", isFeasible)
    }

    @Test
    fun `aggregateDeviceRun generates valid benchmark statistics and recommendations`() {
        val result = runner.aggregateDeviceRun(
            deviceModel = "Pixel 8 Pro (Simulated)",
            apiLevel = 34,
            tier = SpeechTier.T1_FULL_ON_DEVICE,
            pipingSupported = true,
            latenciesMs = listOf(420L, 510L, 460L),
            restartGapsMs = listOf(80L, 95L, 70L),
            wers = listOf(0.05f, 0.08f, 0.04f)
        )

        assertEquals("Pixel 8 Pro (Simulated)", result.deviceModel)
        assertEquals(SpeechTier.T1_FULL_ON_DEVICE, result.detectedTier)
        assertTrue(result.isPipingSupported)
        assertTrue("Latency must be ~463 ms", result.avgFinalizationLatencyMs in 460L..470L)
        assertTrue("Recommendation must include GO for T1", result.recommendations.contains("GO for T1"))
    }

    @Test
    fun `RecognitionError maps platform error codes correctly`() {
        assertEquals(RecognitionError.NoMatch, RecognitionError.fromPlatformCode(7))
        assertEquals(RecognitionError.SpeechTimeout, RecognitionError.fromPlatformCode(6))
        assertEquals(RecognitionError.Network, RecognitionError.fromPlatformCode(2))
        assertEquals(RecognitionError.Busy, RecognitionError.fromPlatformCode(8))
        assertEquals(RecognitionError.Permission, RecognitionError.fromPlatformCode(9))
        assertEquals(RecognitionError.LanguageUnavailable, RecognitionError.fromPlatformCode(12))
        assertTrue(RecognitionError.fromPlatformCode(99) is RecognitionError.Unknown)
    }
}
