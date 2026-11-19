package com.example.data.ai.local.language

// ---------------------------------------------------------------------------
// Data Classes (Stage 1.2: Modular Grammar & Language Intelligence)
// ---------------------------------------------------------------------------

/**
 * Represents a single grammar or syntactic issue detected locally.
 *
 * @param originalText        The erroneous phrase found in the transcript.
 * @param correctedText       The suggested correction.
 * @param issueType           Short category label (e.g. "Subject-Verb Agreement").
 * @param explanation         Actionable, plain-English explanation.
 * @param ruleId              Unique rule identifier (e.g. "SVA_001", "ART_002").
 * @param confidenceLevel     Rule confidence score (0.0 to 1.0).
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
    val correctionFn: (MatchResult) -> String,
    val explanationFn: (MatchResult) -> String
)

// ---------------------------------------------------------------------------
// Local Language Analysis Engine
// ---------------------------------------------------------------------------

/**
 * 100% on-device modular language and grammar engine.
 * Uses deterministic rules with rule IDs, confidence scores, and conservative filtering
 * to avoid over-correcting natural spoken English or uncertain ASR transcripts.
 */
object LocalLanguageAnalysisEngine {

    private const val RUN_ON_WORD_THRESHOLD = 45

    // -----------------------------------------------------------------------
    // Modular Grammar Rules Catalogue (11 Distinct Categories)
    // -----------------------------------------------------------------------

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

        // 2. Tense Consistency (TNS)
        ModularRule(
            ruleId = "TNS_001",
            category = "Tense Consistency",
            pattern = Regex("\\b(did|didn't)\\s+(went|saw|ate|bought|wrote|spoke)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m ->
                val base = when (m.groupValues[2].lowercase()) {
                    "went" -> "go"; "saw" -> "see"; "ate" -> "eat"
                    "bought" -> "buy"; "wrote" -> "write"; else -> "speak"
                }
                "${m.groupValues[1]} $base"
            },
            explanationFn = { m -> "The auxiliary verb '${m.groupValues[1]}' takes the base form of the verb, not the past tense." }
        ),
        ModularRule(
            ruleId = "TNS_002",
            category = "Tense Consistency",
            pattern = Regex("\\b(yesterday|last\\s+(?:week|month|year))\\s+(?:i|he|she|we|they)\\s+(is|am|are|go|see)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.85f,
            correctionFn = { m ->
                val past = when (m.groupValues[2].lowercase()) {
                    "is", "am" -> "was"; "are" -> "were"; "go" -> "went"; else -> "saw"
                }
                "${m.groupValues[1]} ... $past"
            },
            explanationFn = { "Past time markers require past-tense verbs for temporal consistency." }
        ),

        // 3. Articles (ART)
        ModularRule(
            ruleId = "ART_001",
            category = "Article",
            pattern = Regex("\\ba\\s+([aeiou][a-z]+)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            correctionFn = { m -> "an ${m.groupValues[1]}" },
            explanationFn = { m ->
                val word = m.groupValues[1].lowercase()
                if (word.startsWith("uni") || word.startsWith("one") || word.startsWith("use"))
                    "Words beginning with vowel sounds take 'an'; exceptions apply to 'y' sounds like in '$word'."
                else
                    "Use 'an' before words beginning with a vowel sound ('${m.groupValues[1]}')."
            }
        ),
        ModularRule(
            ruleId = "ART_002",
            category = "Article",
            pattern = Regex("\\ban\\s+([bcdfghjklmnpqrstvwxyz][a-z]+)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            correctionFn = { m -> "a ${m.groupValues[1]}" },
            explanationFn = { m ->
                val word = m.groupValues[1].lowercase()
                if (word.startsWith("hour") || word.startsWith("honest") || word.startsWith("honor"))
                    "Words starting with silent 'h' take 'an'; other consonant sounds take 'a'."
                else
                    "Use 'a' before words starting with a consonant sound ('${m.groupValues[1]}')."
            }
        ),

        // 4. Prepositions (PRP)
        ModularRule(
            ruleId = "PRP_001",
            category = "Preposition",
            pattern = Regex("\\b(discuss)\\s+(about)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { "discuss" },
            explanationFn = { "'Discuss' is a transitive verb that directly takes an object without 'about'." }
        ),
        ModularRule(
            ruleId = "PRP_002",
            category = "Preposition",
            pattern = Regex("\\b(listen)\\s+(music|to\\s+me|radio|podcasts)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.85f,
            correctionFn = { m ->
                if (!m.groupValues[2].startsWith("to")) "listen to ${m.groupValues[2]}" else m.value
            },
            explanationFn = { "The verb 'listen' requires the preposition 'to' before its object." }
        ),
        ModularRule(
            ruleId = "PRP_003",
            category = "Preposition",
            pattern = Regex("\\b(explain)\\s+(me|him|her|them)\\b", RegexOption.IGNORE_CASE),
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
            explanationFn = { "In English, the collocation is 'depend on' (or 'upon'), not 'depend of'." }
        ),

        // 5. Pronouns (PRN)
        ModularRule(
            ruleId = "PRN_001",
            category = "Pronoun",
            pattern = Regex("\\b(me\\s+and\\s+[a-z]+)\\s+(went|did|are|were|have|thought)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.90f,
            correctionFn = { m -> "${m.groupValues[1].replace("me and", "").trim()} and I ${m.groupValues[2]}" },
            explanationFn = { "Use subject pronoun 'I' rather than object pronoun 'me' as the subject of a clause." }
        ),
        ModularRule(
            ruleId = "PRN_002",
            category = "Pronoun",
            pattern = Regex("\\b(between\\s+you\\s+and)\\s+(i)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { "between you and me" },
            explanationFn = { "Prepositions take object pronouns ('between you and me', not 'between you and I')." }
        ),
        ModularRule(
            ruleId = "PRN_003",
            category = "Pronoun",
            pattern = Regex("\\b(myself\\s+and\\s+[a-z]+)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.88f,
            correctionFn = { m -> "${m.groupValues[1].replace("myself and", "").trim()} and I" },
            explanationFn = { "Use 'I' or 'me' instead of reflexive 'myself' unless referring back to a previously mentioned subject." }
        ),

        // 6. Singular/Plural Number Agreement (NUM)
        ModularRule(
            ruleId = "NUM_001",
            category = "Singular/Plural Agreement",
            pattern = Regex("\\b(two|three|four|five|several|many)\\s+(year|month|day|week|time|hour)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            correctionFn = { m -> "${m.groupValues[1]} ${m.groupValues[2]}s" },
            explanationFn = { m -> "Numbers greater than one require the plural form '${m.groupValues[2]}s'." }
        ),
        ModularRule(
            ruleId = "NUM_002",
            category = "Singular/Plural Agreement",
            pattern = Regex("\\b(much)\\s+(people|items|things|questions|times)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> "many ${m.groupValues[2]}" },
            explanationFn = { "Use 'many' with countable plural nouns ('people', 'items'), and 'much' with uncountable nouns." }
        ),
        ModularRule(
            ruleId = "NUM_003",
            category = "Singular/Plural Agreement",
            pattern = Regex("\\b(less)\\s+(people|students|workers|employees)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.85f,
            correctionFn = { m -> "fewer ${m.groupValues[2]}" },
            explanationFn = { "Use 'fewer' for countable plural nouns ('people', 'students') and 'less' for uncountable amounts." }
        ),

        // 7. Common Word-Choice Confusions (WCH)
        ModularRule(
            ruleId = "WCH_001",
            category = "Word Choice",
            pattern = Regex("\\b(this\\s+will|it\\s+will|to)\\s+(effect)\\s+(the|our|my|your)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.88f,
            correctionFn = { m -> "${m.groupValues[1]} affect ${m.groupValues[3]}" },
            explanationFn = { "'Affect' is typically the verb (to influence), while 'effect' is the noun (a result)." }
        ),
        ModularRule(
            ruleId = "WCH_002",
            category = "Word Choice",
            pattern = Regex("\\b(loose)\\s+(the|my|our|your|a)\\s+(game|job|opportunity|chance|match)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.92f,
            correctionFn = { m -> "lose ${m.groupValues[2]} ${m.groupValues[3]}" },
            explanationFn = { "Use 'lose' (to misplace or be defeated). 'Loose' means not tight." }
        ),
        ModularRule(
            ruleId = "WCH_003",
            category = "Word Choice",
            pattern = Regex("\\b(good)\\s+(advice)\\s+(to\\s+me|from)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.85f,
            correctionFn = { m -> m.value },
            explanationFn = { "'Advice' is a noun; 'advise' is the verb." }
        ),

        // 8. Basic Sentence Structure & Double Negatives (STR)
        ModularRule(
            ruleId = "STR_001",
            category = "Sentence Structure",
            pattern = Regex("\\b(can't|don't|won't|didn't|couldn't)\\s+(not)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m ->
                val modal = m.groupValues[1].replace("n't", "").lowercase()
                "$modal"
            },
            explanationFn = { "Avoid double negatives in formal English ('${m.value}'). Use a single negative or positive phrasing." }
        ),
        ModularRule(
            ruleId = "STR_002",
            category = "Sentence Structure",
            pattern = Regex("\\b(don't\\s+have|didn't\\s+have)\\s+(no)\\b", RegexOption.IGNORE_CASE),
            confidence = 0.95f,
            correctionFn = { m -> "${m.groupValues[1]} any" },
            explanationFn = { "Replace 'no' with 'any' after a negative verb to avoid double negatives." }
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

        // 1. Modular Grammar Rules Check
        val detectedIssues = mutableListOf<GrammarIssue>()

        // If ASR transcript confidence is low (< 0.5), suppress subtle rules to prevent false corrections
        val minConfidenceThreshold = if (transcriptConfidence < 0.6f) 0.90f else 0.70f

        for (rule in MODULAR_RULES) {
            if (rule.confidence < minConfidenceThreshold) continue

            val matches = rule.pattern.findAll(text)
            for (match in matches) {
                // Prevent duplicate overlapping spans
                if (detectedIssues.none { it.originalText.equals(match.value, ignoreCase = true) }) {
                    detectedIssues.add(
                        GrammarIssue(
                            originalText = match.value,
                            correctedText = rule.correctionFn(match),
                            issueType = rule.category,
                            explanation = rule.explanationFn(match),
                            ruleId = rule.ruleId,
                            confidenceLevel = rule.confidence
                        )
                    )
                }
            }
        }

        // 2. Sentence Fragments Check (FRG_001)
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
                            correctedText = "[Connect to main sentence]",
                            issueType = "Sentence Fragment",
                            explanation = "This dependent clause starts with a subordinating conjunction without a complete main clause.",
                            ruleId = "FRG_001",
                            confidenceLevel = 0.85f
                        )
                    )
                }
            }
        }

        // 3. Run-on Sentence Check (RUN_001)
        sentences.forEach { s ->
            val wCount = tokenizeWords(s).size
            if (wCount > RUN_ON_WORD_THRESHOLD) {
                detectedIssues.add(
                    GrammarIssue(
                        originalText = s.take(70) + if (s.length > 70) "..." else "",
                        correctedText = "[Split into 2-3 concise sentences]",
                        issueType = "Run-on Sentence",
                        explanation = "This sentence is very long ($wCount words). Break it into smaller thoughts to help the listener follow easily.",
                        ruleId = "RUN_001",
                        confidenceLevel = 0.90f
                    )
                )
            }
        }

        // 4. Repeated Words Check (REP_001)
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

        // 5. Vocabulary Richness & Diversity
        val vocabAnalysis = analyzeVocabulary(allWords, targetWords)

        // 6. Sentence Structure Metrics
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
            lowerWords.any { it.equals(target.lowercase(), ignoreCase = true) }
        }

        val suggestions = mutableListOf<String>()
        if (repeatedWords.isNotEmpty()) {
            val topRepeated = repeatedWords.entries.sortedByDescending { it.value }.take(3)
            suggestions += "Replace repeated crutch words: ${topRepeated.joinToString(", ") { "\"${it.key}\" (${it.value}x)" }}."
        }
        if (diversityScore < 40 && totalWords > 20) {
            suggestions += "Vocabulary diversity is $diversityScore%. Incorporate more descriptive adjectives and verbs."
        }
        if (targetWords.isNotEmpty() && usedTargetWords.isEmpty()) {
            suggestions += "Try using the target activity vocabulary: ${targetWords.take(4).joinToString(", ")}."
        }
        if (suggestions.isEmpty()) {
            suggestions += "Good lexical variety and appropriate word selection."
        }

        return VocabularyAnalysis(
            targetWordsUsed = usedTargetWords,
            repeatedWords = repeatedWords,
            vocabularyDiversityScore = diversityScore,
            suggestions = suggestions
        )
    }

    private fun splitIntoSentences(text: String): List<String> =
        text.trim().split(Regex("[.!?]+")).map { it.trim() }.filter { it.length > 3 }

    private fun tokenizeWords(text: String): List<String> =
        text.trim().split(Regex("\\s+")).map { it.replace(Regex("[^a-zA-Z']"), "").lowercase() }.filter { it.isNotEmpty() }

    private fun buildComplexityNote(avgLen: Double, diversity: Int, issues: Int): String = buildString {
        when {
            avgLen > 24 -> append("Sentences are quite long (avg ${avgLen.toInt()} words). Shorter sentences improve clarity. ")
            avgLen < 7 && avgLen > 0 -> append("Sentences are brief. Try connecting related thoughts. ")
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
