package com.example.speech.rules

import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.metrics.TargetBands
import java.util.Locale

/**
 * High-precision grammar rule model (ARC-05 §8.1, NFR-QUAL-003).
 */
data class GrammarRule(
    val id: String,
    val name: String,
    val regex: Regex,
    val explanation: String,
    val replacementTransform: (MatchResult) -> String
)

/**
 * Deterministic grammar and issue detection engine implementing ARC-05 §8.1 and §8.4.
 */
object RuleEngine {

    /**
     * Candidate rules (ARC-05 §8.1 "expected initial survivors"). They are NOT yet qualified: a rule
     * may only produce user-facing observations after SPK-02 has measured its precision ≥ 0.90 on a
     * labelled corpus (SRS-FR-EVAL-006, NFR-QUAL-003). See [RuleQualificationRegistry] and
     * [activeRules]; tests exercise the candidates directly.
     */
    val CANDIDATE_RULES: List<GrammarRule> = listOf(
        GrammarRule(
            id = "R-AGREE-01",
            name = "Third-person singular agreement",
            regex = Regex("""\b(he|she|it)\s+don'?t\b""", RegexOption.IGNORE_CASE),
            explanation = "Use 'doesn't' with third-person singular subjects (he, she, it)."
        ) { match -> "${match.groupValues[1]} doesn't" },

        GrammarRule(
            id = "R-AGREE-02",
            name = "Third-person singular have/has",
            // Excludes contexts where "he/she/it have" is correct: auxiliaries and modals ("does she
            // have", "would he have known"), infinitives ("to have"), causatives ("make it have",
            // "let it have") and pronoun-coordinated subjects ("he and she have", "you or he have").
            // A noun-coordinated subject ("the parents and she have") still fires — measured in SPK-02.
            regex = Regex(
                """(?<!\b(?:do|does|did|would|could|should|will|can|might|must|may|shall|to|make|makes|made|let|lets|help|helps|helped)\s)""" +
                    """(?<!\b(?:i|you|he|she|it|we|they|him|her|me|us|them)\s(?:and|or|nor)\s)\b(he|she|it)\s+have\b""",
                RegexOption.IGNORE_CASE
            ),
            explanation = "Use 'has' instead of 'have' with he, she, or it."
        ) { match -> "${match.groupValues[1]} has" },

        GrammarRule(
            id = "R-COMP-01",
            name = "Double comparative",
            regex = Regex("""\bmore\s+(better|faster|easier|harder|smarter|stronger)\b""", RegexOption.IGNORE_CASE),
            explanation = "Comparative adjectives like 'better' or 'faster' already imply 'more'."
        ) { match -> match.groupValues[1] },

        GrammarRule(
            id = "R-PAST-01",
            name = "Past auxiliary + base verb",
            regex = Regex("""\b(didn'?t|did\s+not)\s+(went|saw|ate|came|did|ran)\b""", RegexOption.IGNORE_CASE),
            explanation = "Use the base form of the verb after 'did not' or 'didn't'."
        ) { match ->
            val aux = match.groupValues[1]
            val base = when (match.groupValues[2].lowercase(Locale.ROOT)) {
                "went" -> "go"
                "saw" -> "see"
                "ate" -> "eat"
                "came" -> "come"
                "did" -> "do"
                "ran" -> "run"
                else -> match.groupValues[2]
            }
            "$aux $base"
        },

        GrammarRule(
            id = "R-PART-01",
            name = "Wrong participle after have",
            regex = Regex("""\b(have|has|had)\s+went\b""", RegexOption.IGNORE_CASE),
            explanation = "Use the past participle 'gone' with have, has, or had."
        ) { match -> "${match.groupValues[1]} gone" },

        GrammarRule(
            id = "R-ARTICLE-01",
            name = "Indefinite article before vowel sound",
            regex = Regex("""\ba\s+(apple|hour|idea|opportunity|example|option|answer|email)\b""", RegexOption.IGNORE_CASE),
            explanation = "Use 'an' before words that start with a vowel sound."
        ) { match -> "an ${match.groupValues[1]}" }
    )

    /**
     * Rules allowed to produce user-facing observations: candidates with a recorded SPK-02
     * qualification. Empty until SPK-02 results are recorded, so no grammar observation is shown and
     * M-GRAM is omitted rather than reported as "clean".
     */
    val activeRules: List<GrammarRule>
        get() = CANDIDATE_RULES.filter { RuleQualificationRegistry.isQualified(it.id) }

    /**
     * Executes grammar rules across the raw transcript. Defaults to [activeRules]; pass
     * [CANDIDATE_RULES] to test rule logic or to run the SPK-02 harness.
     */
    fun evaluateGrammar(transcript: String, rules: List<GrammarRule> = activeRules): List<DetectedIssue> {
        if (transcript.isBlank()) return emptyList()

        val issues = mutableListOf<DetectedIssue>()

        for (rule in rules) {
            val matches = rule.regex.findAll(transcript)
            for (m in matches) {
                val span = m.value
                val suggestion = rule.replacementTransform(m)
                issues.add(
                    DetectedIssue(
                        code = IssueCode.ISSUE_GRAMMAR_POSSIBLE,
                        evidenceText = span,
                        suggestion = "Possible issue: '$span' → '$suggestion'. ${rule.explanation}",
                        ruleId = rule.id
                    )
                )
            }
        }

        return issues
    }

    /**
     * Synthesizes all detected issues from computed metrics and grammar rule hits
     * ordered by authoritative priority (ARC-05 §8.4).
     */
    fun detectIssues(
        metrics: Map<SpeechMetricKey, SpeechMetricValue>,
        transcript: String,
        templateId: String,
        targetWord: String? = null
    ): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()

        // 1. ISSUE_TOO_QUIET (Priority 1)
        val levelMetric = metrics[SpeechMetricKey.M_LEVEL]
        if (levelMetric?.isIssue == true) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_TOO_QUIET,
                    evidenceText = "Loudness variability: ${levelMetric.displayValue}",
                    suggestion = "Audio was quiet or uneven in places. Speak at a steady volume towards the microphone."
                )
            )
        }

        // 2. Word accuracy issues (Priority 2)
        val matchMetric = metrics[SpeechMetricKey.M_MATCH]
        if (matchMetric?.isIssue == true) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_WORD_NOT_RECOGNISED,
                    evidenceText = "Alignment: ${matchMetric.displayValue}",
                    suggestion = "Some words did not match the reference text. Listen to the prompt and try repeating again."
                )
            )
        }

        val targetMetric = metrics[SpeechMetricKey.M_TARGET]
        if (targetMetric?.isIssue == true && !targetWord.isNullOrBlank()) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_TARGET_WORD_MISSING,
                    evidenceText = "Missing target word: '$targetWord'",
                    suggestion = "Remember to include the target word '$targetWord' in your sentence."
                )
            )
        }

        // 3. Framework element missing (Priority 3)
        val structMetric = metrics[SpeechMetricKey.M_STRUCT]
        if (structMetric?.isIssue == true) {
            val detected = structMetric.detectedElements
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_STRUCT_ELEMENT_MISSING,
                    evidenceText = "Detected: ${detected.joinToString(", ")}",
                    suggestion = "Structure incomplete. Try to explicitly state each stage of the framework."
                )
            )
        }

        // 4. Fillers high (Priority 4)
        val fillMetric = metrics[SpeechMetricKey.M_FILL]
        if (fillMetric?.isIssue == true) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_FILLERS_HIGH,
                    evidenceText = fillMetric.displayValue,
                    suggestion = "High filler word rate. Replace vocal fillers with silent pauses to gather your thoughts."
                )
            )
        }

        // 5. Pace fast or slow (Priority 5)
        val wpmMetric = metrics[SpeechMetricKey.M_WPM]
        if (wpmMetric?.isIssue == true) {
            // The band word follows the band actually applied, including technique REBAND overrides (ARC-03 §6.8).
            val isFast = wpmMetric.band == "Fast"
            val code = if (isFast) IssueCode.ISSUE_PACE_FAST else IssueCode.ISSUE_PACE_SLOW
            val target = wpmMetric.targetRangeText()
            val suggestion = if (isFast) {
                "Pace was fast (${wpmMetric.displayValue}). Slow down slightly to aim for $target words per minute."
            } else {
                "Pace was slow (${wpmMetric.displayValue}). Aim for a continuous conversational flow of $target words per minute."
            }
            issues.add(
                DetectedIssue(
                    code = code,
                    evidenceText = wpmMetric.displayValue,
                    suggestion = suggestion
                )
            )
        }

        // 6. Long pauses (Priority 6)
        val pauseMetric = metrics[SpeechMetricKey.M_PAUSE]
        if (pauseMetric?.isIssue == true) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_PAUSES_LONG,
                    evidenceText = "${pauseMetric.rawValue?.toInt() ?: 0} long pauses",
                    suggestion = "Extended pauses detected. Plan your core message points before speaking."
                )
            )
        }

        // 7. Length too short (Priority 7)
        val lenMetric = metrics[SpeechMetricKey.M_LEN]
        if (lenMetric?.isIssue == true) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_TOO_SHORT,
                    evidenceText = lenMetric.displayValue,
                    suggestion = "Say a little more: elaborate with an example or reason to hit the target length."
                )
            )
        }

        // 8. Grammar check observations (Priority 8)
        val grammarIssues = evaluateGrammar(transcript)
        issues.addAll(grammarIssues.take(2)) // Max 2 grammar issues shown (SRS-FR-EVAL-006)

        // 9. Hedging high (Priority 9)
        val hedgeMetric = metrics[SpeechMetricKey.M_HEDGE]
        if (hedgeMetric?.isIssue == true) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_HEDGING_HIGH,
                    evidenceText = hedgeMetric.displayValue,
                    suggestion = "Frequent hedging detected. State your perspective directly without weakening phrases."
                )
            )
        }

        // 10. Low variety (Priority 10)
        val lexMetric = metrics[SpeechMetricKey.M_LEX]
        if (lexMetric?.isIssue == true) {
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_LOW_VARIETY,
                    evidenceText = "MATTR: ${lexMetric.displayValue}",
                    suggestion = "Repetitive vocabulary. Try incorporating diverse descriptive words."
                )
            )
        }

        // Sort strictly by authoritative priority table (§8.4)
        return issues.sorted()
    }

    /** "100–160" from the pace band actually applied (defaults when the metric carries none). */
    private fun SpeechMetricValue.targetRangeText(): String = String.format(
        Locale.ROOT, "%.0f–%.0f",
        targetMin ?: TargetBands.DEFAULT.wpmMin,
        targetMax ?: TargetBands.DEFAULT.wpmMax
    )
}
