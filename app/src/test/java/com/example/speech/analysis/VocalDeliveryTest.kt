package com.example.speech.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sin
import kotlin.random.Random

/**
 * Candidate vocal-delivery metrics (ARC-05 §8): pitch tracking and the derived metrics are checked on synthetic
 * voice-like signals with known pitch, loudness and rhythm. Device accuracy is validated separately (SPK-01).
 */
class VocalDeliveryTest {

    private val rate = 16_000

    /** Voice-like harmonic signal (10 harmonics, 1/k amplitudes) following a pitch contour and an envelope. */
    private fun voice(seconds: Double, pitch: (Double) -> Double, envelope: (Double) -> Double = { 0.3 }): ShortArray {
        val n = (seconds * rate).toInt()
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / rate
            phase += 2 * PI * pitch(t) / rate
            var s = 0.0
            for (k in 1..10) s += sin(k * phase) / k
            (envelope(t) * s / 2.0 * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    private fun silence(seconds: Double) = ShortArray((seconds * rate).toInt())
    private fun concat(vararg parts: ShortArray) = parts.reduce { a, b -> a + b }
    private fun st(a: Double, b: Double) = 12 * ln(a / b) / ln(2.0)

    private fun window(samples: ShortArray, from: Int, size: Int) = FloatArray(size) { samples[from + it] / 32768f }

    @Test
    fun tcVoc001_pitchTracker_findsTheFundamentalOfVoiceLikeSignals() {
        val tracker = PitchTracker(rate)
        for (f0 in listOf(85.0, 120.0, 180.0, 250.0, 350.0)) {
            val s = voice(0.2, { f0 })
            val est = tracker.estimate(window(s, 800, tracker.windowSize))
            assertNotNull("no pitch for $f0 Hz", est)
            assertEquals("pitch for $f0 Hz", f0, est!!.f0Hz, f0 * 0.02)
        }
    }

    @Test
    fun tcVoc002_pitchTracker_returnsNullForNoiseAndSilence() {
        val tracker = PitchTracker(rate)
        val random = Random(7)
        val noise = FloatArray(tracker.windowSize) { (random.nextDouble(-0.3, 0.3)).toFloat() }
        assertNull(tracker.estimate(noise))
        assertNull(tracker.estimate(FloatArray(tracker.windowSize)))
    }

    @Test
    fun tcVoc003_pitchRange_monotoneIsNarrow_octaveGlideIsWide() {
        val flat = StreamingFeatureExtractor.analyse(concat(silence(0.3), voice(3.0, { 150.0 }), silence(0.3))).vocal!!
        assertTrue("flat range ${flat.pitchRangeSemitones}", flat.pitchRangeSemitones!! < 0.5)
        assertEquals(150.0, flat.medianPitchHz!!, 3.0)

        // 150 → 300 Hz linear glide over 3 s: p10 ≈ 165 Hz, p90 ≈ 285 Hz → ≈ 9.5 semitones.
        val glide = StreamingFeatureExtractor.analyse(concat(silence(0.3), voice(3.0, { 150.0 + 50.0 * it }), silence(0.3))).vocal!!
        assertEquals(st(285.0, 165.0), glide.pitchRangeSemitones!!, 1.0)

        val report = VocalDeliveryAnalyzer.report(glide)!!
        assertEquals("wide", report.metrics.getValue(VocalMetricKey.PITCH_RANGE).band)
        assertEquals("narrow", VocalDeliveryAnalyzer.report(flat)!!.metrics.getValue(VocalMetricKey.PITCH_RANGE).band)
    }

    @Test
    fun tcVoc004_volumeVariation_measuresTheLoudnessSpread() {
        // Alternate 0.08 and 0.32 amplitude every 0.5 s: 4× amplitude ≈ 12 dB.
        val s = concat(silence(0.3), voice(4.0, { 140.0 }, { t -> if ((t * 2).toInt() % 2 == 0) 0.08 else 0.32 }), silence(0.3))
        val v = StreamingFeatureExtractor.analyse(s).vocal!!
        assertEquals(12.0, v.volumeVariationDb!!, 1.5)
        val steady = StreamingFeatureExtractor.analyse(concat(silence(0.3), voice(3.0, { 140.0 }), silence(0.3))).vocal!!
        assertTrue("steady ${steady.volumeVariationDb}", steady.volumeVariationDb!! < 1.0)
    }

    @Test
    fun tcVoc005_phraseEndings_risingAndFalling() {
        fun phrase(end: Double) = voice(1.6, { t -> if (t < 1.2) 160.0 else 160.0 + (end - 160.0) * (t - 1.2) / 0.4 })
        val rising = StreamingFeatureExtractor.analyse(
            concat(silence(0.3), phrase(220.0), silence(0.5), phrase(220.0), silence(0.5), phrase(220.0), silence(0.5))
        ).vocal!!
        assertEquals(3, rising.phraseEndings.size)
        assertTrue(rising.phraseEndings.all { it.direction == EndingDirection.RISING })
        assertEquals(1.0, rising.risingEndingShare!!, 0.0)

        val falling = StreamingFeatureExtractor.analyse(
            concat(silence(0.3), phrase(115.0), silence(0.5), phrase(115.0), silence(0.5), phrase(160.0), silence(0.5))
        ).vocal!!
        assertEquals(listOf(EndingDirection.FALLING, EndingDirection.FALLING, EndingDirection.LEVEL), falling.phraseEndings.map { it.direction })
        assertEquals(0.0, falling.risingEndingShare!!, 0.0)
    }

    @Test
    fun tcVoc006_articulationRate_countsSyllableLikePulses() {
        // Loudness pulses at 4 per second (deep dips between "syllables") for 3 s.
        val syllables = voice(3.0, { 150.0 }, { t -> 0.05 + 0.3 * (0.5 - 0.5 * kotlin.math.cos(2 * PI * 4.0 * t)) })
        val v = StreamingFeatureExtractor.analyse(concat(silence(0.3), syllables, silence(0.3))).vocal!!
        assertEquals(4.0, v.articulationRate!!, 0.6)
    }

    @Test
    fun tcVoc007_articulationVariation_evenVersusVaried() {
        fun pulses(perSecond: Double) = voice(2.0, { 150.0 }, { t -> 0.05 + 0.3 * (0.5 - 0.5 * kotlin.math.cos(2 * PI * perSecond * t)) })
        val even = StreamingFeatureExtractor.analyse(
            concat(silence(0.3), pulses(4.0), silence(0.5), pulses(4.0), silence(0.5), pulses(4.0), silence(0.3))
        ).vocal!!
        val varied = StreamingFeatureExtractor.analyse(
            concat(silence(0.3), pulses(2.5), silence(0.5), pulses(4.0), silence(0.5), pulses(6.0), silence(0.3))
        ).vocal!!
        assertTrue("even ${even.articulationRateVariation}", even.articulationRateVariation!! < 0.10)
        assertTrue("varied ${varied.articulationRateVariation}", varied.articulationRateVariation!! > 0.20)
    }

    @Test
    fun tcVoc008_notEnoughEvidence_givesNullNeverADefault() {
        val short = StreamingFeatureExtractor.analyse(concat(silence(0.3), voice(0.5, { 150.0 }), silence(0.3))).vocal!!
        assertNull(short.pitchRangeSemitones)
        assertNull(short.volumeVariationDb)
        assertNull(short.risingEndingShare)
        assertNull(short.articulationRateVariation)

        val silent = StreamingFeatureExtractor.analyse(silence(2.0)).vocal
        assertTrue(silent == null || (silent.pitchRangeSemitones == null && silent.pitchedFrames == 0))
        val rmsSeries = List(80) { RmsSample(it * 60L, if (it in 10..60) 6f + (it % 5) else -2f) }
        assertNull("RMS callbacks carry no pitch", RmsSeriesFeatureExtractor.analyse(rmsSeries, 4_800L).vocal)
    }

    @Test
    fun tcVoc009_candidateMetrics_areNeverApprovedWithoutSpk01Evidence() {
        VocalMetricKey.entries.forEach { assertEquals(ValidationStatus.CANDIDATE, VocalMetricValidation.status(it)) }
        val glide = StreamingFeatureExtractor.analyse(concat(silence(0.3), voice(3.0, { 150.0 + 50.0 * it }), silence(0.3))).vocal
        val report = VocalDeliveryAnalyzer.report(glide)!!
        assertTrue(report.metrics.isNotEmpty())
        assertTrue("nothing user-visible until validated", report.approvedMetrics.isEmpty())
    }

    @Test
    fun tcVoc010_validationHarness_appliesTheAcceptanceCriterion() {
        val good = List(30) { VocalReferenceSample("r$it", measured = 6.0 + (it % 3) * 0.4, reference = 6.2) }
        assertTrue(VocalMetricValidationHarness.evaluate(VocalMetricKey.PITCH_RANGE, good).qualifies)
        val tooFew = good.take(20)
        assertTrue(!VocalMetricValidationHarness.evaluate(VocalMetricKey.PITCH_RANGE, tooFew).qualifies)
        val inaccurate = List(30) { VocalReferenceSample("r$it", measured = if (it < 5) 12.0 else 6.0, reference = 6.0) }
        assertTrue(!VocalMetricValidationHarness.evaluate(VocalMetricKey.PITCH_RANGE, inaccurate).qualifies)
    }
}
