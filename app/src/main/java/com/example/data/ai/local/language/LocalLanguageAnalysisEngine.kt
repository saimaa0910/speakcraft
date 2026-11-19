package com.example.data.ai.local.language

// ---------------------------------------------------------------------------
// Data Classes (Modular On-Device Grammar & Language Intelligence)
// ---------------------------------------------------------------------------

/**
 * Represents a single grammar or syntactic issue detected locally.
 */
data class GrammarIssue(
    val originalText: String,
    val correctedText: String,
    val issueType: String,
    val explanation: String,
    val ruleId: String = "GRM_GENERIC",
    val confidenceLevel: Float = 0.90f
)

/**
 * Vocabulary statistics derived from the transcript.
 */
data class VocabularyAnalysis(
    val targetWordsUsed: List<String>,
    val repeatedWords: Map<String, Int>,
    val vocabularyDiversityScore: Int,
    val suggestions: List<String>
)

/**
 * Complete result returned by [LocalLanguageAnalysisEngine.analyze].
 */
data class LanguageAnalysisResult(
    val grammarIssues: List<GrammarIssue>,
    val vocabularyAnalysis: VocabularyAnalysis,
    val sentenceCount: Int,
    val averageSentenceLength: Double,
    val textComplexityNote: String
)

// ---------------------------------------------------------------------------
// Modular Rule Definition
// ---------------------------------------------------------------------------

data class ModularRule(
    val ruleId: String,
    val category: String,
    val pattern: Regex,
    val confidence: Float,
    val validator: (MatchResult) -> Boolean = { true },
    val correctionFn: (MatchResult) -> String,
    val explanationFn: (MatchResult) -> String
)

// ---------------------------------------------------------------------------
// Local Language Analysis Engine
// ---------------------------------------------------------------------------

/**
 * 100% on-device modular language and grammar engine.
 * Uses deterministic rules with rule IDs, confidence scores, phonetic article exception guards,
 * and conservative filtering so natural spoken English is never falsely penalized.
 */
object LocalLanguageAnalysisEngine {

    private const val RUN_ON_WORD_THRESHOLD = 45

    private val VOWEL_LETTER_CONSONANT_SOUND_PREFIXES = listOf(
        "uni", "use", "usu", "eur", "one", "once", "ute", "ubi", "eup", "eul"
    )

    private val SILENT_H_WORDS = setOf(
        "hour", "hours", "hourly", "honest", "honestly", "honesty",
        "honor", "honour", "honorable", "honourable", "heir", "heiress", "herb"
    )

    private val MODULAR_RULES = listOf(
        // 1. Subject-Verb Agreement (SVA)
        ModularRule(
            ruleId = "SVA_001",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(he|she|it|that|this)\\s+(don't)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> "${m.groupValues[1]} doesn't" },
            explanationFn = { m -> "Use 'doesn't' with third-person singular subjects ('${m.groupValues[1]}')." }
        ),
        ModularRule(
            ruleId = "SVA_002",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(i|we|they|you)\\s+(doesn't)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> "${m.groupValues[1]} don't" },
            explanationFn = { m -> "Use 'don't' with '${m.groupValues[1]}'." }
        ),
        ModularRule(
            ruleId = "SVA_003",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(they|we|you)\\s+(was)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> "${m.groupValues[1]} were" },
            explanationFn = { m -> "Use 'were' with plural subjects and 'you' in past tense." }
        ),
        ModularRule(
            ruleId = "SVA_004",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(i)\\s+(has)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { "I have" },
            explanationFn = { "Use 'have' with the first-person pronoun 'I'." }
        ),
        ModularRule(
            ruleId = "SVA_005",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(there\\s+is)\\s+(\\d+|many|two|three|several)\\s+([a-z]+s)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            correctionFn = { m -> "there are ${m.groupValues[2]} ${m.groupValues[3]}" },
            explanationFn = { "Use 'there are' before plural nouns." }
        ),
        ModularRule(
            ruleId = "SVA_006",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(he|she|it)\\s+(have|are|were|go|do)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.94f,
            correctionFn = { m ->
                val fixed = when (m.groupValues[2].lowercase()) {
                    "have" -> "has"
                    "are" -> "is"
                    "were" -> "was"
                    "go" -> "goes"
                    else -> "does"
                }
                "${m.groupValues[1]} $fixed"
            },
            explanationFn = { m -> "Third-person singular '${m.groupValues[1]}' takes the singular verb form." }
        ),
        ModularRule(
            ruleId = "SVA_007",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(i)\\s+(is|are)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.96f,
            correctionFn = { "I am" },
            explanationFn = { "Use 'I am' rather than 'I is' or 'I are'." }
        ),
        ModularRule(
            ruleId = "SVA_008",
            category = "Subject-Verb Agreement",
            pattern = Regex("\\b(we|they|you)\\s+(is|has)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m ->
                val fixed = if (m.groupValues[2].equals("is", ignoreCase = true)) "are" else "have"
                "${m.groupValues[1]} $fixed"
            },
            explanationFn = { m -> "Use plural verb forms ('are'/'have') with '${m.groupValues[1]}'." }
        ),

        // 2. Tense Consistency & Auxiliaries (TNS)
        ModularRule(
            ruleId = "TNS_001",
            category = "Tense Consistency",
            pattern = Regex("\\b(did|didn't)\\s+(went|saw|ate|bought|wrote|spoke|came|took|made|gave|said|told)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m ->
                val base = when (m.groupValues[2].lowercase()) {
                    "went" -> "go"; "saw" -> "see"; "ate" -> "eat"
                    "bought" -> "buy"; "wrote" -> "write"; "spoke" -> "speak"
                    "came" -> "come"; "took" -> "take"; "made" -> "make"
                    "gave" -> "give"; "said" -> "say"; else -> "tell"
                }
                "${m.groupValues[1]} $base"
            },
            explanationFn = { m -> "The auxiliary verb '${m.groupValues[1]}' takes the base form of the verb, not past tense." }
        ),
        ModularRule(
            ruleId = "TNS_002",
            category = "Tense Consistency",
            pattern = Regex("\\b(yesterday|last\\s+(?:week|month|year))\\s+(i|he|she|we|they)\\s+(is|am|are|go|see|speak|walk)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.88f,
            correctionFn = { m ->
                val past = when (m.groupValues[3].lowercase()) {
                    "is", "am" -> "was"; "are" -> "were"; "go" -> "went"
                    "see" -> "saw"; "speak" -> "spoke"; else -> "walked"
                }
                "${m.groupValues[1]} ${m.groupValues[2]} $past"
            },
            explanationFn = { "Past time markers ('yesterday', 'last week') require past-tense verbs." }
        ),
        ModularRule(
            ruleId = "TNS_003",
            category = "Tense Consistency",
            pattern = Regex("\\b(can|could|should|would|will|must)\\s+(went|spoke|ate|saw|did|had|was|were)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.93f,
            correctionFn = { m ->
                val base = when (m.groupValues[2].lowercase()) {
                    "went" -> "go"; "spoke" -> "speak"; "ate" -> "eat"; "saw" -> "see"
                    "did" -> "do"; "had" -> "have"; else -> "be"
                }
                "${m.groupValues[1]} $base"
            },
            explanationFn = { m -> "Modal verbs like '${m.groupValues[1]}' are followed by the base infinitive form." }
        ),

        // 3. Articles (ART) — with proper phonetic exception guards
        ModularRule(
            ruleId = "ART_001",
            category = "Article",
            pattern = Regex("\\ba\\s+([aeiou][a-z]+)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            validator = { m ->
                val w = m.groupValues[1].lowercase()
                VOWEL_LETTER_CONSONANT_SOUND_PREFIXES.none { w.startsWith(it) }
            },
            correctionFn = { m -> "an ${m.groupValues[1]}" },
            explanationFn = { m -> "Use 'an' before words starting with a vowel sound ('${m.groupValues[1]}')." }
        ),
        ModularRule(
            ruleId = "ART_002",
            category = "Article",
            pattern = Regex("\\ban\\s+([bcdfghjklmnpqrstvwxyz][a-z]+)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            validator = { m ->
                val w = m.groupValues[1].lowercase()
                w !in SILENT_H_WORDS
            },
            correctionFn = { m -> "a ${m.groupValues[1]}" },
            explanationFn = { m -> "Use 'a' before words starting with a consonant sound ('${m.groupValues[1]}')." }
        ),

        // 4. Prepositions (PRP)
        ModularRule(
            ruleId = "PRP_001",
            category = "Preposition",
            pattern = Regex("\\b(discuss)\\s+(about)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { "discuss" },
            explanationFn = { "'Discuss' directly takes an object without 'about' (e.g., 'discuss the plan')." }
        ),
        ModularRule(
            ruleId = "PRP_002",
            category = "Preposition",
            pattern = Regex("\\b(listen)\\s+(music|radio|podcasts|me|him|her)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            correctionFn = { m -> "listen to ${m.groupValues[2]}" },
            explanationFn = { "The verb 'listen' requires the preposition 'to' before its object." }
        ),
        ModularRule(
            ruleId = "PRP_003",
            category = "Preposition",
            pattern = Regex("\\b(explain)\\s+(me|him|her|them|us)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            correctionFn = { m -> "explain to ${m.groupValues[2]}" },
            explanationFn = { "Use 'explain to [person]' rather than 'explain [person]'." }
        ),
        ModularRule(
            ruleId = "PRP_004",
            category = "Preposition",
            pattern = Regex("\\b(married)\\s+(with)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { "married to" },
            explanationFn = { "Use 'married to [person]' instead of 'married with'." }
        ),
        ModularRule(
            ruleId = "PRP_005",
            category = "Preposition",
            pattern = Regex("\\b(depend)\\s+(of)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { "depend on" },
            explanationFn = { "The standard English collocation is 'depend on', not 'depend of'." }
        ),
        ModularRule(
            ruleId = "PRP_006",
            category = "Preposition",
            pattern = Regex("\\b(in)\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            correctionFn = { m -> "on ${m.groupValues[2]}" },
            explanationFn = { m -> "Use 'on' before specific days of the week ('on ${m.groupValues[2]}')." }
        ),

        // 5. Pronouns (PRN)
        ModularRule(
            ruleId = "PRN_001",
            category = "Pronoun",
            pattern = Regex("\\b(me\\s+and\\s+[a-z]+)\\s+(went|did|are|were|have|thought)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            correctionFn = { m -> "${m.groupValues[1].replace("me and", "", ignoreCase = true).trim()} and I ${m.groupValues[2]}" },
            explanationFn = { "Use subject pronoun 'I' rather than object pronoun 'me' as the subject of a clause." }
        ),
        ModularRule(
            ruleId = "PRN_002",
            category = "Pronoun",
            pattern = Regex("\\b(between\\s+you\\s+and)\\s+(i)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { "between you and me" },
            explanationFn = { "Prepositions take object pronouns ('between you and me')." }
        ),
        ModularRule(
            ruleId = "PRN_003",
            category = "Pronoun",
            pattern = Regex("\\b(myself\\s+and\\s+[a-z]+)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.88f,
            correctionFn = { m -> "${m.groupValues[1].replace("myself and", "", ignoreCase = true).trim()} and I" },
            explanationFn = { "Use 'I' or 'me' instead of reflexive 'myself' as a compound subject." }
        ),

        // 6. Singular/Plural Number Agreement (NUM)
        ModularRule(
            ruleId = "NUM_001",
            category = "Singular/Plural Agreement",
            pattern = Regex("\\b(two|three|four|five|several|many)\\s+(year|month|day|week|time|hour|minute|word|idea|point)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            correctionFn = { m -> "${m.groupValues[1]} ${m.groupValues[2]}s" },
            explanationFn = { m -> "Quantities greater than one require the plural noun '${m.groupValues[2]}s'." }
        ),
        ModularRule(
            ruleId = "NUM_002",
            category = "Singular/Plural Agreement",
            pattern = Regex("\\b(much)\\s+(people|items|things|questions|times|words|ideas)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> "many ${m.groupValues[2]}" },
            explanationFn = { "Use 'many' with countable plural nouns and 'much' with uncountable nouns." }
        ),
        ModularRule(
            ruleId = "NUM_003",
            category = "Singular/Plural Agreement",
            pattern = Regex("\\b(less)\\s+(people|students|workers|employees|mistakes|fillers)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.85f,
            correctionFn = { m -> "fewer ${m.groupValues[2]}" },
            explanationFn = { "Use 'fewer' with countable plural nouns and 'less' with uncountable amounts." }
        ),

        // 7. Word Choice & Comparatives (WCH)
        ModularRule(
            ruleId = "WCH_001",
            category = "Word Choice",
            pattern = Regex("\\b(this\\s+will|it\\s+will|to)\\s+(effect)\\s+(the|our|my|your)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.88f,
            correctionFn = { m -> "${m.groupValues[1]} affect ${m.groupValues[3]}" },
            explanationFn = { "'Affect' is the verb (to influence); 'effect' is usually the noun (the outcome)." }
        ),
        ModularRule(
            ruleId = "WCH_002",
            category = "Word Choice",
            pattern = Regex("\\b(loose)\\s+(the|my|our|your|a)\\s+(game|job|opportunity|chance|match|focus)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            correctionFn = { m -> "lose ${m.groupValues[2]} ${m.groupValues[3]}" },
            explanationFn = { "Use 'lose' (to misplace or fail to keep). 'Loose' means not tight." }
        ),
        ModularRule(
            ruleId = "WCH_004",
            category = "Comparatives",
            pattern = Regex("\\b(more)\\s+(better|worse|easier|faster|clearer|stronger|bigger)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> m.groupValues[2] },
            explanationFn = { m -> "Avoid double comparatives — '${m.groupValues[2]}' is already comparative." }
        ),
        ModularRule(
            ruleId = "WCH_005",
            category = "Conciseness",
            pattern = Regex("\\b(return\\s+back|repeat\\s+again|revert\\s+back|reply\\s+back)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            correctionFn = { m -> m.value.split(Regex("\\s+")).first() },
            explanationFn = { m -> "'${m.value}' is redundant — '${m.value.split(Regex("\\s+")).first()}' already carries that meaning." }
        ),

        // 8. Sentence Structure & Double Negatives (STR)
        ModularRule(
            ruleId = "STR_001",
            category = "Sentence Structure",
            pattern = Regex("\\b(can't|don't|won't|didn't|couldn't)\\s+(not)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> m.groupValues[1].replace("n't", "", ignoreCase = true).lowercase() },
            explanationFn = { m -> "Avoid double negatives ('${m.value}'). Use a single clear negative or positive phrasing." }
        ),
        ModularRule(
            ruleId = "STR_002",
            category = "Sentence Structure",
            pattern = Regex("\\b(don't\\s+have|didn't\\s+have)\\s+(no)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> "${m.groupValues[1]} any" },
            explanationFn = { "Replace 'no' with 'any' after a negative verb to avoid double negatives." }
        ),
        ModularRule(
            ruleId = "STR_003",
            category = "Missing Linking Verb",
            pattern = Regex("\\b(i)\\s+(very|really|so)\\s+(happy|excited|confident|nervous|tired|ready|grateful)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            correctionFn = { m -> "I am ${m.groupValues[2]} ${m.groupValues[3]}" },
            explanationFn = { "Include the linking verb 'am' between 'I' and the adjective phrase." }
        )
    )

    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
        "have", "has", "had", "do", "does", "did", "i", "you", "he", "she",
        "it", "we", "they", "this", "that", "these", "those", "my", "your",
        "his", "her", "its", "our", "their", "me", "him", "them", "us"
    )

    /**
     * Analyzes [text] for modular grammar rules, sentence length, and vocabulary richness.
     */
    fun analyze(
        text: String,
        targetWords: List<String> = emptyList(),
        transcriptConfidence: Float = 1.0f
    ): LanguageAnalysisResult {
        if (text.isBlank()) return emptyResult()

        val sentences = splitIntoSentences(text)
        val allWords = tokenizeWords(text)
        val detectedIssues = mutableListOf<GrammarIssue>()

        // Suppress subtle rules when ASR transcript confidence is low so we never fabricate errors
        val minConfidenceThreshold = if (transcriptConfidence < 0.6f) 0.92f else 0.75f

        for (rule in MODULAR_RULES) {
            if (rule.confidence < minConfidenceThreshold) continue

            val matches = rule.pattern.findAll(text)
            for (match in matches) {
                if (!rule.validator(match)) continue
                val corrected = rule.correctionFn(match)
                if (corrected.equals(match.value, ignoreCase = true)) continue
                if (detectedIssues.none { it.originalText.equals(match.value, ignoreCase = true) }) {
                    detectedIssues.add(
                        GrammarIssue(
                            originalText = match.value,
                            correctedText = corrected,
                            issueType = rule.category,
                            explanation = rule.explanationFn(match),
                            ruleId = rule.ruleId,
                            confidenceLevel = rule.confidence
                        )
                    )
                }
            }
        }

        // Sentence Fragment check (FRG_001)
        sentences.forEach { s ->
            val trimmed = s.trim()
            if (trimmed.startsWith("Because ", ignoreCase = true) ||
                trimmed.startsWith("Although ", ignoreCase = true) ||
                trimmed.startsWith("Since ", ignoreCase = true)
            ) {
                if (tokenizeWords(trimmed).size < 7 && !trimmed.contains(",")) {
                    detectedIssues.add(
                        GrammarIssue(
                            originalText = trimmed,
                            correctedText = "[Connect to a complete main clause]",
                            issueType = "Sentence Fragment",
                            explanation = "This clause begins with a subordinating conjunction without a complete main clause.",
                            ruleId = "FRG_001",
                            confidenceLevel = 0.85f
                        )
                    )
                }
            }
        }

        // Run-on Sentence check (RUN_001)
        sentences.forEach { s ->
            val wCount = tokenizeWords(s).size
            if (wCount > RUN_ON_WORD_THRESHOLD) {
                detectedIssues.add(
                    GrammarIssue(
                        originalText = s.take(70) + if (s.length > 70) "..." else "",
                        correctedText = "[Split into 2–3 concise sentences]",
                        issueType = "Run-on Sentence",
                        explanation = "This sentence is very long ($wCount words). Breaking it into shorter thoughts improves listener clarity.",
                        ruleId = "RUN_001",
                        confidenceLevel = 0.90f
                    )
                )
            }
        }

        // Repeated adjacent words check (REP_001)
        val duplicateMatch = Regex("\\b([a-zA-Z]{2,})\\s+\\1\\b", RegexOption.IGNORE_CASE).find(text)
        if (duplicateMatch != null) {
            detectedIssues.add(
                GrammarIssue(
                    originalText = duplicateMatch.value,
                    correctedText = duplicateMatch.groupValues[1],
                    issueType = "Word Repetition",
                    explanation = "Accidental duplicate word '${duplicateMatch.groupValues[1]}' detected.",
                    ruleId = "REP_001",
                    confidenceLevel = 0.98f
                )
            )
        }

        val vocabAnalysis = analyzeVocabulary(allWords, targetWords)
        val sentenceCount = maxOf(sentences.size, 1)
        val avgSentenceLength = allWords.size.toDouble() / sentenceCount
        val complexityNote = buildComplexityNote(avgSentenceLength, vocabAnalysis.vocabularyDiversityScore, detectedIssues.size)

        return LanguageAnalysisResult(
            grammarIssues = detectedIssues,
            vocabularyAnalysis = vocabAnalysis,
            sentenceCount = sentenceCount,
            averageSentenceLength = avgSentenceLength,
            textComplexityNote = complexityNote
        )
    }

    /**
     * Applies safe in-place deterministic corrections to [text] and normalizes leading capitalization
     * and terminal punctuation.
     */
    fun applyCorrections(text: String, issues: List<GrammarIssue>): String {
        var result = text.trim()
        issues.forEach { issue ->
            if (!issue.correctedText.startsWith("[")) {
                result = result.replace(issue.originalText, issue.correctedText, ignoreCase = true)
            }
        }
        if (result.isNotBlank()) {
            result = result.replaceFirstChar { it.uppercase() }
            if (result.last() !in listOf('.', '!', '?')) {
                result = "$result."
            }
        }
        return result
    }

    private fun analyzeVocabulary(words: List<String>, targetWords: List<String>): VocabularyAnalysis {
        val lowerWords = words.map { it.lowercase() }
        val totalWords = lowerWords.size

        val frequencyMap = lowerWords.filter { it.length > 2 }
            .groupingBy { it }
            .eachCount()

        val repeatedWords = frequencyMap.filter { it.value > 3 && it.key !in STOP_WORDS }

        val uniqueWords = lowerWords.filter { it !in STOP_WORDS && it.length > 2 }.toSet()
        val diversityScore = if (totalWords > 0) {
            ((uniqueWords.size.toFloat() / totalWords) * 100).toInt().coerceIn(0, 100)
        } else 0

        val usedTargetWords = targetWords.filter { target ->
            val stem = target.lowercase().removeSuffix("e").removeSuffix("y")
            lowerWords.any { w ->
                w.equals(target, ignoreCase = true) ||
                    (stem.length >= 4 && w.startsWith(stem) && w.length <= target.length + 4)
            }
        }

        val suggestions = mutableListOf<String>()
        if (repeatedWords.isNotEmpty()) {
            val topRepeated = repeatedWords.entries.sortedByDescending { it.value }.take(3)
            suggestions += "Replace repeated crutch words: ${topRepeated.joinToString(", ") { "\"${it.key}\" (${it.value}x)" }}."
        }
        if (diversityScore < 40 && totalWords > 20) {
            suggestions += "Vocabulary diversity is $diversityScore%. Incorporate more descriptive verbs and nouns."
        }
        if (targetWords.isNotEmpty() && usedTargetWords.isEmpty()) {
            suggestions += "Try weaving in the target vocabulary: ${targetWords.take(4).joinToString(", ")}."
        }
        if (suggestions.isEmpty()) {
            suggestions += "Good lexical variety and natural word choice."
        }

        return VocabularyAnalysis(
            targetWordsUsed = usedTargetWords,
            repeatedWords = repeatedWords,
            vocabularyDiversityScore = diversityScore,
            suggestions = suggestions
        )
    }

    private fun splitIntoSentences(text: String): List<String> =
        text.trim().split(Regex("[.!?]+")).map { it.trim() }.filter { it.length > 2 }

    internal fun tokenizeWords(text: String): List<String> =
        text.trim().split(Regex("\\s+")).map { it.replace(Regex("[^a-zA-Z']"), "").lowercase() }.filter { it.isNotEmpty() }

    private fun buildComplexityNote(avgLen: Double, diversity: Int, issues: Int): String = buildString {
        when {
            avgLen > 24 -> append("Sentences are quite long (avg ${avgLen.toInt()} words). Shorter sentences improve clarity. ")
            avgLen in 1.0..6.9 -> append("Sentences are brief. Try connecting related thoughts smoothly. ")
            else -> append("Sentence length is well-balanced (avg ${avgLen.toInt()} words). ")
        }
        append("Vocabulary diversity is $diversity%. ")
        if (issues == 0) append("Zero grammar issues detected.")
        else append("$issues grammar/structural issue(s) identified.")
    }.trim()

    private fun emptyResult() = LanguageAnalysisResult(
        grammarIssues = emptyList(),
        vocabularyAnalysis = VocabularyAnalysis(emptyList(), emptyMap(), 0, listOf("No text provided.")),
        sentenceCount = 0,
        averageSentenceLength = 0.0,
        textComplexityNote = "No text provided."
    )
}
