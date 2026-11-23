package com.example.speech.pipeline

import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.TargetBands
import com.example.speech.rules.IssueCode

/**
 * Per-technique or per-activity adjustment of issue detection (ARC-03 §6.7–§6.8 `issueOverrides`,
 * consistency item CL-60): feedback must not contradict the technique being practised, e.g. a
 * deliberate-pause technique must not be told its pauses are a mistake.
 *
 * Overrides never change metric values, labels or which metrics exist; they only change whether a
 * metric is interpreted as a mistake, and they cannot add issue codes.
 */
sealed class IssueOverride {
    /** The issue is not raised for attempts on this activity or technique. */
    object Suppress : IssueOverride() {
        override fun toString(): String = "SUPPRESS"
    }

    /**
     * The issue's source metric is checked against [min]–[max] (in the metric's band unit, see
     * [TargetBands]) instead of the default band. Only the side of the band that this issue checks
     * moves: `ISSUE_PACE_SLOW` uses [min], `ISSUE_PACE_FAST` uses [max], and so on ([IssueOverrides.REBANDABLE]).
     */
    data class Reband(val min: Double, val max: Double) : IssueOverride() {
        init {
            require(min < max) { "REBAND needs min < max (got $min–$max)" }
        }

        val width: Double get() = max - min
    }
}

/** One authored `issueOverrides` entry of a technique or activity record. */
data class IssueOverrideEntry(val issueCode: IssueCode, val override: IssueOverride)

object IssueOverrides {

    /** Which side of its source metric's band an issue checks (ARC-03 §11.22). */
    enum class BandSide { MIN, MAX }

    /** Source metric, checked side and plausible band range of an issue that can be re-banded (V-22). */
    data class RebandSource(
        val metric: SpeechMetricKey,
        val side: BandSide,
        val plausible: ClosedFloatingPointRange<Double>
    )

    /**
     * Issues whose source metric has a numeric band. Other issues (recognition, structure, grammar,
     * recording level, video) can be suppressed but not re-banded; content validation rejects a
     * `REBAND` on them (V-22).
     */
    val REBANDABLE: Map<IssueCode, RebandSource> = mapOf(
        IssueCode.ISSUE_PACE_SLOW to RebandSource(SpeechMetricKey.M_WPM, BandSide.MIN, 0.0..400.0),
        IssueCode.ISSUE_PACE_FAST to RebandSource(SpeechMetricKey.M_WPM, BandSide.MAX, 0.0..400.0),
        IssueCode.ISSUE_PAUSES_LONG to RebandSource(SpeechMetricKey.M_PAUSE, BandSide.MAX, 0.0..30.0),
        IssueCode.ISSUE_FILLERS_HIGH to RebandSource(SpeechMetricKey.M_FILL, BandSide.MAX, 0.0..30.0),
        IssueCode.ISSUE_HEDGING_HIGH to RebandSource(SpeechMetricKey.M_HEDGE, BandSide.MAX, 0.0..50.0),
        IssueCode.ISSUE_LOW_VARIETY to RebandSource(SpeechMetricKey.M_LEX, BandSide.MIN, 0.0..1.0)
    )

    /**
     * Content validation rule V-22 for the overrides of one record. Returns one message per problem;
     * an empty list means the record is valid. (Unknown issue codes are rejected when parsing.)
     */
    fun validate(recordId: String, entries: List<IssueOverrideEntry>): List<String> {
        val problems = mutableListOf<String>()
        entries.groupBy { it.issueCode }.filterValues { it.size > 1 }.keys.forEach {
            problems += "$recordId: duplicate issueOverrides entry for $it"
        }
        entries.forEach { entry ->
            val reband = entry.override as? IssueOverride.Reband ?: return@forEach
            val source = REBANDABLE[entry.issueCode]
            if (source == null) {
                problems += "$recordId: ${entry.issueCode} has no numeric band and cannot be re-banded"
            } else if (reband.min < source.plausible.start || reband.max > source.plausible.endInclusive) {
                problems += "$recordId: REBAND ${reband.min}–${reband.max} for ${entry.issueCode} is outside " +
                    "the plausible range ${source.plausible.start}–${source.plausible.endInclusive}"
            }
        }
        return problems
    }

    /**
     * Resolves the overrides that apply to one attempt (ARC-03 §6.8 precedence, deterministic for a
     * given content version):
     * 1. an activity's own entry beats any technique entry for the same issue;
     * 2. across the activity's techniques, `SUPPRESS` beats `REBAND`;
     * 3. among `REBAND`s, the widest band wins (ties: lower min, then higher max).
     */
    fun resolve(
        activity: List<IssueOverrideEntry>,
        techniques: List<List<IssueOverrideEntry>>
    ): Map<IssueCode, IssueOverride> {
        val resolved = linkedMapOf<IssueCode, IssueOverride>()
        techniques.flatten().groupBy { it.issueCode }.toSortedMap().forEach { (code, entries) ->
            val overrides = entries.map { it.override }
            resolved[code] = if (overrides.any { it is IssueOverride.Suppress }) {
                IssueOverride.Suppress
            } else {
                overrides.filterIsInstance<IssueOverride.Reband>().sortedWith(
                    compareByDescending<IssueOverride.Reband> { it.width }
                        .thenBy { it.min }
                        .thenByDescending { it.max }
                ).first()
            }
        }
        activity.forEach { resolved[it.issueCode] = it.override }
        return resolved
    }

    /**
     * Target bands after applying the `REBAND` overrides. If re-banding the two pace issues would leave
     * no valid pace band (min ≥ max), both pace overrides are ignored and the default pace band is used.
     */
    fun targetBands(overrides: Map<IssueCode, IssueOverride>): TargetBands {
        fun rebandOf(code: IssueCode) = overrides[code] as? IssueOverride.Reband
        val d = TargetBands.DEFAULT
        var wpmMin = rebandOf(IssueCode.ISSUE_PACE_SLOW)?.min ?: d.wpmMin
        var wpmMax = rebandOf(IssueCode.ISSUE_PACE_FAST)?.max ?: d.wpmMax
        if (wpmMin >= wpmMax) {
            wpmMin = d.wpmMin
            wpmMax = d.wpmMax
        }
        return TargetBands(
            wpmMin = wpmMin,
            wpmMax = wpmMax,
            pausesPerMinMax = rebandOf(IssueCode.ISSUE_PAUSES_LONG)?.max ?: d.pausesPerMinMax,
            fillersPerMinMax = rebandOf(IssueCode.ISSUE_FILLERS_HIGH)?.max ?: d.fillersPerMinMax,
            hedgesPer100Max = rebandOf(IssueCode.ISSUE_HEDGING_HIGH)?.max ?: d.hedgesPer100Max,
            mattrMin = rebandOf(IssueCode.ISSUE_LOW_VARIETY)?.min ?: d.mattrMin
        )
    }

    /** Issues removed by `SUPPRESS`. */
    fun suppressed(overrides: Map<IssueCode, IssueOverride>): Set<IssueCode> =
        overrides.filterValues { it is IssueOverride.Suppress }.keys
}
