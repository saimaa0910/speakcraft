package com.example.speech.metrics

/**
 * Metric measurement methodology label defined in ARC-05 §6.
 */
enum class MetricMethodology {
    MEASURED,
    ESTIMATED
}

/**
 * Authoritative 14 speech metric keys defined in ARC-05 §6 (ENT-04).
 */
enum class SpeechMetricKey(
    val code: String,
    val displayName: String,
    val methodology: MetricMethodology,
    val unit: String
) {
    M_DUR("M-DUR", "Duration", MetricMethodology.MEASURED, "ms"),
    M_SPK("M-SPK", "Speaking Time", MetricMethodology.MEASURED, "ms"),
    M_RATIO("M-RATIO", "Speech Ratio", MetricMethodology.MEASURED, "ratio"),
    M_WPM("M-WPM", "Pace", MetricMethodology.MEASURED, "wpm"),
    M_PAUSE("M-PAUSE", "Long Pauses", MetricMethodology.MEASURED, "count"),
    M_WORDS("M-WORDS", "Words", MetricMethodology.MEASURED, "count"),
    M_LEN("M-LEN", "Length vs Target", MetricMethodology.MEASURED, "words"),
    M_TARGET("M-TARGET", "Target Word Used", MetricMethodology.MEASURED, "bool"),
    M_MATCH("M-MATCH", "Recognised as Intended", MetricMethodology.MEASURED, "%"),
    M_FILL("M-FILL", "Filler Words", MetricMethodology.ESTIMATED, "per_min"),
    M_LEX("M-LEX", "Vocabulary Variety", MetricMethodology.ESTIMATED, "mattr"),
    M_GRAM("M-GRAM", "Possible Grammar Issues", MetricMethodology.ESTIMATED, "per_100w"),
    M_STRUCT("M-STRUCT", "Structure Elements", MetricMethodology.ESTIMATED, "elements"),
    M_HEDGE("M-HEDGE", "Hedging", MetricMethodology.ESTIMATED, "per_100w"),
    M_LEVEL("M-LEVEL", "Loudness Consistency", MetricMethodology.MEASURED, "cv")
}

/**
 * Computed metric value with raw storage value and qualitative band.
 */
data class SpeechMetricValue(
    val key: SpeechMetricKey,
    val rawValue: Double?,
    val displayValue: String,
    val band: String, // e.g. "Target", "Slow", "Fast", "High", "Low", "Pass", "Fail"
    val isIssue: Boolean,
    val detectedElements: List<String> = emptyList() // For M-STRUCT or cue phrase lists
)
