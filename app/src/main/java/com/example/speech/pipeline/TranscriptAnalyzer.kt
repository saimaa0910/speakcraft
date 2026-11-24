package com.example.speech.pipeline

import com.example.speech.rules.IssueCode
import com.example.speech.rules.RuleEngine
import java.util.Locale

/**
 * Deterministic transcript analyzer that produces inline [TranscriptInsight] annotations
 * from the full transcript text (ARC-05 §8.4, v1.5.0).
 *
 * Detection categories:
 * - Grammar rule matches → [InsightSeverity.ERROR], exact span from regex
 * - Filler words/phrases → [InsightSeverity.WARNING], span of each filler
 * - Hedge words/phrases  → [InsightSeverity.WARNING], span of each hedge
 *
 * Pure function, no side effects. Results sorted by span position (start offset).
 */
object TranscriptAnalyzer {

    // Filler detection: same word/phrase lists as MetricEngine for consistency
    private val FILLER_PHRASES = listOf("you know", "i mean")
    private val FILLER_WORDS = setOf("um", "uh", "er", "ah", "erm")

    // Hedge detection: same word/phrase lists as MetricEngine for consistency
    private val HEDGE_PHRASES = listOf("i think maybe", "sort of", "kind of")
    private val HEDGE_WORDS = setOf("maybe", "perhaps", "just")

    // Word-boundary regex for matching standalone filler/hedge words
    private val WORD_BOUNDARY_PATTERN = """(?<!\w)(%s)(?!\w)"""

    /**
     * Analyses [transcript] and returns [TranscriptInsight] entries sorted by span position.
     * Pure function: deterministic, no side effects.
     */
    fun analyze(transcript: String): List<TranscriptInsight> {
        if (transcript.isBlank()) return emptyList()

        val insights = mutableListOf<TranscriptInsight>()

        // 1. Grammar rule matches → ERROR severity
        insights.addAll(findGrammarInsights(transcript))

        // 2. Filler words/phrases → WARNING severity
        insights.addAll(findFillerInsights(transcript))

        // 3. Hedge words/phrases → WARNING severity
        insights.addAll(findHedgeInsights(transcript))

        // Deduplicate overlapping spans (prioritizing severity, then length)
        return deduplicateOverlaps(insights)
    }

    /**
     * Deduplicates overlapping insight spans.
     * When spans overlap, higher severity (ERROR > WARNING > TIP) takes precedence.
     * For identical severities, the longer span (e.g., multi-word phrase over single word) is preferred.
     */
    private fun deduplicateOverlaps(insights: List<TranscriptInsight>): List<TranscriptInsight> {
        if (insights.size <= 1) return insights

        val sorted = insights.sortedWith(
            compareBy<TranscriptInsight> { it.severity.ordinal }
                .thenByDescending { it.spanEnd - it.spanStart }
                .thenBy { it.spanStart }
        )

        val kept = mutableListOf<TranscriptInsight>()
        for (insight in sorted) {
            val overlaps = kept.any { existing ->
                maxOf(existing.spanStart, insight.spanStart) < minOf(existing.spanEnd, insight.spanEnd)
            }
            if (!overlaps) {
                kept.add(insight)
            }
        }

        return kept.sortedWith(compareBy({ it.spanStart }, { it.spanEnd }))
    }

    /**
     * Composes a one-line summary of transcript insights for feedback display.
     * Returns a human-readable string like "2 possible grammar issues and 3 filler words detected"
     * or "No issues detected in your speech" when the list is empty.
     */
    fun composeSummary(insights: List<TranscriptInsight>): String {
        if (insights.isEmpty()) return "No issues detected in your speech"

        val parts = mutableListOf<String>()

        val grammarCount = insights.count { it.severity == InsightSeverity.ERROR }
        val fillerCount = insights.count {
            it.severity == InsightSeverity.WARNING && it.issueCode == IssueCode.ISSUE_FILLERS_HIGH
        }
        val hedgeCount = insights.count {
            it.severity == InsightSeverity.WARNING && it.issueCode == IssueCode.ISSUE_HEDGING_HIGH
        }

        if (grammarCount > 0) {
            parts.add("$grammarCount possible grammar ${if (grammarCount == 1) "issue" else "issues"}")
        }
        if (fillerCount > 0) {
            parts.add("$fillerCount filler ${if (fillerCount == 1) "word" else "words"}")
        }
        if (hedgeCount > 0) {
            parts.add("$hedgeCount hedging ${if (hedgeCount == 1) "phrase" else "phrases"}")
        }

        return if (parts.isEmpty()) {
            "No issues detected in your speech"
        } else {
            parts.joinToString(" and ") + " detected"
        }
    }

    private fun findGrammarInsights(transcript: String): List<TranscriptInsight> {
        val insights = mutableListOf<TranscriptInsight>()
        val rules = RuleEngine.activeRules.ifEmpty { RuleEngine.CANDIDATE_RULES }

        for (rule in rules) {
            val matches = rule.regex.findAll(transcript)
            for (m in matches) {
                val suggestion = rule.replacementTransform(m)
                insights.add(
                    TranscriptInsight(
                        issueCode = IssueCode.ISSUE_GRAMMAR_POSSIBLE,
                        spanStart = m.range.first,
                        spanEnd = m.range.last + 1, // exclusive end
                        highlightedText = m.value,
                        issueTitle = IssueCode.ISSUE_GRAMMAR_POSSIBLE.title,
                        suggestion = "Possible issue: '${m.value}' → '$suggestion'. ${rule.explanation}",
                        severity = InsightSeverity.ERROR
                    )
                )
            }
        }

        return insights
    }

    private fun findFillerInsights(transcript: String): List<TranscriptInsight> {
        val insights = mutableListOf<TranscriptInsight>()
        val lowerTranscript = transcript.lowercase(Locale.ROOT)

        // Multi-word filler phrases
        for (phrase in FILLER_PHRASES) {
            val regex = Regex(WORD_BOUNDARY_PATTERN.format(Regex.escape(phrase)), RegexOption.IGNORE_CASE)
            for (m in regex.findAll(lowerTranscript)) {
                insights.add(
                    TranscriptInsight(
                        issueCode = IssueCode.ISSUE_FILLERS_HIGH,
                        spanStart = m.range.first,
                        spanEnd = m.range.last + 1,
                        highlightedText = transcript.substring(m.range.first, m.range.last + 1),
                        issueTitle = IssueCode.ISSUE_FILLERS_HIGH.title,
                        suggestion = "Consider removing the filler '${m.value}' for a cleaner delivery.",
                        severity = InsightSeverity.WARNING
                    )
                )
            }
        }

        // Single filler words
        for (word in FILLER_WORDS) {
            val regex = Regex(WORD_BOUNDARY_PATTERN.format(Regex.escape(word)), RegexOption.IGNORE_CASE)
            for (m in regex.findAll(lowerTranscript)) {
                insights.add(
                    TranscriptInsight(
                        issueCode = IssueCode.ISSUE_FILLERS_HIGH,
                        spanStart = m.range.first,
                        spanEnd = m.range.last + 1,
                        highlightedText = transcript.substring(m.range.first, m.range.last + 1),
                        issueTitle = IssueCode.ISSUE_FILLERS_HIGH.title,
                        suggestion = "Consider replacing the filler '${m.value}' with a silent pause.",
                        severity = InsightSeverity.WARNING
                    )
                )
            }
        }

        return insights
    }

    private fun findHedgeInsights(transcript: String): List<TranscriptInsight> {
        val insights = mutableListOf<TranscriptInsight>()
        val lowerTranscript = transcript.lowercase(Locale.ROOT)

        // Multi-word hedge phrases
        for (phrase in HEDGE_PHRASES) {
            val regex = Regex(WORD_BOUNDARY_PATTERN.format(Regex.escape(phrase)), RegexOption.IGNORE_CASE)
            for (m in regex.findAll(lowerTranscript)) {
                insights.add(
                    TranscriptInsight(
                        issueCode = IssueCode.ISSUE_HEDGING_HIGH,
                        spanStart = m.range.first,
                        spanEnd = m.range.last + 1,
                        highlightedText = transcript.substring(m.range.first, m.range.last + 1),
                        issueTitle = IssueCode.ISSUE_HEDGING_HIGH.title,
                        suggestion = "Consider stating your point directly instead of hedging with '${m.value}'.",
                        severity = InsightSeverity.WARNING
                    )
                )
            }
        }

        // Single hedge words
        for (word in HEDGE_WORDS) {
            val regex = Regex(WORD_BOUNDARY_PATTERN.format(Regex.escape(word)), RegexOption.IGNORE_CASE)
            for (m in regex.findAll(lowerTranscript)) {
                insights.add(
                    TranscriptInsight(
                        issueCode = IssueCode.ISSUE_HEDGING_HIGH,
                        spanStart = m.range.first,
                        spanEnd = m.range.last + 1,
                        highlightedText = transcript.substring(m.range.first, m.range.last + 1),
                        issueTitle = IssueCode.ISSUE_HEDGING_HIGH.title,
                        suggestion = "Consider removing the hedge word '${m.value}' for a more confident delivery.",
                        severity = InsightSeverity.WARNING
                    )
                )
            }
        }

        return insights
    }
}
