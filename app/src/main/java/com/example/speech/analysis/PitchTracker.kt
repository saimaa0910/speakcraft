package com.example.speech.analysis

import kotlin.math.max
import kotlin.math.min

/** Fundamental-frequency estimate for one analysis window. */
data class PitchEstimate(
    val f0Hz: Double,
    /** YIN cumulative-mean-normalised difference at the chosen lag: 0 = perfectly periodic, 1 = noise. */
    val aperiodicity: Double
)

/**
 * YIN fundamental-frequency estimator (de Cheveigné & Kawahara, 2002) for app-owned PCM (tier T1/T3b).
 *
 * Steps: difference function → cumulative-mean normalisation → first lag under [threshold] (then its local
 * minimum) → parabolic interpolation. Windows without a lag under the threshold are unvoiced (`null`), so
 * breath, noise and unvoiced consonants never produce a pitch value.
 *
 * Search range [minHz]–[maxHz] covers adult and child speaking voices. Pure Kotlin, allocation-free per call
 * after construction; about 0.15 M multiply-adds per 40 ms window at 16 kHz.
 */
class PitchTracker(
    private val sampleRateHz: Int = StreamingFeatureExtractor.DEFAULT_SAMPLE_RATE_HZ,
    val windowSize: Int = sampleRateHz * WINDOW_MS / 1000,
    private val minHz: Double = 70.0,
    private val maxHz: Double = 400.0,
    private val threshold: Double = 0.15
) {
    private val tauMin = max(2, (sampleRateHz / maxHz).toInt())
    private val tauMax = min((sampleRateHz / minHz).toInt(), windowSize / 2)
    private val diff = DoubleArray(tauMax + 1)
    private val cmnd = DoubleArray(tauMax + 1)

    init {
        require(tauMax > tauMin + 2) { "window too short for the pitch range" }
    }

    /** Estimates F0 of the first [windowSize] samples of [x]; `null` when the window is not periodic. */
    fun estimate(x: FloatArray): PitchEstimate? {
        require(x.size >= windowSize) { "need $windowSize samples" }
        val w = windowSize - tauMax
        for (tau in 1..tauMax) {
            var sum = 0.0
            for (j in 0 until w) {
                val d = x[j] - x[j + tau]
                sum += d * d
            }
            diff[tau] = sum
        }
        cmnd[0] = 1.0
        var running = 0.0
        for (tau in 1..tauMax) {
            running += diff[tau]
            cmnd[tau] = if (running > 0.0) diff[tau] * tau / running else 1.0
        }

        var tau = tauMin
        while (tau <= tauMax) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 <= tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                break
            }
            tau++
        }
        if (tau > tauMax) return null

        val refined = if (tau > 1 && tau < tauMax) {
            val a = cmnd[tau - 1]
            val b = cmnd[tau]
            val c = cmnd[tau + 1]
            val denom = a - 2 * b + c
            if (denom != 0.0) tau + 0.5 * (a - c) / denom else tau.toDouble()
        } else tau.toDouble()
        val f0 = sampleRateHz / refined
        if (f0 < minHz || f0 > maxHz) return null
        return PitchEstimate(f0, cmnd[tau])
    }

    companion object {
        /** Two periods of the lowest pitch (70 Hz ≈ 14.3 ms) plus margin. */
        const val WINDOW_MS = 40
    }
}
