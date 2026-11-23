package com.example.speech.analysis

import kotlin.math.pow

/** One `RecognitionListener.onRmsChanged` reading: time since capture start and level in dB. */
data class RmsSample(val timeMs: Long, val rmsDb: Float)

/**
 * Acoustic evidence for tiers where the platform recogniser owns the microphone (T2, T3a), so the
 * app sees only `onRmsChanged` levels (ARC-05 §3 T2: "acoustic metrics from onRmsChanged + segment
 * timing").
 *
 * The dB scale of `onRmsChanged` is device- and recogniser-specific and not calibrated, so levels
 * are converted to a *relative* linear scale and only compared with each other: voice activity,
 * pauses and loudness consistency are meaningful; absolute loudness and clipping are not. The
 * returned [AcousticFeatures.clippingRatio] is therefore `null`.
 *
 * Readings arrive at irregular intervals (typically 10–20 per second); they are resampled onto a
 * fixed grid by holding the last reading.
 */
object RmsSeriesFeatureExtractor {

    /** Analysis grid for resampled callbacks. */
    const val GRID_MS = 50L

    /**
     * Absolute floor on the relative scale. Typical `onRmsChanged` values range from about −2 dB
     * (silence) to 10 dB (loud speech); 10^(−2/20) ≈ 0.8, so the floor is effectively "never" and
     * the adaptive noise-floor threshold decides.
     */
    private const val MIN_RELATIVE_LEVEL = 1e-6

    fun analyse(samples: List<RmsSample>, durationMs: Long): AcousticFeatures {
        require(durationMs >= 0) { "durationMs must not be negative" }
        if (samples.isEmpty() || durationMs == 0L) {
            return VoiceActivity.analyse(emptyList(), GRID_MS, durationMs, FeatureSource.RMS_CALLBACKS, null, MIN_RELATIVE_LEVEL)
        }
        val sorted = samples.sortedBy { it.timeMs }
        val cells = ((durationMs + GRID_MS - 1) / GRID_MS).toInt()
        val levels = ArrayList<Double>(cells)
        var idx = 0
        var current = sorted.first().rmsDb
        for (c in 0 until cells) {
            val cellEnd = (c + 1) * GRID_MS
            while (idx < sorted.size && sorted[idx].timeMs < cellEnd) {
                current = sorted[idx].rmsDb
                idx++
            }
            levels.add(10.0.pow(current / 20.0))
        }
        return VoiceActivity.analyse(levels, GRID_MS, durationMs, FeatureSource.RMS_CALLBACKS, null, MIN_RELATIVE_LEVEL)
    }
}
