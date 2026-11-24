package com.example.speech.analysis

import com.example.speech.metrics.MetricMethodology
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/** Pitch movement at the end of a phrase (before a pause of ≥ [VocalDeliveryAnalyzer.PHRASE_GAP_MS]). */
enum class EndingDirection { FALLING, LEVEL, RISING }

data class PhraseEnding(val endMs: Long, val deltaSemitones: Double, val direction: EndingDirection)

/**
 * Vocal-delivery evidence from app-owned PCM (T1/T3b), computed on the device (ARC-05 §8 candidate acoustic
 * metrics). Every value is `null` when there was not enough evidence for it; nothing is defaulted (SRS-BR-002).
 */
data class VocalFeatures(
    /** Frames with a reliable pitch estimate inside speech. */
    val pitchedFrames: Int,
    val medianPitchHz: Double?,
    /** Spread of voiced pitch between the 10th and 90th percentiles, in semitones. */
    val pitchRangeSemitones: Double?,
    /** Spread of voiced-frame level between the 10th and 90th percentiles, in dB. Sensitive to mic distance. */
    val volumeVariationDb: Double?,
    /** Syllable nuclei (intensity peaks on voiced frames) per second of phrase time. */
    val articulationRate: Double?,
    /** Coefficient of variation of the per-phrase articulation rate (phrases ≥ 1.5 s; needs ≥ 3). */
    val articulationRateVariation: Double?,
    val phraseEndings: List<PhraseEnding>,
    /** Share of classified phrase endings that rise (needs ≥ 3 endings). */
    val risingEndingShare: Double?
)

/**
 * Candidate vocal-delivery metrics (ARC-05 §8 "Candidate acoustic metrics for vocal-delivery techniques").
 * They are **not** in the R1 catalogue until SPK-01 validates them on the RD-LOW/MID/HIGH reference devices:
 * the engine computes them and returns them with their [ValidationStatus], and feedback uses only APPROVED ones.
 * Bands use plain words; never "melody", "expressiveness" or "pronunciation" (ARC-05 §8).
 */
enum class VocalMetricKey(val code: String, val displayName: String, val methodology: MetricMethodology, val unit: String) {
    PITCH_RANGE("M-PITCH-RANGE", "Pitch range", MetricMethodology.MEASURED, "semitones"),
    VOLUME_VARIATION("M-VOL-VAR", "Volume variation", MetricMethodology.MEASURED, "dB"),
    ARTICULATION_RATE("M-ARTIC-RATE", "Syllable rate", MetricMethodology.ESTIMATED, "syllables/s"),
    ARTICULATION_VARIATION("M-ARTIC-VAR", "Pace variation", MetricMethodology.ESTIMATED, "cv"),
    RISING_ENDINGS("M-RISE-END", "Rising sentence endings", MetricMethodology.ESTIMATED, "share")
}

enum class ValidationStatus { CANDIDATE, APPROVED, REJECTED }

data class VocalMetricValue(
    val key: VocalMetricKey,
    val value: Double,
    val displayValue: String,
    /** Plain band word (†, tuned on SPK-01). */
    val band: String,
    val status: ValidationStatus
)

data class VocalDeliveryReport(
    val metrics: Map<VocalMetricKey, VocalMetricValue>,
    val phraseEndings: List<PhraseEnding>
) {
    /** Only validated metrics may be shown to users or drive feedback. */
    val approvedMetrics: Map<VocalMetricKey, VocalMetricValue>
        get() = metrics.filterValues { it.status == ValidationStatus.APPROVED }
}

/**
 * Validation state of each candidate metric. All are CANDIDATE: SPK-01 has not yet recorded accuracy results
 * on reference devices. A metric becomes APPROVED only with recorded results that meet its acceptance criterion
 * ([VocalMetricValidationHarness]), in the same way grammar rules qualify through SPK-02.
 */
object VocalMetricValidation {
    data class Record(val status: ValidationStatus, val evidence: String)

    val RECORDS: Map<VocalMetricKey, Record> = VocalMetricKey.entries.associateWith {
        Record(ValidationStatus.CANDIDATE, "Awaiting SPK-01 device recordings (ARC-05 §8)")
    }

    fun status(key: VocalMetricKey): ValidationStatus = RECORDS[key]?.status ?: ValidationStatus.CANDIDATE
}

/** One reference recording with a trusted value (e.g. measured in Praat by a reviewer) for one metric. */
data class VocalReferenceSample(val id: String, val measured: Double?, val reference: Double)

/** Result of comparing engine values with reference values for one metric (SPK-01). */
data class VocalValidationReport(
    val key: VocalMetricKey,
    val samples: Int,
    val withinTolerance: Int,
    val missing: Int,
    val meanAbsoluteError: Double?,
    val tolerance: Double
) {
    val shareWithinTolerance: Double get() = if (samples == 0) 0.0 else withinTolerance.toDouble() / samples

    /** Acceptance: ≥ 30 recordings, ≥ 90 % within tolerance, and a value produced for ≥ 90 % of them. */
    val qualifies: Boolean
        get() = samples >= MIN_SAMPLES && shareWithinTolerance >= MIN_SHARE && missing <= samples * (1 - MIN_SHARE)

    companion object {
        const val MIN_SAMPLES = 30
        const val MIN_SHARE = 0.90
    }
}

/** SPK-01 harness: measures a candidate metric against reference values with a per-metric tolerance. */
object VocalMetricValidationHarness {
    /** Tolerances † (agreed before measuring): semitones, dB, syllables/s, CV, share. */
    val TOLERANCE: Map<VocalMetricKey, Double> = mapOf(
        VocalMetricKey.PITCH_RANGE to 1.5,
        VocalMetricKey.VOLUME_VARIATION to 3.0,
        VocalMetricKey.ARTICULATION_RATE to 0.7,
        VocalMetricKey.ARTICULATION_VARIATION to 0.08,
        VocalMetricKey.RISING_ENDINGS to 0.20
    )

    fun evaluate(key: VocalMetricKey, samples: List<VocalReferenceSample>): VocalValidationReport {
        val tolerance = TOLERANCE.getValue(key)
        val errors = samples.mapNotNull { s -> s.measured?.let { abs(it - s.reference) } }
        return VocalValidationReport(
            key = key,
            samples = samples.size,
            withinTolerance = errors.count { it <= tolerance },
            missing = samples.count { it.measured == null },
            meanAbsoluteError = errors.takeIf { it.isNotEmpty() }?.average(),
            tolerance = tolerance
        )
    }
}

/**
 * Computes [VocalFeatures] from per-frame levels, per-frame pitch and the voice-activity mask produced by
 * [StreamingFeatureExtractor], and turns them into a [VocalDeliveryReport]. Pure and deterministic.
 */
object VocalDeliveryAnalyzer {

    /** Minimum pitched speech for a pitch value: 1 s. */
    const val MIN_PITCHED_FRAMES_MS = 1_000L
    /** Minimum voiced speech for a volume value: 1 s. */
    const val MIN_ACTIVE_MS = 1_000L
    /** A phrase is a voiced run of at least this length. */
    const val MIN_PHRASE_MS = 600L
    /** A phrase ending needs a following silence of at least this length (or the end of the recording). */
    const val PHRASE_GAP_MS = 300L
    /** Ending window and the reference window before it. */
    const val END_WINDOW_MS = 300L
    const val REFERENCE_WINDOW_MS = 600L
    /** Pitch change beyond ± this many semitones counts as rising/falling. † */
    const val ENDING_THRESHOLD_ST = 1.5
    /** Syllable nuclei need an intensity dip of at least this much between them. † */
    const val NUCLEUS_DIP_DB = 2.0
    /** Phrases at least this long contribute to articulation-rate variation. */
    const val RATE_PHRASE_MS = 1_500L

    fun analyse(levels: List<Double>, pitchHz: List<Double>, voiced: BooleanArray, frameMs: Long): VocalFeatures {
        val n = minOf(levels.size, pitchHz.size, voiced.size)
        val pitched = (0 until n).filter { voiced[it] && !pitchHz[it].isNaN() }.map { pitchHz[it] }
        val active = (0 until n).filter { voiced[it] && levels[it] > 0.0 }.map { levels[it] }

        val pitchRange = if (pitched.size * frameMs >= MIN_PITCHED_FRAMES_MS) {
            semitones(percentile(pitched, 0.90), percentile(pitched, 0.10))
        } else null
        val volumeVariation = if (active.size * frameMs >= MIN_ACTIVE_MS) {
            20.0 * log10(percentile(active, 0.90) / percentile(active, 0.10))
        } else null

        val phrases = phrases(voiced, n, frameMs)
        val endings = phrases.mapNotNull { ending(it, pitchHz, frameMs) }
        val classified = endings.size
        val rising = endings.count { it.direction == EndingDirection.RISING }

        val db = DoubleArray(n) { if (levels[it] > 0.0) 20.0 * log10(levels[it]) else -120.0 }
        val smooth = DoubleArray(n) { i ->
            val lo = max(0, i - 1)
            val hi = minOf(n - 1, i + 1)
            (lo..hi).sumOf { db[it] } / (hi - lo + 1)
        }
        val perPhrase = phrases.map { p -> p to nuclei(p, smooth, pitchHz) }
        val phraseSeconds = phrases.sumOf { (it.last - it.first + 1) * frameMs } / 1000.0
        val articulationRate = if (phraseSeconds >= 1.0) perPhrase.sumOf { it.second } / phraseSeconds else null
        val rates = perPhrase.filter { (p, _) -> (p.last - p.first + 1) * frameMs >= RATE_PHRASE_MS }
            .map { (p, count) -> count / ((p.last - p.first + 1) * frameMs / 1000.0) }
        val rateVariation = if (rates.size >= 3 && rates.average() > 0.0) {
            val mean = rates.average()
            sqrt(rates.sumOf { (it - mean) * (it - mean) } / rates.size) / mean
        } else null

        return VocalFeatures(
            pitchedFrames = pitched.size,
            medianPitchHz = pitched.takeIf { it.isNotEmpty() }?.let { percentile(it, 0.5) },
            pitchRangeSemitones = pitchRange,
            volumeVariationDb = volumeVariation,
            articulationRate = articulationRate,
            articulationRateVariation = rateVariation,
            phraseEndings = endings,
            risingEndingShare = if (classified >= 3) rising.toDouble() / classified else null
        )
    }

    /** Builds the report with plain bands (†) and each metric's validation status. */
    fun report(features: VocalFeatures?): VocalDeliveryReport? {
        features ?: return null
        val metrics = linkedMapOf<VocalMetricKey, VocalMetricValue>()
        fun put(key: VocalMetricKey, value: Double?, display: (Double) -> String, band: (Double) -> String) {
            value ?: return
            metrics[key] = VocalMetricValue(key, value, display(value), band(value), VocalMetricValidation.status(key))
        }
        put(VocalMetricKey.PITCH_RANGE, features.pitchRangeSemitones, { fmt("%.1f semitones", it) }) {
            when { it < 3.0 -> "narrow"; it <= 7.0 -> "moderate"; else -> "wide" }
        }
        put(VocalMetricKey.VOLUME_VARIATION, features.volumeVariationDb, { fmt("%.1f dB", it) }) {
            when { it < 6.0 -> "steady"; it <= 14.0 -> "moderate"; else -> "wide" }
        }
        put(VocalMetricKey.ARTICULATION_RATE, features.articulationRate, { fmt("%.1f syllables/s", it) }) {
            when { it < 3.0 -> "slow"; it <= 5.5 -> "typical"; else -> "fast" }
        }
        put(VocalMetricKey.ARTICULATION_VARIATION, features.articulationRateVariation, { fmt("%.2f", it) }) {
            if (it < 0.10) "even" else "varied"
        }
        put(VocalMetricKey.RISING_ENDINGS, features.risingEndingShare, { fmt("%.0f %%", it * 100) }) {
            when { it <= 0.33 -> "mostly falling or level"; it <= 0.66 -> "mixed"; else -> "mostly rising" }
        }
        return VocalDeliveryReport(metrics, features.phraseEndings)
    }

    private fun phrases(voiced: BooleanArray, n: Int, frameMs: Long): List<IntRange> {
        val minFrames = (MIN_PHRASE_MS / frameMs).toInt()
        val gapFrames = (PHRASE_GAP_MS / frameMs).toInt()
        val runs = ArrayList<IntRange>()
        var start = -1
        for (i in 0..n) {
            val v = i < n && voiced[i]
            if (v && start < 0) start = i
            if (!v && start >= 0) { runs += start until i; start = -1 }
        }
        // Merge runs separated by gaps shorter than the phrase gap.
        val merged = ArrayList<IntRange>()
        for (r in runs) {
            val last = merged.lastOrNull()
            if (last != null && r.first - last.last - 1 < gapFrames) merged[merged.size - 1] = last.first..r.last
            else merged += r
        }
        return merged.filter { it.last - it.first + 1 >= minFrames }
    }

    private fun ending(phrase: IntRange, pitchHz: List<Double>, frameMs: Long): PhraseEnding? {
        val endFrames = (END_WINDOW_MS / frameMs).toInt()
        val refFrames = (REFERENCE_WINDOW_MS / frameMs).toInt()
        val endStart = phrase.last - endFrames + 1
        val refStart = endStart - refFrames
        if (refStart < phrase.first) return null
        val end = (endStart..phrase.last).map { pitchHz[it] }.filter { !it.isNaN() }
        val ref = (refStart until endStart).map { pitchHz[it] }.filter { !it.isNaN() }
        if (end.size < endFrames / 3 || ref.size < refFrames / 3) return null
        val delta = semitones(percentile(end, 0.5), percentile(ref, 0.5))
        val direction = when {
            delta > ENDING_THRESHOLD_ST -> EndingDirection.RISING
            delta < -ENDING_THRESHOLD_ST -> EndingDirection.FALLING
            else -> EndingDirection.LEVEL
        }
        return PhraseEnding((phrase.last + 1) * frameMs, delta, direction)
    }

    /** Syllable nuclei: local intensity maxima on pitched frames separated by a dip of ≥ [NUCLEUS_DIP_DB]. */
    private fun nuclei(phrase: IntRange, db: DoubleArray, pitchHz: List<Double>): Int {
        val peaks = ArrayList<Int>()
        for (i in phrase) {
            val prev = if (i > phrase.first) db[i - 1] else Double.NEGATIVE_INFINITY
            val next = if (i < phrase.last) db[i + 1] else Double.NEGATIVE_INFINITY
            if (db[i] >= prev && db[i] > next && !pitchHz[i].isNaN()) {
                val last = peaks.lastOrNull()
                if (last == null) { peaks += i; continue }
                val dip = (last..i).minOf { db[it] }
                if (minOf(db[last], db[i]) - dip >= NUCLEUS_DIP_DB) peaks += i
                else if (db[i] > db[last]) peaks[peaks.size - 1] = i
            }
        }
        return peaks.size
    }

    private fun semitones(a: Double, b: Double): Double = 12.0 * ln(a / b) / ln(2.0)

    private fun percentile(values: List<Double>, p: Double): Double {
        val sorted = values.sorted()
        val idx = (p * (sorted.size - 1)).coerceIn(0.0, (sorted.size - 1).toDouble())
        val lo = idx.toInt()
        val hi = minOf(lo + 1, sorted.size - 1)
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (idx - lo)
    }

    private fun fmt(pattern: String, value: Double) = String.format(Locale.ROOT, pattern, value)
}
