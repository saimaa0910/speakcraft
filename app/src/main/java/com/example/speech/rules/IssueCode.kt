package com.example.speech.rules

import com.example.speech.metrics.MetricMethodology

/**
 * Standardized issue codes defined in ARC-05 §8.4 (PRD-FR-047, SRS-FR-EVAL-015/016).
 * Priority controls display ordering (1 = shown first).
 */
enum class IssueCode(
    val priority: Int,
    val title: String,
    val methodology: MetricMethodology
) {
    ISSUE_TOO_QUIET(1, "Recording was quiet or muffled", MetricMethodology.MEASURED),
    ISSUE_WORD_NOT_RECOGNISED(2, "Some words were not recognised as intended", MetricMethodology.MEASURED),
    ISSUE_WRONG_FORM(2, "A different word form was used", MetricMethodology.MEASURED),
    ISSUE_TARGET_WORD_MISSING(2, "Target word was missing", MetricMethodology.MEASURED),
    ISSUE_STRUCT_ELEMENT_MISSING(3, "Framework element was missing", MetricMethodology.ESTIMATED),
    ISSUE_FILLERS_HIGH(4, "High number of filler words", MetricMethodology.ESTIMATED),
    ISSUE_PACE_FAST(5, "Speaking pace was fast", MetricMethodology.MEASURED),
    ISSUE_PACE_SLOW(5, "Speaking pace was slow", MetricMethodology.MEASURED),
    ISSUE_PAUSES_LONG(6, "Frequent or extended pauses", MetricMethodology.MEASURED),
    ISSUE_TOO_SHORT(7, "Response was shorter than target", MetricMethodology.MEASURED),
    ISSUE_GRAMMAR_POSSIBLE(8, "Possible grammar check hit", MetricMethodology.ESTIMATED),
    ISSUE_HEDGING_HIGH(9, "Frequent hedging phrases", MetricMethodology.ESTIMATED),
    ISSUE_LOW_VARIETY(10, "Vocabulary variety was repetitive", MetricMethodology.ESTIMATED),

    // Video issues (ARC-07 §6; D-42). Shown after audio issues; all Estimated (landmark models).
    ISSUE_FACE_OUT_OF_FRAME(11, "Face was often out of frame", MetricMethodology.ESTIMATED),
    ISSUE_LOOKING_AWAY(12, "Often not facing the camera", MetricMethodology.ESTIMATED),
    ISSUE_POSTURE_TILTED(13, "Shoulders were often tilted", MetricMethodology.ESTIMATED)
}

/**
 * An issue raised from metric analysis and rule execution (ARC-05 §8.4).
 */
data class DetectedIssue(
    val code: IssueCode,
    val evidenceText: String,
    val suggestion: String,
    val ruleId: String? = null
) : Comparable<DetectedIssue> {
    override fun compareTo(other: DetectedIssue): Int {
        val priorityComparison = this.code.priority.compareTo(other.code.priority)
        if (priorityComparison != 0) return priorityComparison
        return this.code.name.compareTo(other.code.name)
    }
}
