package com.example.speech.metrics

import java.util.Locale
import kotlin.math.sqrt

/**
 * Deterministic metric calculation engine implementing ARC-05 §6 and §7.
 */
object MetricEngine {

    private val FILLER_PHRASES = listOf("you know", "i mean")
    private val FILLER_WORDS = setOf("um", "uh", "er", "ah", "erm")
    private val HEDGE_PHRASES = listOf("i think maybe", "sort of", "kind of")
    private val HEDGE_WORDS = setOf("maybe", "perhaps", "just")

    // STAR framework cue phrases
    private val STAR_CUES = mapOf(
        "Situation" to listOf("when i was", "at my previous", "in my role as", "the situation was", "we were facing"),
        "Task" to listOf("my task was", "i was responsible for", "i needed to", "my goal was", "the objective was"),
        "Action" to listOf("i decided to", "i started by", "i implemented", "i spoke with", "i created", "i took the lead"),
        "Result" to listOf("as a result", "in the end", "the outcome was", "we achieved", "because of this", "ultimately")
    )

    // PREP framework cue phrases
    private val PREP_CUES = mapOf(
        "Point" to listOf("my main point is", "i believe that", "first of all", "to begin with"),
        "Reason" to listOf("the reason is", "because", "due to the fact", "this is important because"),
        "Example" to listOf("for example", "for instance", "to illustrate", "such as"),
        "Point_Restate" to listOf("in conclusion", "that is why", "to summarize", "therefore")
    )

    /**
     * Computes the complete catalogue of metrics for an attempt.
     */
    fun computeMetrics(
        durationMs: Long,
        voicedDurationMs: Long,
        transcript: String,
        pauseCount: Int,
        activeFrameRmsList: List<Double> = emptyList(),
        referenceText: String? = null,
        targetWord: String? = null,
        targetWordInflections: List<String> = emptyList(),
        targetMinWords: Int? = null,
        targetMaxWords: Int? = null,
        framework: String? = null, // "STAR" or "PREP"
        grammarHitCount: Int = 0,
        hasTranscript: Boolean = true,
        grammarRulesAvailable: Boolean = true
    ): Map<SpeechMetricKey, SpeechMetricValue> {
        val results = mutableMapOf<SpeechMetricKey, SpeechMetricValue>()
        val words = tokenize(transcript)
        val wordCount = words.size
        val speakingMinutes = (voicedDurationMs.toDouble() / 60000.0).coerceAtLeast(0.01)

        // 1. M-DUR (Duration in ms)
        results[SpeechMetricKey.M_DUR] = SpeechMetricValue(
            key = SpeechMetricKey.M_DUR,
            rawValue = durationMs.toDouble(),
            displayValue = String.format(Locale.ROOT, "%.1f s", durationMs / 1000.0),
            band = "Measured",
            isIssue = false
        )

        // 2. M-SPK (Speaking time in ms)
        results[SpeechMetricKey.M_SPK] = SpeechMetricValue(
            key = SpeechMetricKey.M_SPK,
            rawValue = voicedDurationMs.toDouble(),
            displayValue = String.format(Locale.ROOT, "%.1f s", voicedDurationMs / 1000.0),
            band = "Measured",
            isIssue = false
        )

        // 3. M-RATIO (Speech ratio 0.0 - 1.0)
        val ratio = if (durationMs > 0) (voicedDurationMs.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0) else 0.0
        results[SpeechMetricKey.M_RATIO] = SpeechMetricValue(
            key = SpeechMetricKey.M_RATIO,
            rawValue = ratio,
            displayValue = String.format(Locale.ROOT, "%.0f %%", ratio * 100.0),
            band = if (ratio >= 0.5) "Active" else "Sparse",
            isIssue = false
        )

        // 4. M-WORDS (Word count)
        results[SpeechMetricKey.M_WORDS] = SpeechMetricValue(
            key = SpeechMetricKey.M_WORDS,
            rawValue = wordCount.toDouble(),
            displayValue = "$wordCount words",
            band = "Measured",
            isIssue = false
        )

        // 5. M-WPM (Pace)
        val wpm = if (transcript.isNotBlank()) wordCount.toDouble() / speakingMinutes else 0.0
        val (wpmBand, wpmIssue) = when {
            transcript.isBlank() -> "No speech" to false
            wpm < 90.0 -> "Slow" to true // < 100 with 10 wpm noise band
            wpm > 170.0 -> "Fast" to true // > 160 with 10 wpm noise band
            else -> "Comfortable" to false
        }
        results[SpeechMetricKey.M_WPM] = SpeechMetricValue(
            key = SpeechMetricKey.M_WPM,
            rawValue = wpm,
            displayValue = String.format(Locale.ROOT, "%.0f wpm", wpm),
            band = wpmBand,
            isIssue = wpmIssue
        )

        // 6. M-PAUSE (Long pauses >= 1.0 s)
        val pausesPerMin = pauseCount.toDouble() / speakingMinutes
        val pauseIssue = pausesPerMin > 3.0 // > 2.0 with ±1 noise band
        results[SpeechMetricKey.M_PAUSE] = SpeechMetricValue(
            key = SpeechMetricKey.M_PAUSE,
            rawValue = pauseCount.toDouble(),
            displayValue = "$pauseCount",
            band = if (pauseIssue) "High pauses" else "Good flow",
            isIssue = pauseIssue
        )

        // 7. M-LEN (Length vs target)
        if (targetMinWords != null) {
            val isTooShort = wordCount < (targetMinWords - 5).coerceAtLeast(0)
            results[SpeechMetricKey.M_LEN] = SpeechMetricValue(
                key = SpeechMetricKey.M_LEN,
                rawValue = wordCount.toDouble(),
                displayValue = "$wordCount / $targetMinWords target",
                band = if (isTooShort) "Below target" else "On target",
                isIssue = isTooShort
            )
        }

        // 8. M-TARGET (Target word used)
        if (!targetWord.isNullOrBlank()) {
            val allAccepted = (listOf(targetWord) + targetWordInflections).map { normalizeToken(it) }
            val used = words.any { normalizeToken(it) in allAccepted }
            results[SpeechMetricKey.M_TARGET] = SpeechMetricValue(
                key = SpeechMetricKey.M_TARGET,
                rawValue = if (used) 1.0 else 0.0,
                displayValue = if (used) "Used" else "Missing",
                band = if (used) "Pass" else "Missing",
                isIssue = !used
            )
        }

        // 9. M-MATCH (Reference text alignment)
        if (!referenceText.isNullOrBlank()) {
            val matchPct = computeReferenceMatch(words, tokenize(referenceText))
            val isPass = matchPct >= 75.0
            results[SpeechMetricKey.M_MATCH] = SpeechMetricValue(
                key = SpeechMetricKey.M_MATCH,
                rawValue = matchPct,
                displayValue = String.format(Locale.ROOT, "%.0f %%", matchPct),
                band = if (isPass) "Pass" else "Review needed",
                isIssue = !isPass
            )
        }

        // 10. M-FILL (Fillers per minute)
        val fillerCount = countFillers(transcript, words)
        val fillerRate = fillerCount.toDouble() / speakingMinutes
        val fillerIssue = fillerRate > 4.0 // > 3 with ±1 noise band
        results[SpeechMetricKey.M_FILL] = SpeechMetricValue(
            key = SpeechMetricKey.M_FILL,
            rawValue = fillerRate,
            displayValue = String.format(Locale.ROOT, "%.1f / min", fillerRate),
            band = if (fillerIssue) "Frequent" else "Low",
            isIssue = fillerIssue
        )

        // 11. M-LEX (MATTR vocabulary variety)
        val mattr = computeMattr(words, windowSize = 25)
        if (mattr != null) {
            val lexIssue = mattr < 0.50 // < 0.55 with ±0.05 noise band
            results[SpeechMetricKey.M_LEX] = SpeechMetricValue(
                key = SpeechMetricKey.M_LEX,
                rawValue = mattr,
                displayValue = String.format(Locale.ROOT, "%.2f", mattr),
                band = if (lexIssue) "Repetitive" else "Varied",
                isIssue = lexIssue
            )
        }

        // 12. M-GRAM (Grammar hits per 100 words)
        val gramHitsPer100 = if (wordCount > 0) (grammarHitCount.toDouble() / wordCount.toDouble()) * 100.0 else 0.0
        results[SpeechMetricKey.M_GRAM] = SpeechMetricValue(
            key = SpeechMetricKey.M_GRAM,
            rawValue = gramHitsPer100,
            displayValue = "$grammarHitCount issues",
            band = if (grammarHitCount > 0) "Issues detected" else "Clean",
            isIssue = grammarHitCount > 0
        )

        // 13. M-STRUCT (Framework cue phrases)
        if (!framework.isNullOrBlank()) {
            val detected = detectStructure(transcript, framework)
            val expectedCount = if (framework.equals("STAR", ignoreCase = true)) 4 else 4
            val structIssue = detected.size < expectedCount
            results[SpeechMetricKey.M_STRUCT] = SpeechMetricValue(
                key = SpeechMetricKey.M_STRUCT,
                rawValue = detected.size.toDouble(),
                displayValue = "${detected.size}/$expectedCount elements",
                band = if (structIssue) "Incomplete" else "Complete",
                isIssue = structIssue,
                detectedElements = detected
            )
        }

        // 14. M-HEDGE (Hedging per 100 words)
        val hedgeCount = countHedges(transcript, words)
        val hedgeRate = if (wordCount > 0) (hedgeCount.toDouble() / wordCount.toDouble()) * 100.0 else 0.0
        val hedgeIssue = hedgeRate > 4.0 // > 3 with ±1 noise band
        results[SpeechMetricKey.M_HEDGE] = SpeechMetricValue(
            key = SpeechMetricKey.M_HEDGE,
            rawValue = hedgeRate,
            displayValue = String.format(Locale.ROOT, "%.1f / 100w", hedgeRate),
            band = if (hedgeIssue) "Frequent" else "Confident",
            isIssue = hedgeIssue
        )

        // 15. M-LEVEL (Loudness CV)
        if (activeFrameRmsList.isNotEmpty()) {
            val mean = activeFrameRmsList.average()
            val variance = activeFrameRmsList.map { (it - mean) * (it - mean) }.average()
            val stdDev = sqrt(variance)
            val cv = if (mean > 0.0) (stdDev / mean).coerceIn(0.0, 1.0) else 0.0
            results[SpeechMetricKey.M_LEVEL] = SpeechMetricValue(
                key = SpeechMetricKey.M_LEVEL,
                rawValue = cv,
                displayValue = String.format(Locale.ROOT, "%.2f", cv),
                band = if (cv > 0.6) "Uneven" else "Consistent",
                isIssue = cv > 0.7
            )
        }

        // SRS-BR-002: a metric without its required inputs is null, never a default.
        // Without a transcript (tier T3b audio-only), transcript-based metrics are not computable —
        // reporting "0 fillers" or "0 grammar issues" would be a false claim of quality.
        if (!hasTranscript) results.keys.removeAll(TRANSCRIPT_METRICS)
        // M-GRAM may only come from rules that passed SPK-02 qualification (SRS-FR-EVAL-006).
        if (!grammarRulesAvailable) results.remove(SpeechMetricKey.M_GRAM)

        return results
    }

    /**
     * Whether [key] applies to [templateId] (ARC-05 §6 "Templates" column, reconciled with the §7
     * headline table: READ shows M-PAUSE; audio-only attempts show M-RATIO and M-PAUSE). A metric that
     * does not apply is neither shown nor praised nor reported as a mistake — e.g. no pace for a
     * one-word "Say it" stage, no filler rate for read-aloud. Unknown templates keep every metric.
     */
    fun isApplicable(key: SpeechMetricKey, templateId: String, hasTranscript: Boolean = true): Boolean {
        val t = templateId.uppercase(Locale.ROOT).removePrefix("TPL-")
        val free = t == "ANSWER" || t == "STRUCT" || t == "STORY"
        val known = free || t in setOf("READ", "REPEAT", "ROLE", "WORD-SAY", "WORD-USE", "WORD-OWN", "WORD-RECALL")
        if (!known) return true
        return when (key) {
            SpeechMetricKey.M_DUR, SpeechMetricKey.M_SPK, SpeechMetricKey.M_LEVEL -> true
            SpeechMetricKey.M_RATIO -> free || !hasTranscript
            SpeechMetricKey.M_PAUSE -> free || t == "READ" || !hasTranscript
            SpeechMetricKey.M_WPM -> free || t == "READ"
            SpeechMetricKey.M_WORDS -> true
            SpeechMetricKey.M_LEN -> free || t == "WORD-OWN"
            SpeechMetricKey.M_TARGET -> t == "WORD-SAY" || t == "WORD-USE" || t == "WORD-OWN" || t == "WORD-RECALL"
            SpeechMetricKey.M_MATCH -> t == "READ" || t == "REPEAT" || t == "WORD-USE"
            SpeechMetricKey.M_FILL, SpeechMetricKey.M_LEX -> free
            SpeechMetricKey.M_GRAM -> free || t == "WORD-OWN"
            SpeechMetricKey.M_STRUCT -> t == "STRUCT"
            SpeechMetricKey.M_HEDGE -> t == "ANSWER" || t == "STRUCT"
        }
    }

    /** Metrics that require a transcript; omitted when none exists (ARC-05 §6 "Inputs" column). */
    val TRANSCRIPT_METRICS: Set<SpeechMetricKey> = setOf(
        SpeechMetricKey.M_WORDS, SpeechMetricKey.M_WPM, SpeechMetricKey.M_LEN, SpeechMetricKey.M_TARGET,
        SpeechMetricKey.M_MATCH, SpeechMetricKey.M_FILL, SpeechMetricKey.M_LEX, SpeechMetricKey.M_GRAM,
        SpeechMetricKey.M_STRUCT, SpeechMetricKey.M_HEDGE
    )

    /**
     * Computes Moving-Average Type-Token Ratio (MATTR) over a sliding token window (ARC-05 §6).
     */
    fun computeMattr(tokens: List<String>, windowSize: Int = 25): Double? {
        if (tokens.size < windowSize) return null

        var sumTtr = 0.0
        val totalWindows = tokens.size - windowSize + 1

        for (i in 0 until totalWindows) {
            val window = tokens.subList(i, i + windowSize)
            val uniqueCount = window.map { normalizeToken(it) }.distinct().size
            sumTtr += (uniqueCount.toDouble() / windowSize.toDouble())
        }

        return sumTtr / totalWindows.toDouble()
    }

    /**
     * Word-level alignment percentage between recognized tokens and reference tokens.
     */
    fun computeReferenceMatch(hypothesisTokens: List<String>, referenceTokens: List<String>): Double {
        if (referenceTokens.isEmpty()) return 100.0
        if (hypothesisTokens.isEmpty()) return 0.0

        val hyp = hypothesisTokens.map { normalizeToken(it) }
        val ref = referenceTokens.map { normalizeToken(it) }

        val d = Array(ref.size + 1) { IntArray(hyp.size + 1) }
        for (i in 0..ref.size) d[i][0] = i
        for (j in 0..hyp.size) d[0][j] = j

        for (i in 1..ref.size) {
            for (j in 1..hyp.size) {
                val cost = if (ref[i - 1] == hyp[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,
                    d[i][j - 1] + 1,
                    d[i - 1][j - 1] + cost
                )
            }
        }

        val distance = d[ref.size][hyp.size]
        val matchRatio = 1.0 - (distance.toDouble() / ref.size.toDouble())
        return (matchRatio.coerceIn(0.0, 1.0) * 100.0)
    }

    private fun countFillers(rawText: String, tokens: List<String>): Int {
        var count = 0
        val lowerText = rawText.lowercase(Locale.ROOT)

        for (phrase in FILLER_PHRASES) {
            var index = 0
            while (true) {
                index = lowerText.indexOf(phrase, index)
                if (index != -1) {
                    count++
                    index += phrase.length
                } else break
            }
        }

        for (token in tokens) {
            if (normalizeToken(token) in FILLER_WORDS) {
                count++
            }
        }
        return count
    }

    private fun countHedges(rawText: String, tokens: List<String>): Int {
        var count = 0
        val lowerText = rawText.lowercase(Locale.ROOT)

        for (phrase in HEDGE_PHRASES) {
            var index = 0
            while (true) {
                index = lowerText.indexOf(phrase, index)
                if (index != -1) {
                    count++
                    index += phrase.length
                } else break
            }
        }

        for (token in tokens) {
            if (normalizeToken(token) in HEDGE_WORDS) {
                count++
            }
        }
        return count
    }

    private fun detectStructure(rawText: String, framework: String): List<String> {
        val detected = mutableListOf<String>()
        val lower = rawText.lowercase(Locale.ROOT)
        val cueMap = if (framework.equals("STAR", ignoreCase = true)) STAR_CUES else PREP_CUES

        for ((element, cues) in cueMap) {
            if (cues.any { lower.contains(it) }) {
                detected.add(element)
            }
        }
        return detected
    }

    fun tokenize(text: String): List<String> {
        return text.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
    }

    fun normalizeToken(token: String): String {
        return token.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
    }
}
