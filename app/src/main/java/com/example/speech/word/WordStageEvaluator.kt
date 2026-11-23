package com.example.speech.word

import com.example.speech.metrics.MetricEngine
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import java.util.Locale
import kotlin.math.max

/**
 * Deterministic word-stage evaluator implementing ARC-05 §9 (CMP-36).
 * Evaluates stages 2 ("Say it"), 3 ("Use it"), and 4 ("Make your own").
 */
object WordStageEvaluator {

    const val CAVEAT_SAY_IT =
        "Speech recognition may recognise a word even when it isn't pronounced clearly, so 'Say it' checks that you were understood, not your accent."

    private val STOP_WORDS = setOf(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "is", "am", "are", "was", "were", "be", "been", "being", "it", "this", "that"
    )

    /**
     * Stage 2: "Say it" (TPL-WORD-SAY).
     * Score: 100 exact, 90 inflection, 75–89 near-match, < 75 other. Pass >= 75.
     */
    fun evaluateSayIt(
        targetWord: String,
        acceptedInflections: List<String>,
        transcript: String
    ): WordStageResult {
        val tokens = MetricEngine.tokenize(transcript).map { MetricEngine.normalizeToken(it) }
        val normalizedTarget = MetricEngine.normalizeToken(targetWord)
        val normalizedInflections = acceptedInflections.map { MetricEngine.normalizeToken(it) }

        // 1. Exact match (Score 100)
        if (tokens.contains(normalizedTarget)) {
            return WordStageResult(
                stage = WordStage.SAY_IT,
                score = 100,
                threshold = 75,
                passed = true,
                caveat = CAVEAT_SAY_IT
            )
        }

        // 2. Accepted inflection match (Score 90)
        if (tokens.any { it in normalizedInflections }) {
            return WordStageResult(
                stage = WordStage.SAY_IT,
                score = 90,
                threshold = 75,
                passed = true,
                caveat = CAVEAT_SAY_IT
            )
        }

        // 3. Near-match check (Levenshtein similarity on best matching token)
        var bestSimilarity = 0.0
        var bestHeardToken = ""
        for (token in tokens) {
            val sim = tokenSimilarity(token, normalizedTarget)
            if (sim > bestSimilarity) {
                bestSimilarity = sim
                bestHeardToken = token
            }
        }

        return if (bestSimilarity >= 0.8) {
            val nearMatchScore = (75 + ((bestSimilarity - 0.8) / 0.2) * 14).toInt().coerceIn(75, 89)
            WordStageResult(
                stage = WordStage.SAY_IT,
                score = nearMatchScore,
                threshold = 75,
                passed = true,
                caveat = CAVEAT_SAY_IT
            )
        } else if (bestSimilarity >= 0.6) {
            val score = (bestSimilarity * 100).toInt().coerceAtMost(74)
            val issue = DetectedIssue(
                code = IssueCode.ISSUE_WRONG_FORM,
                evidenceText = "We heard '$bestHeardToken'",
                suggestion = "We heard '$bestHeardToken'. Say the target word '$targetWord' clearly."
            )
            WordStageResult(
                stage = WordStage.SAY_IT,
                score = score,
                threshold = 75,
                passed = false,
                unmet = setOf(StageRequirement.TARGET_PRESENT),
                issues = listOf(issue),
                caveat = CAVEAT_SAY_IT
            )
        } else {
            val issue = DetectedIssue(
                code = IssueCode.ISSUE_TARGET_WORD_MISSING,
                evidenceText = "Target word: '$targetWord'",
                suggestion = "Target word '$targetWord' was not detected. Please pronounce the word clearly."
            )
            WordStageResult(
                stage = WordStage.SAY_IT,
                score = 0,
                threshold = 75,
                passed = false,
                unmet = setOf(StageRequirement.TARGET_PRESENT),
                issues = listOf(issue),
                caveat = CAVEAT_SAY_IT
            )
        }
    }

    /**
     * Stage 3: "Use it" (TPL-WORD-USE).
     * M-MATCH on each given reference sentence. Pass: all >= 75.
     */
    fun evaluateUseIt(
        referenceSentences: List<String>,
        transcripts: List<String>
    ): WordStageResult {
        if (referenceSentences.isEmpty()) {
            return WordStageResult(
                stage = WordStage.USE_IT,
                score = 100,
                threshold = 75,
                passed = true
            )
        }

        val scores = mutableListOf<Int>()
        val issues = mutableListOf<DetectedIssue>()

        for (i in referenceSentences.indices) {
            val ref = referenceSentences[i]
            val trans = transcripts.getOrNull(i) ?: ""
            val hypTokens = MetricEngine.tokenize(trans)
            val refTokens = MetricEngine.tokenize(ref)
            val matchScore = MetricEngine.computeReferenceMatch(hypTokens, refTokens).toInt()
            scores.add(matchScore)

            if (matchScore < 75) {
                issues.add(
                    DetectedIssue(
                        code = IssueCode.ISSUE_WORD_NOT_RECOGNISED,
                        evidenceText = "Sentence ${i + 1} match: $matchScore%",
                        suggestion = "Practice repeating sentence ${i + 1} with accurate word pronunciation."
                    )
                )
            }
        }

        val minScore = scores.minOrNull() ?: 0
        val allPassed = scores.all { it >= 75 }

        return WordStageResult(
            stage = WordStage.USE_IT,
            score = minScore,
            threshold = 75,
            passed = allPassed,
            unmet = if (allPassed) emptySet() else setOf(StageRequirement.BOTH_SENTENCES),
            issues = issues
        )
    }

    /**
     * Stage 4: "Make your own" (TPL-WORD-OWN).
     * Requirements:
     * - Target present (required)
     * - >= 5 words (required)
     * - Grammar hits <= 1 (required)
     * - Composite score = 50 (target + length) + 25 * (1 - grammarHits.coerceAtMost(1)) + 25 * (contentWords >= 3 ? 1 : 0)
     * - Pass: >= 70 and all required conditions met.
     */
    fun evaluateMakeYourOwn(
        targetWord: String,
        acceptedInflections: List<String>,
        transcript: String,
        grammarHitCount: Int
    ): WordStageResult {
        val tokens = MetricEngine.tokenize(transcript)
        val normalizedTokens = tokens.map { MetricEngine.normalizeToken(it) }
        val normalizedTarget = MetricEngine.normalizeToken(targetWord)
        val normalizedInflections = acceptedInflections.map { MetricEngine.normalizeToken(it) }

        val targetPresent = normalizedTokens.contains(normalizedTarget) ||
                normalizedTokens.any { it in normalizedInflections }
        val lengthMet = tokens.size >= 5
        val grammarMet = grammarHitCount <= 1

        val contentWordCount = normalizedTokens.count { it !in STOP_WORDS && it.isNotBlank() }
        val contentWordsMet = contentWordCount >= 3

        val unmet = mutableSetOf<StageRequirement>()
        val issues = mutableListOf<DetectedIssue>()

        if (!targetPresent) {
            unmet.add(StageRequirement.TARGET_PRESENT)
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_TARGET_WORD_MISSING,
                    evidenceText = "Missing target: '$targetWord'",
                    suggestion = "Remember to include the word '$targetWord' in your sentence."
                )
            )
        }

        if (!lengthMet) {
            unmet.add(StageRequirement.MIN_WORDS)
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_TOO_SHORT,
                    evidenceText = "${tokens.size} words spoken (minimum 5 required)",
                    suggestion = "Make a fuller sentence with at least 5 words to practice natural context."
                )
            )
        }

        if (!grammarMet) {
            unmet.add(StageRequirement.GRAMMAR_HITS)
            issues.add(
                DetectedIssue(
                    code = IssueCode.ISSUE_GRAMMAR_POSSIBLE,
                    evidenceText = "$grammarHitCount grammar check hits",
                    suggestion = "Review the sentence structure to ensure clear grammatical agreement."
                )
            )
        }

        // Composite calculation
        var composite = 0
        if (targetPresent && lengthMet) {
            composite += 50
        }
        if (grammarMet) {
            composite += (25 * (1 - grammarHitCount.coerceAtMost(1)))
        }
        if (contentWordsMet) {
            composite += 25
        }

        val allRequiredMet = targetPresent && lengthMet && grammarMet
        val passed = allRequiredMet && composite >= 70

        if (!passed && allRequiredMet) {
            unmet.add(StageRequirement.COMPOSITE_SCORE)
        }

        return WordStageResult(
            stage = WordStage.MAKE_YOUR_OWN,
            score = composite,
            threshold = 70,
            passed = passed,
            unmet = unmet,
            issues = issues
        )
    }

    private fun tokenSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0

        val dist = levenshtein(s1, s2)
        return (1.0 - (dist.toDouble() / maxLen.toDouble())).coerceIn(0.0, 1.0)
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        var prev = IntArray(rhs.length + 1) { it }
        var curr = IntArray(rhs.length + 1)

        for (i in lhs.indices) {
            curr[0] = i + 1
            for (j in rhs.indices) {
                val cost = if (lhs[i] == rhs[j]) 0 else 1
                curr[j + 1] = minOf(
                    curr[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[rhs.length]
    }
}
