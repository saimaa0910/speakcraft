package com.example.speech.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** Synthetic-PCM tests for CMP-30 (ARC-05 §4.1, §6). 16 kHz mono; times in ms. */
class StreamingFeatureExtractorTest {

    private val rate = 16_000

    /** Builds PCM from segments of (durationMs, amplitude 0..1); amplitude 0 = low noise. */
    private fun pcm(vararg segments: Pair<Int, Double>, noise: Double = 0.0005): ShortArray {
        val out = ArrayList<Short>()
        var n = 0
        var seed = 12345L
        for ((ms, amp) in segments) {
            val count = rate * ms / 1000
            repeat(count) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                val rnd = (seed % 2001 - 1000) / 1000.0
                val tone = amp * sin(2 * PI * 220.0 * n / rate)
                val v = (tone + noise * rnd) * 32767.0
                out.add(v.coerceIn(-32768.0, 32767.0).toInt().toShort())
                n++
            }
        }
        return out.toShortArray()
    }

    @Test
    fun tcAco001_silenceOnly_hasNoSpeech() {
        val f = StreamingFeatureExtractor.analyse(pcm(3000 to 0.0))
        assertEquals(3000L, f.durationMs)
        assertEquals(0L, f.voicedDurationMs)
        assertFalse(f.hasSpeech)
        assertNull("no voiced frames → no level, never a default", f.meanActiveRms)
        assertTrue(f.pauses.isEmpty())
        assertNull(f.firstVoicedMs)
    }

    @Test
    fun tcAco002_continuousSpeech_measuresSpeakingTimeWithinTwoFrames() {
        val f = StreamingFeatureExtractor.analyse(pcm(500 to 0.0, 2000 to 0.3, 500 to 0.0))
        assertEquals(3000L, f.durationMs)
        assertTrue("voiced ${f.voicedDurationMs}", f.voicedDurationMs in 1960L..2040L)
        assertEquals(0, f.pauseCount)
        assertNotNull(f.meanActiveRms)
        assertTrue(f.meanActiveRms!! > f.noiseFloorRms * 10)
    }

    @Test
    fun tcAco003_longPauseInsideSpeech_isCounted_leadingAndTrailingSilenceIsNot() {
        val f = StreamingFeatureExtractor.analyse(
            pcm(1500 to 0.0, 1000 to 0.3, 1200 to 0.0, 1000 to 0.3, 2000 to 0.0)
        )
        assertEquals(1, f.pauseCount)
        val p = f.pauses.single()
        assertTrue("pause ${p.durationMs}", p.durationMs in 1160L..1240L)
        assertTrue("starts near 2500 ms: ${p.startMs}", p.startMs in 2480L..2540L)
        assertEquals(p.durationMs, f.longestPauseMs)
        assertTrue(f.firstVoicedMs!! in 1480L..1520L)
        assertTrue(f.lastVoicedMs!! in 4680L..4720L)
    }

    @Test
    fun tcAco004_shortGapsBetweenWords_stayInsideSpeech() {
        // 150 ms gaps (below the 200 ms hangover) must not split speech or count as pauses.
        val f = StreamingFeatureExtractor.analyse(pcm(400 to 0.3, 150 to 0.0, 400 to 0.3, 150 to 0.0, 400 to 0.3))
        assertEquals(0, f.pauseCount)
        assertTrue("voiced ${f.voicedDurationMs}", f.voicedDurationMs >= 1450L)
    }

    @Test
    fun tcAco005_gapBetweenHangoverAndOneSecond_isNotALongPause() {
        val f = StreamingFeatureExtractor.analyse(pcm(800 to 0.3, 700 to 0.0, 800 to 0.3))
        assertEquals(0, f.pauseCount)
        assertTrue("700 ms of silence excluded from speaking time", f.voicedDurationMs in 1560L..1640L)
    }

    @Test
    fun tcAco006_isolatedClick_isNotSpeech() {
        val f = StreamingFeatureExtractor.analyse(pcm(1000 to 0.0, 20 to 0.8, 1000 to 0.0))
        assertEquals(0L, f.voicedDurationMs)
    }

    @Test
    fun tcAco007_clipping_isMeasured() {
        val clean = StreamingFeatureExtractor.analyse(pcm(2000 to 0.3))
        assertEquals(0.0, clean.clippingRatio!!, 1e-9)

        val clipped = StreamingFeatureExtractor.analyse(pcm(2000 to 1.6)) // overdriven tone
        assertTrue("clipping ${clipped.clippingRatio}", clipped.clippingRatio!! > 0.01)
    }

    @Test
    fun tcAco008_chunkBoundaries_doNotChangeResults() {
        val samples = pcm(1000 to 0.0, 1500 to 0.25, 1100 to 0.0, 900 to 0.25, 500 to 0.0)
        val whole = StreamingFeatureExtractor.analyse(samples)

        val ex = StreamingFeatureExtractor()
        var i = 0
        val sizes = intArrayOf(1, 7, 333, 1024, 4096, 17)
        var s = 0
        while (i < samples.size) {
            val len = minOf(sizes[s++ % sizes.size], samples.size - i)
            ex.accept(samples.copyOfRange(i, i + len))
            i += len
        }
        val chunked = ex.finish()
        assertEquals(whole, chunked)
    }

    @Test
    fun tcAco009_quietSpeechInNoisyRoom_followsAdaptiveNoiseFloor() {
        // Speech 6x the background level is detected even with a raised noise floor.
        val f = StreamingFeatureExtractor.analyse(pcm(1000 to 0.0, 1500 to 0.06, 1000 to 0.0, noise = 0.01))
        assertTrue("voiced ${f.voicedDurationMs}", f.voicedDurationMs in 1400L..1600L)
        assertTrue(f.noiseFloorRms > 0.002)
    }

    @Test
    fun tcAco011_briefLeadIn_thenContinuousSpeech_isStillDetected() {
        // 300 ms of silence is under 10 % of the recording; the noise floor must still come from it.
        val f = StreamingFeatureExtractor.analyse(pcm(300 to 0.0, 4000 to 0.3))
        assertTrue("voiced ${f.voicedDurationMs}", f.voicedDurationMs in 3900L..4100L)
    }

    @Test
    fun tcAco012_constantSignalWithNoQuietMoment_isNotClaimedAsSpeech() {
        // Indistinguishable from steady background noise: honest result is "no speech found".
        val f = StreamingFeatureExtractor.analyse(pcm(4000 to 0.3))
        assertEquals(0L, f.voicedDurationMs)
    }

    @Test
    fun tcAco010_finishTwice_isRejected() {
        val ex = StreamingFeatureExtractor()
        ex.accept(pcm(500 to 0.2))
        ex.finish()
        var threw = false
        try { ex.finish() } catch (e: IllegalStateException) { threw = true }
        assertTrue(threw)
    }
}
