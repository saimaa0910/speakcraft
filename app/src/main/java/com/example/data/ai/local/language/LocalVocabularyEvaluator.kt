package com.example.data.ai.local.language

import com.example.data.ai.local.LocalExecutionPolicy
import com.example.data.ai.local.audio.AcousticFeatures
import com.example.data.ai.local.audio.AlignmentType
import com.example.data.ai.local.audio.PronunciationAnalysisEngine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Outcome status for any vocabulary speaking/sentence evaluation stage.
 * [INCONCLUSIVE] prevents unfair failures when on-device speech recognition confidence is low
 * or microphone input was too faint, prompting a clean retry instead.
 */
enum class EvaluationOutcome {
    PASSED,
    NEEDS_RETRY,
    INCONCLUSIVE
}

/**
 * Configurable pass thresholds for the 5-stage vocabulary workflow.
 */
data class VocabularyEvaluationThresholds(
    val speakWordPassScore: Int = 75,
    val speakSentencePassScore: Int = 75,
    val ownSentencePassScore: Int = 70,
    val minConfidentAsrThreshold: Float = 0.42f
)

/**
 * Detailed, honest, 100% on-device evaluation report for Stage 2, Stage 3, or Stage 4.
 */
data class LocalVocabStageEvaluation(
    val stageNumber: Int,
    val outcome: EvaluationOutcome,
    val score: Int,
    val passThreshold: Int,
    val recognizedText: String,
    val targetWordMatched: Boolean,
    val wordAccuracyPercent: Int = 0,
    val missingWords: List<String> = emptyList(),
    val extraWords: List<String> = emptyList(),
    val correctedSentence: String = "",
    val mistakes: List<String> = emptyList(),
    val naturalImprovedSentence: String = "",
    val fluencyNote: String = "",
    val explanation: String = "",
    val honestyNotice: String = HONESTY_NOTICE,
    val suggestEasierFallback: Boolean = false
) {
    val passed: Boolean get() = outcome == EvaluationOutcome.PASSED
    val isInconclusive: Boolean get() = outcome == EvaluationOutcome.INCONCLUSIVE

    companion object {
        const val HONESTY_NOTICE =
            "Evaluated 100% on-device using speech recognition alignment, acoustic DSP, and rule-based grammar checks (no cloud LLM; non-phonemic)."
    }
}

/**
 * Dedicated 100% On-Device Vocabulary Stage Evaluator powering:
 * - Stage 2: Speak the Word (accent-tolerant phonetic & edit-distance matching + confidence gating)
 * - Stage 3: Speak Practice Sentences (word-level alignment + target word check + pace/fluency)
 * - Stage 4: Create & Speak Own Sentence (inflection matching + grammar + completeness + context fit)
 */
object LocalVocabularyEvaluator {

    private val pronunciationEngine = PronunciationAnalysisEngine()

    private val TRIVIAL_SENTENCE_PATTERNS = listOf(
        Regex("^i\\s+(like|love|know|said|say|use|learned)\\s+(the\\s+word\\s+)?[a-z]+\\.?$", RegexOption.IGNORE_CASE),
        Regex("^(this|that)\\s+is\\s+(a\\s+|an\\s+)?[a-z]+\\.?$", RegexOption.IGNORE_CASE)
    )

    // -----------------------------------------------------------------------
    // STAGE 2: SPEAK THE WORD
    // -----------------------------------------------------------------------

    fun evaluateWordSpeaking(
        targetWord: String,
        recognizedText: String,
        alternatives: List<String> = emptyList(),
        asrConfidence: Float = 0.85f,
        acousticFeatures: AcousticFeatures? = null,
        thresholds: VocabularyEvaluationThresholds = VocabularyEvaluationThresholds(),
        consecutiveFailures: Int = 0
    ): LocalVocabStageEvaluation {
        LocalExecutionPolicy.verifyLocalOnly()

        val cleanTarget = normalizeSingleWord(targetWord)
        val allCandidates = (listOf(recognizedText) + alternatives)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Check for silent/unusable audio or empty recognition -> INCONCLUSIVE (never unfair fail!)
        if (allCandidates.isEmpty()) {
            val audioHeard = acousticFeatures != null && acousticFeatures.speechDurationSeconds >= 0.25 &&
                acousticFeatures.relativeLoudness > -44.0
            return LocalVocabStageEvaluation(
                stageNumber = 2,
                outcome = EvaluationOutcome.INCONCLUSIVE,
                score = 0,
                passThreshold = thresholds.speakWordPassScore,
                recognizedText = "",
                targetWordMatched = false,
                explanation = if (audioHeard) {
                    "Your voice was captured, but offline speech recognition could not produce a confident transcript. Tap Speak again clearly at a steady pace."
                } else {
                    "No clear speech was detected. Move closer to the microphone and say \"$targetWord\" cleanly."
                },
                fluencyNote = if (acousticFeatures?.clippingDetected == true) "Audio clipping detected — hold phone 15 cm away." else "",
                suggestEasierFallback = consecutiveFailures >= 2
            )
        }

        // Evaluate best match across primary and alternative ASR hypotheses
        var bestScore = 0
        var bestMatched = false
        var bestRecognized = allCandidates.first()

        for (candidate in allCandidates) {
            val tokens = candidate.lowercase()
                .replace(Regex("[^a-z'\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            for (tok in tokens) {
                val sim = computeWordSimilarity(cleanTarget, tok)
                val bonus = when {
                    tok == cleanTarget -> 100
                    isInflectionOf(cleanTarget, tok) -> 96
                    soundex(cleanTarget) == soundex(tok) && sim >= 0.65f -> (sim * 100).roundToInt().coerceAtLeast(82)
                    else -> (sim * 100).roundToInt()
                }
                if (bonus > bestScore) {
                    bestScore = bonus
                    bestRecognized = candidate
                    bestMatched = bonus >= thresholds.speakWordPassScore
                }
            }
        }

        // Acoustic clarity modifier (gentle: rewards clear audio, warns on clipping)
        var adjustedScore = bestScore
        val acousticNote = when {
            acousticFeatures == null -> "Evaluated via on-device speech recognition word similarity."
            acousticFeatures.clippingDetected -> {
                adjustedScore = (adjustedScore - 4).coerceAtLeast(0)
                "Slight microphone clipping detected — speak a little softer or step back slightly."
            }
            acousticFeatures.relativeLoudness < -38.0 -> {
                "Voice volume was quiet (${acousticFeatures.relativeLoudness.roundToInt()} dBFS). Project a bit more from your diaphragm."
            }
            else -> "Clear vocal volume (${acousticFeatures.relativeLoudness.roundToInt()} dBFS)."
        }

        // Confidence gating: if ASR confidence is too low and word didn't clearly match -> INCONCLUSIVE
        if (!bestMatched && asrConfidence in 0.01f..thresholds.minConfidentAsrThreshold) {
            return LocalVocabStageEvaluation(
                stageNumber = 2,
                outcome = EvaluationOutcome.INCONCLUSIVE,
                score = adjustedScore.coerceIn(0, 100),
                passThreshold = thresholds.speakWordPassScore,
                recognizedText = bestRecognized,
                targetWordMatched = false,
                wordAccuracyPercent = adjustedScore.coerceIn(0, 100),
                explanation = "Recognition confidence was low (${(asrConfidence * 100).roundToInt()}%). We heard \"$bestRecognized\", which is inconclusive — tap Retry and say \"$targetWord\" slowly.",
                fluencyNote = acousticNote,
                suggestEasierFallback = consecutiveFailures >= 2
            )
        }

        val finalScore = adjustedScore.coerceIn(0, 100)
        val passed = finalScore >= thresholds.speakWordPassScore

        val explanation = when {
            finalScore >= 95 -> "Excellent! You pronounced \"$targetWord\" clearly and it was recognized accurately."
            passed -> "Well done! \"$targetWord\" was clearly intelligible (similarity $finalScore%)."
            else -> "We heard \"$bestRecognized\" instead of \"$targetWord\" (similarity $finalScore%). Listen to the reference audio once more and emphasize each syllable."
        }

        return LocalVocabStageEvaluation(
            stageNumber = 2,
            outcome = if (passed) EvaluationOutcome.PASSED else EvaluationOutcome.NEEDS_RETRY,
            score = finalScore,
            passThreshold = thresholds.speakWordPassScore,
            recognizedText = bestRecognized,
            targetWordMatched = passed,
            wordAccuracyPercent = finalScore,
            explanation = explanation,
            fluencyNote = acousticNote,
            suggestEasierFallback = !passed && consecutiveFailures >= 2
        )
    }

    // -----------------------------------------------------------------------
    // STAGE 3: SPEAK PRACTICE SENTENCES
    // -----------------------------------------------------------------------

    fun evaluatePracticeSentence(
        targetWord: String,
        expectedSentence: String,
        recognizedText: String,
        asrConfidence: Float = 0.85f,
        durationSeconds: Double = 0.0,
        acousticFeatures: AcousticFeatures? = null,
        thresholds: VocabularyEvaluationThresholds = VocabularyEvaluationThresholds(),
        consecutiveFailures: Int = 0
    ): LocalVocabStageEvaluation {
        LocalExecutionPolicy.verifyLocalOnly()

        val cleanRecognized = recognizedText.trim()
        if (cleanRecognized.isBlank()) {
            return LocalVocabStageEvaluation(
                stageNumber = 3,
                outcome = EvaluationOutcome.INCONCLUSIVE,
                score = 0,
                passThreshold = thresholds.speakSentencePassScore,
                recognizedText = "",
                targetWordMatched = false,
                explanation = "No speech transcript was captured. Tap Speak and read the sentence aloud at a steady pace.",
                suggestEasierFallback = consecutiveFailures >= 2
            )
        }

        val alignment = pronunciationEngine.analyze(cleanRecognized, expectedSentence)
        val missingWords = alignment.alignmentOperations
            .filter { it.type == AlignmentType.DELETION || it.type == AlignmentType.SUBSTITUTION }
            .mapNotNull { it.expectedWord }
            .distinct()
        val extraWords = alignment.alignmentOperations
            .filter { it.type == AlignmentType.INSERTION }
            .mapNotNull { it.recognizedWord }
            .distinct()

        val targetPresent = containsTargetOrInflection(targetWord, cleanRecognized)
        val wordAccuracyPct = (alignment.wordAccuracy * 100f).roundToInt().coerceIn(0, 100)
        val matchScorePct = (alignment.matchScore * 100f).roundToInt().coerceIn(0, 100)

        // Fluency & pacing score
        val wordCount = LocalLanguageAnalysisEngine.tokenizeWords(cleanRecognized).size
        val effectiveDuration = when {
            acousticFeatures != null && acousticFeatures.speechDurationSeconds > 0.5 -> acousticFeatures.speechDurationSeconds
            durationSeconds > 0.5 -> durationSeconds
            else -> (wordCount / 2.2).coerceAtLeast(1.0)
        }
        val wpm = ((wordCount / effectiveDuration) * 60.0).roundToInt().coerceIn(30, 300)
        val fluencyScore = when {
            wpm in 100..175 -> 95
            wpm in 80..195 -> 82
            else -> 68
        }

        // Low-confidence recognition safeguard
        if (asrConfidence in 0.01f..0.38f && matchScorePct < 45) {
            return LocalVocabStageEvaluation(
                stageNumber = 3,
                outcome = EvaluationOutcome.INCONCLUSIVE,
                score = matchScorePct,
                passThreshold = thresholds.speakSentencePassScore,
                recognizedText = cleanRecognized,
                targetWordMatched = targetPresent,
                wordAccuracyPercent = wordAccuracyPct,
                missingWords = missingWords,
                extraWords = extraWords,
                explanation = "Recognition confidence was low (${(asrConfidence * 100).roundToInt()}%). Please retry in a quieter spot so we can fairly evaluate your sentence.",
                suggestEasierFallback = consecutiveFailures >= 2
            )
        }

        // Composite score: 65% alignment match, 20% target word presence, 15% fluency
        val targetComponent = if (targetPresent) 100 else 25
        val finalScore = ((matchScorePct * 0.65) + (targetComponent * 0.20) + (fluencyScore * 0.15))
            .roundToInt()
            .coerceIn(0, 100)
        val passed = finalScore >= thresholds.speakSentencePassScore && targetPresent

        val mistakes = buildList {
            if (!targetPresent) add("Target word \"$targetWord\" was not clearly recognized in the sentence.")
            if (missingWords.isNotEmpty()) add("Missed or unclear words: ${missingWords.take(4).joinToString(", ")}.")
            if (extraWords.isNotEmpty()) add("Extra words inserted: ${extraWords.take(3).joinToString(", ")}.")
        }

        val fluencyNote = buildString {
            append("Estimated pace: ~$wpm WPM")
            when {
                wpm > 180 -> append(" (slightly fast — pause briefly around \"$targetWord\").")
                wpm < 90 -> append(" (deliberate pace — try connecting phrases smoothly).")
                else -> append(" (clear, natural cadence).")
            }
        }

        val explanation = when {
            passed -> "Great delivery! Word alignment reached $wordAccuracyPct% and \"$targetWord\" landed clearly."
            !targetPresent -> "Almost there — make sure the target word \"$targetWord\" is pronounced distinctly."
            else -> "Sentence match was $wordAccuracyPct%. Focus on the highlighted words (${missingWords.take(3).joinToString(", ")}) and try once more."
        }

        return LocalVocabStageEvaluation(
            stageNumber = 3,
            outcome = if (passed) EvaluationOutcome.PASSED else EvaluationOutcome.NEEDS_RETRY,
            score = finalScore,
            passThreshold = thresholds.speakSentencePassScore,
            recognizedText = cleanRecognized,
            targetWordMatched = targetPresent,
            wordAccuracyPercent = wordAccuracyPct,
            missingWords = missingWords,
            extraWords = extraWords,
            mistakes = mistakes,
            fluencyNote = fluencyNote,
            explanation = explanation,
            suggestEasierFallback = !passed && consecutiveFailures >= 2
        )
    }

    // -----------------------------------------------------------------------
    // STAGE 4: CREATE & SPEAK OWN SENTENCE
    // -----------------------------------------------------------------------

    fun evaluateOwnSentence(
        targetWord: String,
        meaning: String,
        userSentence: String,
        wordForms: List<String> = emptyList(),
        synonyms: List<String> = emptyList(),
        collocations: List<String> = emptyList(),
        exampleFallback: String = "",
        asrConfidence: Float = 1.0f,
        durationSeconds: Double = 0.0,
        acousticFeatures: AcousticFeatures? = null,
        difficulty: String = "Intermediate",
        thresholds: VocabularyEvaluationThresholds = VocabularyEvaluationThresholds(),
        consecutiveFailures: Int = 0
    ): LocalVocabStageEvaluation {
        LocalExecutionPolicy.verifyLocalOnly()

        val clean = userSentence.trim()
        val words = LocalLanguageAnalysisEngine.tokenizeWords(clean)

        if (clean.isBlank()) {
            return LocalVocabStageEvaluation(
                stageNumber = 4,
                outcome = EvaluationOutcome.INCONCLUSIVE,
                score = 0,
                passThreshold = thresholds.ownSentencePassScore,
                recognizedText = "",
                targetWordMatched = false,
                naturalImprovedSentence = buildNaturalExample(targetWord, meaning, collocations, exampleFallback),
                explanation = "Speak or type an original sentence using \"$targetWord\" so we can evaluate it locally."
            )
        }

        if (asrConfidence in 0.01f..0.38f && words.size < 3) {
            return LocalVocabStageEvaluation(
                stageNumber = 4,
                outcome = EvaluationOutcome.INCONCLUSIVE,
                score = 0,
                passThreshold = thresholds.ownSentencePassScore,
                recognizedText = clean,
                targetWordMatched = false,
                naturalImprovedSentence = buildNaturalExample(targetWord, meaning, collocations, exampleFallback),
                explanation = "Speech recognition confidence was low (${(asrConfidence * 100).roundToInt()}%). Retry speaking or refine the transcript below."
            )
        }

        val acceptedForms = (listOf(targetWord) + wordForms).map { it.lowercase().trim() }.filter { it.isNotBlank() }
        val includesTarget = acceptedForms.any { form -> containsTargetOrInflection(form, clean) }
        val isTrivial = TRIVIAL_SENTENCE_PATTERNS.any { it.matches(clean) }

        val lang = LocalLanguageAnalysisEngine.analyze(
            text = clean,
            targetWords = acceptedForms,
            transcriptConfidence = asrConfidence
        )

        // Check contextual richness: collocations, meaning keywords, subordinating/coordinating connectors
        val lower = clean.lowercase()
        val usesCollocation = collocations.any { it.isNotBlank() && lower.contains(it.lowercase()) }
        val hasRichConnector = Regex(
            "\\b(because|although|though|while|since|so that|whenever|during|before|after|unless|despite|whereas)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lower)
        val meaningTokens = LocalLanguageAnalysisEngine.tokenizeWords(meaning)
            .filter { it.length >= 4 }
            .toSet()
        val contextOverlap = words.count { it in meaningTokens || it in synonyms.map(String::lowercase) }

        val completenessScore = when {
            words.size >= 8 && hasRichConnector && !isTrivial -> 100
            words.size >= 8 && !isTrivial -> 94
            words.size in 5..7 && !isTrivial -> 84
            words.size == 4 && !isTrivial -> 70
            else -> 40
        }
        val grammarScore = (100 - lang.grammarIssues.size * 16).coerceIn(30, 100)
        val contextScore = when {
            !includesTarget -> 15
            isTrivial -> 45
            usesCollocation && hasRichConnector -> 99
            usesCollocation -> 95
            contextOverlap >= 1 && hasRichConnector -> 93
            contextOverlap >= 1 || (words.size >= 8 && hasRichConnector) -> 89
            words.size >= 8 -> 84
            else -> 75
        }

        val rawComposite = if (!includesTarget) {
            min(44, ((grammarScore * 0.4) + (completenessScore * 0.25)).roundToInt())
        } else {
            ((contextScore * 0.40) + (grammarScore * 0.35) + (completenessScore * 0.25)).roundToInt()
        }.coerceIn(10, 99)

        val passed = includesTarget && !isTrivial && words.size >= 4 &&
            rawComposite >= thresholds.ownSentencePassScore && lang.grammarIssues.size <= 1

        val corrected = LocalLanguageAnalysisEngine.applyCorrections(clean, lang.grammarIssues)
        val polishedDraft = DynamicLocalCoachEngine.analyzeUserDraft(corrected, targetWord)
            .rewrites.firstOrNull()?.improved?.takeIf { !it.equals(corrected, ignoreCase = true) }

        val naturalSuggestion = when {
            !includesTarget -> buildNaturalExample(targetWord, meaning, collocations, exampleFallback)
            polishedDraft != null -> polishedDraft
            lang.grammarIssues.isNotEmpty() -> corrected
            passed && words.size >= 6 -> corrected
            else -> buildNaturalExample(targetWord, meaning, collocations, exampleFallback)
        }

        val mistakes = buildList {
            if (!includesTarget) add("Target word \"$targetWord\" (or one of its word forms) is missing.")
            if (isTrivial) add("Sentence mentions the word without demonstrating its real-world meaning.")
            if (words.size < 5) add("Sentence is too short (${words.size} words) — add a reason, place, or situation.")
            lang.grammarIssues.forEach { issue ->
                add("${issue.issueType}: \"${issue.originalText}\" → \"${issue.correctedText}\" (${issue.explanation})")
            }
            if (includesTarget && !isTrivial && !hasRichConnector && words.size < 9) {
                add("To elevate naturalness, connect a cause or outcome using 'because', 'while', or 'so that'.")
            }
        }

        val fluencyNote = if (durationSeconds > 0.5 || acousticFeatures != null) {
            val dur = acousticFeatures?.speechDurationSeconds?.takeIf { it > 0.5 } ?: durationSeconds
            val wpm = ((words.size / dur.coerceAtLeast(1.0)) * 60.0).roundToInt().coerceIn(40, 280)
            "Spoken clarity: ~$wpm WPM (${words.size} words)."
        } else {
            "${words.size} words • $difficulty-level semantic & grammar evaluation."
        }

        val explanation = when {
            passed && lang.grammarIssues.isEmpty() && (usesCollocation || hasRichConnector) ->
                "Executive-grade sentence! \"$targetWord\" flows naturally with rich context and clean grammar."
            passed && lang.grammarIssues.isEmpty() ->
                "Strong original sentence! You used \"$targetWord\" naturally with clean grammar and clear context."
            passed ->
                "Nice work using \"$targetWord\" in context! Check the small grammar polish note to make it even sharper."
            !includesTarget ->
                "Include \"$targetWord\" directly inside your sentence so we can verify active usage."
            isTrivial ->
                "Show what \"$targetWord\" means in a real situation instead of just naming the word."
            else ->
                "Good effort! Address the highlighted tip${if (mistakes.size > 1) "s" else ""} below and retry."
        }

        return LocalVocabStageEvaluation(
            stageNumber = 4,
            outcome = if (passed) EvaluationOutcome.PASSED else EvaluationOutcome.NEEDS_RETRY,
            score = rawComposite,
            passThreshold = thresholds.ownSentencePassScore,
            recognizedText = clean,
            targetWordMatched = includesTarget,
            wordAccuracyPercent = rawComposite,
            correctedSentence = corrected,
            mistakes = mistakes,
            naturalImprovedSentence = naturalSuggestion,
            fluencyNote = fluencyNote,
            explanation = explanation,
            suggestEasierFallback = !passed && consecutiveFailures >= 2
        )
    }

    // -----------------------------------------------------------------------
    // Natural Example & Phonetic / Morphological Helpers
    // -----------------------------------------------------------------------

    fun buildNaturalExample(
        word: String,
        meaning: String,
        collocations: List<String> = emptyList(),
        existingExample: String = ""
    ): String {
        if (existingExample.isNotBlank()) return existingExample.trim()
        val firstCollocation = collocations.firstOrNull { it.isNotBlank() }
        if (firstCollocation != null) {
            return "In today's team update, I made a conscious effort to $firstCollocation so everyone stayed aligned."
        }
        val cleanWord = word.trim()
        val lowerMeaning = meaning.lowercase()
        return when {
            lowerMeaning.startsWith("to ") ->
                "Before the client meeting, I practiced how to ${cleanWord.lowercase()} my key points clearly."
            lowerMeaning.startsWith("able to") || lowerMeaning.startsWith("giving ") || lowerMeaning.startsWith("deep,") ->
                "Our team appreciated her ${cleanWord.lowercase()} explanation during the Q&A session."
            else ->
                "Strong speakers rely on ${cleanWord.lowercase()} to keep their audience engaged from start to finish."
        }
    }

    internal fun containsTargetOrInflection(targetWord: String, sentence: String): Boolean {
        val cleanTarget = normalizeSingleWord(targetWord)
        if (cleanTarget.isBlank()) return false
        val tokens = LocalLanguageAnalysisEngine.tokenizeWords(sentence)
        return tokens.any { tok ->
            tok == cleanTarget || isInflectionOf(cleanTarget, tok)
        }
    }

    internal fun isInflectionOf(base: String, candidate: String): Boolean {
        if (base == candidate) return true
        val stem = base.removeSuffix("e").removeSuffix("y")
        if (stem.length < 3) return false
        val commonSuffixes = listOf("s", "es", "ed", "d", "ing", "ly", "er", "est", "ion", "ation", "ity", "ness", "ied", "ies", "al")
        if (commonSuffixes.any { candidate == base + it || candidate == stem + it }) return true
        return stem.length >= 5 && candidate.startsWith(stem) && (candidate.length - stem.length) in 1..5
    }

    internal fun computeWordSimilarity(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val dist = levenshtein(a, b)
        val maxLen = max(a.length, b.length)
        return (1f - dist.toFloat() / maxLen.toFloat()).coerceIn(0f, 1f)
    }

    internal fun levenshtein(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }

    /**
     * American Soundex phonetic code for accent-tolerant single-word similarity.
     */
    internal fun soundex(word: String): String {
        val clean = word.uppercase().filter { it in 'A'..'Z' }
        if (clean.isEmpty()) return ""
        fun mapChar(c: Char): Char = when (c) {
            'B', 'F', 'P', 'V' -> '1'
            'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2'
            'D', 'T' -> '3'
            'L' -> '4'
            'M', 'N' -> '5'
            'R' -> '6'
            else -> '0'
        }
        val sb = StringBuilder().append(clean[0])
        var prevCode = mapChar(clean[0])
        for (i in 1 until clean.length) {
            val code = mapChar(clean[i])
            if (code != '0' && code != prevCode) {
                sb.append(code)
                if (sb.length == 4) break
            }
            prevCode = code
        }
        while (sb.length < 4) sb.append('0')
        return sb.toString()
    }

    private fun normalizeSingleWord(word: String): String =
        word.lowercase().replace(Regex("[^a-z']"), "").trim()
}
