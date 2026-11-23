package com.example.speech.feedback

import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode

/**
 * Authoritative Strength codes defined in ARC-05 §8.2 and §8.4.
 * Stored as the primary strength in attempt results.
 */
enum class StrengthCode(val title: String) {
    STR_ALL_RECOGNISED("All Words Recognised"),
    STR_TARGET_WORD_USED("Target Word Used"),
    STR_STRUCT_COMPLETE("Complete Structure"),
    STR_PACE_IN_BAND("Natural Speaking Pace"),
    STR_FEW_PAUSES("Smooth Delivery"),
    STR_FEW_FILLERS("Clean Flow"),
    STR_LENGTH_ON_TARGET("Target Length Reached"),
    STR_GOOD_VARIETY("Rich Vocabulary"),
    STR_IMPROVED("Improvement vs Previous Attempt"),
    STR_COMPLETED("Attempt Completed")
}

/**
 * One strength selected per attempt (SRS-FR-EVAL-005).
 */
data class StrengthFeedback(
    val code: StrengthCode,
    val title: String,
    val message: String,
    val metricKey: SpeechMetricKey? = null
)

/**
 * One focus selected per attempt, derived from highest priority issue (SRS-FR-EVAL-005, ARC-05 §8.2).
 */
data class FocusFeedback(
    val priority: Int,
    val issueCode: IssueCode?,
    val title: String,
    val advice: String,
    val evidenceText: String? = null,
    val suggestionText: String? = null
)

/**
 * Technique coach tip selected deterministically (ARC-05 §8.4, PRD-FR-047, SRS-FR-EVAL-016).
 */
data class CoachTip(
    val tipId: String,
    val techniqueId: String?,
    val title: String,
    val text: String,
    val forIssue: IssueCode? = null,
    val isFocusTip: Boolean = false,
    val sourceVideoUrl: String? = null
)

/**
 * Self-check item for vocal-delivery or body-language techniques that audio cannot measure (ARC-05 §8.4).
 */
data class SelfCheckItem(
    val id: String,
    val text: String
)

/**
 * Single-source "How we measured this" explanation (ARC-05 §10).
 */
data class MetricExplainability(
    val key: SpeechMetricKey,
    val label: String,
    val methodology: String,
    val explanation: String
)

/**
 * Complete feedback payload produced by FeedbackComposer (ARC-05 §8.2, §8.4).
 */
data class Feedback(
    val strength: StrengthFeedback,
    val focus: FocusFeedback,
    val issues: List<DetectedIssue>,
    val tips: List<CoachTip>,
    val selfCheckItems: List<SelfCheckItem> = emptyList(),
    val explainability: List<MetricExplainability> = emptyList()
)
