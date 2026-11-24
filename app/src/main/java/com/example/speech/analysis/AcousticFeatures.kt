package com.example.speech.analysis

/**
 * Where the acoustic evidence came from (ARC-05 §3–§4).
 *
 * - [PCM]: app-owned 16 kHz PCM frames (tiers T1 and T3b), full analysis.
 * - [RMS_CALLBACKS]: recogniser `onRmsChanged` levels only (tier T2/T3a), relative levels and
 *   timing only; clipping cannot be observed.
 */
enum class FeatureSource { PCM, RMS_CALLBACKS }

/** A silence of at least [StreamingFeatureExtractor.LONG_PAUSE_MS] inside speech. */
data class PauseSpan(val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

/**
 * Acoustic evidence for one attempt, produced on the device by [StreamingFeatureExtractor] or
 * [RmsSeriesFeatureExtractor] and consumed by the evidence gate and metric engine (ARC-05 §5–§6).
 *
 * Every value is measured; nothing is defaulted. A value that cannot be observed is `null`
 * (SRS-BR-002) — for example [clippingRatio] for [FeatureSource.RMS_CALLBACKS].
 */
data class AcousticFeatures(
    val source: FeatureSource,
    /** Total analysed duration (M-DUR input). */
    val durationMs: Long,
    /** Sum of voiced time after smoothing (M-SPK input). */
    val voicedDurationMs: Long,
    /** Silences ≥ 1.0 s between the first and last voiced frame; leading/trailing silence excluded (M-PAUSE). */
    val pauses: List<PauseSpan>,
    /** Level of each voiced frame (linear RMS 0..1 for PCM; relative linear level for RMS callbacks). */
    val activeFrameRms: List<Double>,
    /** Estimated background level on the same scale as [activeFrameRms]. */
    val noiseFloorRms: Double,
    /** Share of samples at or near full scale; `null` when not observable. */
    val clippingRatio: Double?,
    /** Start of the first voiced frame, or `null` when no speech was found. */
    val firstVoicedMs: Long?,
    /** End of the last voiced frame, or `null` when no speech was found. */
    val lastVoicedMs: Long?,
    /** Pitch, loudness and rhythm evidence; only from app-owned PCM ([FeatureSource.PCM]), else `null`. */
    val vocal: VocalFeatures? = null
) {
    val pauseCount: Int get() = pauses.size
    val longestPauseMs: Long? get() = pauses.maxOfOrNull { it.durationMs }
    /** Mean level of voiced frames, or `null` when there were none (never a default). */
    val meanActiveRms: Double? get() = activeFrameRms.takeIf { it.isNotEmpty() }?.average()
    val hasSpeech: Boolean get() = voicedDurationMs > 0
}
