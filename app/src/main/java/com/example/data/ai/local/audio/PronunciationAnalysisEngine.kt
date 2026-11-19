package com.example.data.ai.local.audio

/**
 * Alignment classification for individual words in the pronunciation comparison.
 */
enum class AlignmentType {
    MATCH,
    ACCENT_VARIANT,
    SUBSTITUTION,
    DELETION,
    INSERTION
}

data class WordAlignmentOp(
    val expectedWord: String?,
    val recognizedWord: String?,
    val type: AlignmentType,
    val score: Float
)

/**
 * Result of the layered pronunciation assessment.
 *
 * **Scientific Transparency Invariant:** If phoneme-level acoustic data is not available,
 * this is explicitly labeled as transcript-based similarity rather than definitive phoneme accuracy.
 */
data class PronunciationAnalysisResult(
    val transcriptMatch: Boolean,
    val recognizedText: String,
    val expectedText: String?,
    val normalizedExpectedText: String?,
    val normalizedRecognizedText: String,
    val matchScore: Float,
    val likelyMispronounced: List<String>,
    val wordAccuracy: Float,
    val intelligibilityEstimate: String,
    val alignmentOperations: List<WordAlignmentOp>,
    val stressPatternNote: String,
    val limitations: List<String>,
    val isPhonemeLevel: Boolean = false,
    val assessmentType: String = "Transcript-Based Word Alignment (Non-Phonetic)"
)

/**
 * Layered Pronunciation Assessment Engine (Stage 1.2).
 *
 * Implements:
 * 1. Reference text normalization & contraction expansion.
 * 2. Speech recognition transcript comparison.
 * 3. Word-level alignment backtrace (Needleman-Wunsch/Levenshtein).
 * 4. Accent tolerance & homophone recognition (e.g. Indian English /th/ -> /t/ or /d/, /v/ <-> /w/).
 * 5. Confidence-aware scoring with explicit honesty labeling.
 */
class PronunciationAnalysisEngine {

    companion object {
        private val LIMITATIONS = listOf(
            "Assessment is based on transcript-level word alignment, not an acoustic phoneme model.",
            "Understandable accent variations and natural spoken contractions are not penalized.",
            "Scores reflect word intelligibility and recognition accuracy, not native-speaker accent conformity."
        )

        private const val STRESS_NOTE =
            "Prosodic stress and intonation contour require a phonetic laboratory acoustic model. " +
                    "Evaluations reflect spoken word accuracy and intelligibility only."

        // Contraction expansions for fair normalization
        private val CONTRACTIONS = mapOf(
            "can't" to "cannot",
            "won't" to "will not",
            "don't" to "do not",
            "didn't" to "did not",
            "it's" to "it is",
            "i'm" to "i am",
            "i've" to "i have",
            "you're" to "you are",
            "they're" to "they are",
            "we're" to "we are",
            "that's" to "that is",
            "there's" to "there is",
            "isn't" to "is not",
            "aren't" to "are not",
            "wasn't" to "was not",
            "weren't" to "were not"
        )

        // Number words mapping
        private val NUMBER_WORDS = mapOf(
            "0" to "zero", "1" to "one", "2" to "two", "3" to "three", "4" to "four",
            "5" to "five", "6" to "six", "7" to "seven", "8" to "eight", "9" to "nine",
            "10" to "ten"
        )

        // Common homophones and understandable accent pairs (e.g. Indian English /th/ -> /t/, /d/)
        private val ACCENT_EQUIVALENTS = mapOf(
            "their" to setOf("there", "they're"),
            "there" to setOf("their", "they're"),
            "they're" to setOf("their", "there"),
            "to" to setOf("too", "two"),
            "too" to setOf("to", "two"),
            "hear" to setOf("here"),
            "here" to setOf("hear"),
            "right" to setOf("write"),
            "write" to setOf("right"),
            // Accent-related phonetic substitutions
            "think" to setOf("tink", "sink"),
            "three" to setOf("tree"),
            "that" to setOf("dat"),
            "this" to setOf("dis"),
            "then" to setOf("den"),
            "very" to setOf("wery"),
            "water" to setOf("vater")
        )
    }

    /**
     * Executes the 5-layer pronunciation evaluation.
     */
    fun analyze(
        recognizedTranscript: String,
        expectedText: String? = null
    ): PronunciationAnalysisResult {
        val normRecognized = normalizeText(recognizedTranscript)

        if (expectedText.isNullOrBlank()) {
            return PronunciationAnalysisResult(
                transcriptMatch = false,
                recognizedText = recognizedTranscript,
                expectedText = null,
                normalizedExpectedText = null,
                normalizedRecognizedText = normRecognized,
                matchScore = 1.0f,
                likelyMispronounced = emptyList(),
                wordAccuracy = 1.0f,
                intelligibilityEstimate = "Unknown — self-paced practice without reference text",
                alignmentOperations = emptyList(),
                stressPatternNote = STRESS_NOTE,
                limitations = LIMITATIONS,
                isPhonemeLevel = false
            )
        }

        val normExpected = normalizeText(expectedText)
        val expectedWords = tokenizeWords(normExpected)
        val recognizedWords = tokenizeWords(normRecognized)

        // Layer 3: Word-Level Alignment Matrix with Backtrace
        val alignmentOps = alignWords(expectedWords, recognizedWords)

        // Layer 4 & 5: Accent tolerance & Confidence-Aware Scoring
        var scoreSum = 0.0f
        var matchCount = 0
        val mispronounced = mutableListOf<String>()

        alignmentOps.forEach { op ->
            when (op.type) {
                AlignmentType.MATCH -> {
                    scoreSum += 1.0f
                    matchCount++
                }
                AlignmentType.ACCENT_VARIANT -> {
                    scoreSum += 0.90f // Fair credit for understandable accent or homophone
                    matchCount++
                }
                AlignmentType.SUBSTITUTION -> {
                    scoreSum += 0.30f
                    if (op.expectedWord != null) mispronounced.add(op.expectedWord)
                }
                AlignmentType.DELETION -> {
                    scoreSum += 0.0f
                    if (op.expectedWord != null) mispronounced.add(op.expectedWord)
                }
                AlignmentType.INSERTION -> {
                    scoreSum += 0.0f // Extra word inserted by speaker
                }
            }
        }

        val totalExpected = expectedWords.size.coerceAtLeast(1)
        val wordAccuracy = (matchCount.toFloat() / totalExpected).coerceIn(0f, 1f)
        val matchScore = (scoreSum / maxOf(totalExpected, alignmentOps.size)).coerceIn(0f, 1f)

        val intelligibility = when {
            matchScore >= 0.88f -> "High"
            matchScore >= 0.68f -> "Medium"
            else                -> "Low"
        }

        val transcriptMatch = matchScore >= 0.88f

        return PronunciationAnalysisResult(
            transcriptMatch = transcriptMatch,
            recognizedText = recognizedTranscript,
            expectedText = expectedText,
            normalizedExpectedText = normExpected,
            normalizedRecognizedText = normRecognized,
            matchScore = matchScore,
            likelyMispronounced = mispronounced.distinct(),
            wordAccuracy = wordAccuracy,
            intelligibilityEstimate = intelligibility,
            alignmentOperations = alignmentOps,
            stressPatternNote = STRESS_NOTE,
            limitations = LIMITATIONS,
            isPhonemeLevel = false,
            assessmentType = "Transcript-Based Word Alignment (Non-Phonetic)"
        )
    }

    // -----------------------------------------------------------------------
    // Text Normalization (Layer 1)
    // -----------------------------------------------------------------------

    internal fun normalizeText(text: String): String {
        var clean = text.lowercase().trim()
        CONTRACTIONS.forEach { (contracted, expanded) ->
            clean = clean.replace(Regex("\\b$contracted\\b", RegexOption.IGNORE_CASE), expanded)
        }
        NUMBER_WORDS.forEach { (digit, word) ->
            clean = clean.replace(Regex("\\b$digit\\b"), word)
        }
        return clean.replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    internal fun tokenizeWords(text: String): List<String> =
        text.split(" ").filter { it.isNotBlank() }

    // -----------------------------------------------------------------------
    // Alignment Algorithm (Layer 3 & 4)
    // -----------------------------------------------------------------------

    private fun alignWords(expected: List<String>, recognized: List<String>): List<WordAlignmentOp> {
        val n = expected.size
        val m = recognized.size
        val dp = Array(n + 1) { FloatArray(m + 1) }

        for (i in 0..n) dp[i][0] = i * 1.0f
        for (j in 0..m) dp[0][j] = j * 1.0f

        for (i in 1..n) {
            for (j in 1..m) {
                val exp = expected[i - 1]
                val rec = recognized[j - 1]

                val matchCost = when {
                    exp == rec -> 0.0f
                    isAccentOrHomophoneEquivalent(exp, rec) -> 0.15f
                    else -> 1.0f
                }

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1.0f,     // deletion
                    dp[i][j - 1] + 1.0f,     // insertion
                    dp[i - 1][j - 1] + matchCost // match/sub
                )
            }
        }

        // Backtrace alignment path
        var i = n
        var j = m
        val ops = mutableListOf<WordAlignmentOp>()

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val exp = expected[i - 1]
                val rec = recognized[j - 1]
                val matchCost = when {
                    exp == rec -> 0.0f
                    isAccentOrHomophoneEquivalent(exp, rec) -> 0.15f
                    else -> 1.0f
                }

                if (dp[i][j] == dp[i - 1][j - 1] + matchCost) {
                    val opType = when {
                        exp == rec -> AlignmentType.MATCH
                        isAccentOrHomophoneEquivalent(exp, rec) -> AlignmentType.ACCENT_VARIANT
                        else -> AlignmentType.SUBSTITUTION
                    }
                    val opScore = if (opType == AlignmentType.MATCH) 1.0f else if (opType == AlignmentType.ACCENT_VARIANT) 0.9f else 0.3f
                    ops.add(WordAlignmentOp(exp, rec, opType, opScore))
                    i--
                    j--
                    continue
                }
            }

            if (i > 0 && dp[i][j] == dp[i - 1][j] + 1.0f) {
                ops.add(WordAlignmentOp(expected[i - 1], null, AlignmentType.DELETION, 0.0f))
                i--
            } else if (j > 0) {
                ops.add(WordAlignmentOp(null, recognized[j - 1], AlignmentType.INSERTION, 0.0f))
                j--
            } else {
                break
            }
        }

        return ops.reversed()
    }

    private fun isAccentOrHomophoneEquivalent(expected: String, recognized: String): Boolean {
        if (expected == recognized) return true
        val eqSet = ACCENT_EQUIVALENTS[expected]
        return eqSet?.contains(recognized) == true
    }
}
