package com.example.speech.rules

/** One labelled example for qualifying a grammar rule: recogniser-style text and whether the rule should fire. */
data class LabelledExample(val text: String, val shouldFire: Boolean)

/** Measured outcome of running one rule over a labelled corpus (SPK-02, ARC-05 §8.1). */
data class QualificationReport(
    val ruleId: String,
    val truePositives: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
    val trueNegatives: Int
) {
    val positives: Int get() = truePositives + falseNegatives
    val negatives: Int get() = falsePositives + trueNegatives
    /** Share of firings that were correct; `null` when the rule never fired. */
    val precision: Double? get() = (truePositives + falsePositives).takeIf { it > 0 }?.let { truePositives.toDouble() / it }
    /** Reported, not gated (ARC-05 §8.1). */
    val recall: Double? get() = positives.takeIf { it > 0 }?.let { truePositives.toDouble() / it }

    /** ARC-05 §8.1: ≥ 30 positive and ≥ 30 negative examples, and precision ≥ 0.90 (NFR-QUAL-003). */
    val qualifies: Boolean
        get() = positives >= RuleQualificationHarness.MIN_POSITIVES &&
            negatives >= RuleQualificationHarness.MIN_NEGATIVES &&
            (precision ?: 0.0) >= RuleQualificationHarness.MIN_PRECISION
}

/** Measures rule precision on a labelled corpus — the SPK-02 harness (WBS T-05.3). */
object RuleQualificationHarness {
    const val MIN_POSITIVES = 30
    const val MIN_NEGATIVES = 30
    const val MIN_PRECISION = 0.90

    fun evaluate(rule: GrammarRule, corpus: List<LabelledExample>): QualificationReport {
        var tp = 0; var fp = 0; var fn = 0; var tn = 0
        for (ex in corpus) {
            val fired = RuleEngine.evaluateGrammar(ex.text, listOf(rule)).isNotEmpty()
            when {
                fired && ex.shouldFire -> tp++
                fired && !ex.shouldFire -> fp++
                !fired && ex.shouldFire -> fn++
                else -> tn++
            }
        }
        return QualificationReport(rule.id, tp, fp, fn, tn)
    }
}

/** A recorded SPK-02 result that allows a rule into production (ARC-05 §8.1; versioned with evaluationVersion). */
data class RuleQualification(
    val ruleId: String,
    val precision: Double,
    val positives: Int,
    val negatives: Int,
    val corpusVersion: String,
    val measuredOn: String
)

/**
 * The only place a rule becomes active. Entries record SPK-02 measurements (corpus version, date)
 * and change together with an `evaluationVersion` bump. `Spk02QualificationTest` re-measures every
 * candidate on the corpus and fails if an entry differs from the fresh measurement.
 *
 * SPK02-v1 (2026-11-23) was authored by the development agent; a QA / Product Owner review of the
 * corpus is recommended (RPT-EP07-01).
 */
object RuleQualificationRegistry {
    private const val CORPUS = "SPK02-v1"
    private const val DATE = "2026-11-23"

    val QUALIFIED: Map<String, RuleQualification> = listOf(
        RuleQualification("R-AGREE-01", precision = 31.0 / 31, positives = 31, negatives = 31, corpusVersion = CORPUS, measuredOn = DATE),
        RuleQualification("R-AGREE-02", precision = 31.0 / 32, positives = 31, negatives = 31, corpusVersion = CORPUS, measuredOn = DATE),
        RuleQualification("R-COMP-01", precision = 31.0 / 33, positives = 31, negatives = 31, corpusVersion = CORPUS, measuredOn = DATE),
        RuleQualification("R-PAST-01", precision = 31.0 / 31, positives = 31, negatives = 31, corpusVersion = CORPUS, measuredOn = DATE),
        RuleQualification("R-PART-01", precision = 31.0 / 33, positives = 31, negatives = 31, corpusVersion = CORPUS, measuredOn = DATE),
        RuleQualification("R-ARTICLE-01", precision = 31.0 / 33, positives = 31, negatives = 31, corpusVersion = CORPUS, measuredOn = DATE)
    ).associateBy { it.ruleId }

    fun isQualified(ruleId: String): Boolean {
        val q = QUALIFIED[ruleId] ?: return false
        return q.positives >= RuleQualificationHarness.MIN_POSITIVES &&
            q.negatives >= RuleQualificationHarness.MIN_NEGATIVES &&
            q.precision >= RuleQualificationHarness.MIN_PRECISION
    }
}
