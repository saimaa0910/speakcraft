package com.example.speech.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tier T2/T3a acoustic evidence from `onRmsChanged` readings (ARC-05 §3). */
class RmsSeriesFeatureExtractorTest {

    /** Readings every 60 ms: (fromMs until toMs) at the given dB level. */
    private fun series(vararg parts: Triple<Long, Long, Float>): List<RmsSample> =
        parts.flatMap { (from, to, db) -> (from until to step 60L).map { RmsSample(it, db) } }

    @Test
    fun tcAco011_speechAndPause_fromRmsCallbacks() {
        val samples = series(
            Triple(0L, 800L, -2f),      // silence before speaking
            Triple(800L, 2800L, 8f),    // speech
            Triple(2800L, 4300L, -2f),  // 1.5 s pause
            Triple(4300L, 6300L, 8f),   // speech
            Triple(6300L, 7000L, -2f)
        )
        val f = RmsSeriesFeatureExtractor.analyse(samples, durationMs = 7000L)

        assertEquals(FeatureSource.RMS_CALLBACKS, f.source)
        assertEquals(1, f.pauseCount)
        assertTrue("pause ${f.pauses.single().durationMs}", f.pauses.single().durationMs in 1400L..1600L)
        assertTrue("voiced ${f.voicedDurationMs}", f.voicedDurationMs in 3800L..4200L)
        assertNull("clipping is not observable from RMS callbacks", f.clippingRatio)
    }

    @Test
    fun tcAco012_noReadings_isNoSpeech_notDefaulted() {
        val f = RmsSeriesFeatureExtractor.analyse(emptyList(), durationMs = 3000L)
        assertEquals(0L, f.voicedDurationMs)
        assertNull(f.meanActiveRms)
    }

    @Test
    fun tcAco013_unsortedReadings_areHandled() {
        val ordered = series(Triple(0L, 600L, -2f), Triple(600L, 2600L, 9f), Triple(2600L, 3000L, -2f))
        val shuffled = ordered.reversed()
        assertEquals(
            RmsSeriesFeatureExtractor.analyse(ordered, 3000L),
            RmsSeriesFeatureExtractor.analyse(shuffled, 3000L)
        )
    }
}
