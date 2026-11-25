package com.example.data.learning

import com.example.data.ai.local.language.EvaluationOutcome
import com.example.data.ai.local.language.LocalVocabStageEvaluation
import com.example.data.ai.local.language.LocalVocabularyEvaluator
import com.example.data.model.CurriculumCatalog
import com.example.data.model.VocabSeed
import com.example.data.model.VocabularyEntry
import com.example.speech.word.WordStageEvaluator
import java.time.LocalDate
import kotlin.math.abs

enum class WeeklyQuestionMode(val label: String) {
    SPOKEN_WORD_RECALL("Speak the Word"),
    SPOKEN_SENTENCE_USAGE("Use in a Spoken Sentence")
}

data class WeeklySpokenQuestion(
    val id: String,
    val seed: VocabSeed,
    val mode: WeeklyQuestionMode,
    val prompt: String,
    val hint: String,
    val audioPromptText: String
)

data class WeeklySpokenAnswerResult(
    val questionId: String,
    val wordId: String,
    val word: String,
    val mode: WeeklyQuestionMode,
    val transcript: String,
    val passed: Boolean,
    val isInconclusive: Boolean,
    val score: Int,
    val feedback: String,
    val improvedExample: String
)

/**
 * Pure, deterministic engine handling:
 * 1) User-level calibrated vocabulary filtering.
 * 2) Daily 3 non-repeating words assignment per day without repeats across days.
 * 3) Spoken-word pronunciation & usage verification required before marking a word Done.
 * 4) After-7-days cumulative assessment eligibility & 100% speak-only quiz generation/grading.
 */
object VocabularyMasteryEngine {

    const val DAILY_WORD_COUNT = 3
    const val WEEKLY_ASSESSMENT_DAYS = 7
    const val MAX_WEEKLY_QUESTIONS = 10

    fun filterBankByLevel(
        bank: List<VocabSeed>,
        levelLabel: String
    ): List<VocabSeed> {
        if (levelLabel.equals("All", ignoreCase = true) || levelLabel.isBlank()) return bank
        val normalized = LearningLevel.fromLabel(levelLabel).label
        return bank.filter { it.level.equals(normalized, ignoreCase = true) }
            .ifEmpty { bank }
    }

    /**
     * Ensures today has 3 level-matched, non-repeating words assigned.
     * Avoids all [shownWordIds] (words shown on previous days) until every word at the user's
     * level has had its turn.
     */
    fun ensureDailyVocabularyWords(
        bank: List<VocabSeed> = CurriculumCatalog.vocabularyBank,
        level: LearningLevel,
        todayDateKey: String,
        shownWordIds: List<String> = emptyList(),
        completedAllTimeIds: Set<String> = emptySet(),
        existingAssignment: DailyVocabularyAssignment? = null,
        previousDaysCount: Int = 0
    ): Pair<DailyVocabularyAssignment, List<String>> {
        val validIds = bank.map { it.id }.toSet()

        if (existingAssignment != null &&
            existingAssignment.dateKey == todayDateKey &&
            existingAssignment.level == level &&
            existingAssignment.wordIds.size == DAILY_WORD_COUNT &&
            existingAssignment.wordIds.all { it in validIds }
        ) {
            val updatedShown = (shownWordIds + existingAssignment.wordIds).distinct()
            return existingAssignment to updatedShown
        }

        val levelPool = bank.filter { it.level.equals(level.label, ignoreCase = true) }
            .ifEmpty {
                val adjLabels = LearningLevel.entries
                    .filter { abs(it.rank - level.rank) <= 1 }
                    .map { it.label.lowercase() }
                    .toSet()
                bank.filter { it.level.lowercase() in adjLabels }.ifEmpty { bank }
            }

        val previouslyShownSet = shownWordIds.toSet() + completedAllTimeIds
        // Do not exclude words already selected for this same level if today had an invalid/partial list,
        // otherwise avoid all previously shown/completed words first.
        val freshPool = levelPool.filter { it.id !in previouslyShownSet }

        val seed = abs(todayDateKey.hashCode())
        fun rotated(list: List<VocabSeed>): List<VocabSeed> =
            if (list.isEmpty()) emptyList()
            else List(list.size) { i -> list[Math.floorMod(seed + i, list.size)] }

        val chosen = mutableListOf<VocabSeed>()
        for (item in rotated(freshPool)) {
            if (chosen.size >= DAILY_WORD_COUNT) break
            if (chosen.none { it.id == item.id }) chosen += item
        }

        // If exact-level fresh words are exhausted (e.g. after 7 full days = 21 words),
        // cycle least-recently-shown words at this level before spilling outside level.
        if (chosen.size < DAILY_WORD_COUNT) {
            val leastRecentSameLevel = levelPool
                .filter { s -> chosen.none { it.id == s.id } }
                .sortedBy { s -> shownWordIds.lastIndexOf(s.id) } // -1 first, then oldest shown
            for (item in leastRecentSameLevel) {
                if (chosen.size >= DAILY_WORD_COUNT) break
                chosen += item
            }
        }

        if (chosen.size < DAILY_WORD_COUNT) {
            for (item in rotated(bank)) {
                if (chosen.size >= DAILY_WORD_COUNT) break
                if (chosen.none { it.id == item.id }) chosen += item
            }
        }

        val wordIds = chosen.map { it.id }
        val dayIndex = when {
            existingAssignment != null && existingAssignment.dateKey == todayDateKey -> existingAssignment.dayIndex
            existingAssignment != null && existingAssignment.dateKey.isNotBlank() -> existingAssignment.dayIndex + 1
            else -> (previousDaysCount + 1).coerceAtLeast(1)
        }

        val assignment = DailyVocabularyAssignment(
            dateKey = todayDateKey,
            level = level,
            wordIds = wordIds,
            completedWordIds = emptySet(),
            wordScores = emptyMap(),
            lastTranscripts = emptyMap(),
            dayIndex = dayIndex
        )
        val updatedShown = (shownWordIds + wordIds).distinct()
        return assignment to updatedShown
    }

    /**
     * Evaluates whether the user spoke the target vocabulary word accurately enough
     * to mark it Done. Combines phonetic/Soundex similarity & inflection awareness.
     */
    fun evaluateSpokenWordAttempt(
        seed: VocabSeed,
        transcript: String,
        alternatives: List<String> = emptyList(),
        asrConfidence: Float = 0.85f,
        consecutiveFailures: Int = 0
    ): LocalVocabStageEvaluation {
        val eval = LocalVocabularyEvaluator.evaluateWordSpeaking(
            targetWord = seed.word,
            recognizedText = transcript,
            alternatives = alternatives,
            asrConfidence = asrConfidence,
            consecutiveFailures = consecutiveFailures
        )
        if (eval.isInconclusive || eval.passed) return eval

        val inflections = (seed.wordForms + seed.word).distinct()
        val sayIt = WordStageEvaluator.evaluateSayIt(
            targetWord = seed.word,
            acceptedInflections = inflections,
            transcript = transcript
        )
        return if (sayIt.passed) {
            eval.copy(
                outcome = EvaluationOutcome.PASSED,
                score = maxOf(eval.score, sayIt.score),
                targetWordMatched = true,
                wordAccuracyPercent = maxOf(eval.wordAccuracyPercent, sayIt.score),
                explanation = "Well done! You spoke \"${seed.word}\" clearly (score ${maxOf(eval.score, sayIt.score)}%)."
            )
        } else {
            eval
        }
    }

    /**
     * Computes whether the 7-day cumulative AI Quiz assessment is unlocked and which learned words
     * belong to the current 7-day window.
     */
    fun computeWeeklyEligibility(
        learnedLog: List<LearnedWordRecord>,
        wordProgress: Map<String, VocabularyWordProgress>,
        weeklyAssessments: List<WeeklyVocabularyAssessmentRecord>,
        firstVocabEpochDay: Long,
        vocabularyActiveDateKeys: List<String>,
        todayEpochDay: Long,
        fallbackEntries: List<VocabularyEntry> = emptyList()
    ): WeeklyAssessmentEligibility {
        val latestAssessment = weeklyAssessments.maxByOrNull { it.completedAtMs }
        val lastAssessmentEpochDay = latestAssessment?.completedEpochDay ?: 0L
        val lastAssessmentDateKey = latestAssessment?.completedDateKey.orEmpty()
        val lastAssessmentMs = latestAssessment?.completedAtMs ?: 0L
        val cycleNumber = (weeklyAssessments.size + 1)

        val baselineEpochDay = when {
            lastAssessmentEpochDay > 0L -> lastAssessmentEpochDay
            firstVocabEpochDay > 0L -> firstVocabEpochDay
            learnedLog.isNotEmpty() -> learnedLog.minOf { it.learnedEpochDay }.takeIf { it > 0L } ?: todayEpochDay
            else -> todayEpochDay
        }

        val daysElapsed = (todayEpochDay - baselineEpochDay).coerceAtLeast(0L).toInt()

        val activeDatesSinceBaseline = vocabularyActiveDateKeys
            .filter { dateKey ->
                dateKey.isNotBlank() && (lastAssessmentDateKey.isBlank() || dateKey > lastAssessmentDateKey)
            }
            .distinct()

        val logInCycle = learnedLog.filter { rec ->
            if (lastAssessmentMs > 0L) rec.markedDoneAtMs > lastAssessmentMs || rec.learnedEpochDay > lastAssessmentEpochDay
            else true
        }

        val progressInCycleIds = wordProgress.values
            .filter { wp ->
                (wp.isCompleted || wp.stage2Passed || wp.stage4Passed) &&
                    (lastAssessmentMs <= 0L || wp.updatedAtMs > lastAssessmentMs)
            }
            .map { it.wordId }

        val fallbackIds = if (lastAssessmentMs <= 0L) {
            fallbackEntries
                .filter { it.usageAssessed || it.repetitions > 0L || it.masteryLevel > 0L }
                .mapNotNull { entry ->
                    CurriculumCatalog.vocabularyBank.firstOrNull {
                        it.id == entry.id || it.word.equals(entry.word, ignoreCase = true)
                    }?.id
                }
        } else emptyList()

        val learnedIds = (logInCycle.map { it.wordId } + progressInCycleIds + fallbackIds)
            .filter { it.isNotBlank() }
            .distinct()

        val distinctActiveDays = maxOf(
            activeDatesSinceBaseline.size,
            logInCycle.map { it.learnedDateKey }.filter { it.isNotBlank() }.distinct().size
        )

        val dayCounter = maxOf(daysElapsed, distinctActiveDays)
        val unlockedByDays = dayCounter >= WEEKLY_ASSESSMENT_DAYS
        val isDueNow = unlockedByDays && learnedIds.isNotEmpty()
        val daysRemaining = if (unlockedByDays) 0 else (WEEKLY_ASSESSMENT_DAYS - dayCounter).coerceIn(1, WEEKLY_ASSESSMENT_DAYS)

        return WeeklyAssessmentEligibility(
            isUnlocked = unlockedByDays,
            isDueNow = isDueNow,
            daysElapsed = daysElapsed,
            distinctActiveDays = distinctActiveDays,
            daysRemaining = daysRemaining,
            cycleNumber = cycleNumber,
            learnedWordIdsInCycle = learnedIds,
            latestAssessment = latestAssessment
        )
    }

    /**
     * Resolves concrete [VocabSeed] objects for a list of learned word IDs.
     */
    fun resolveLearnedSeeds(
        learnedWordIds: List<String>,
        bank: List<VocabSeed> = CurriculumCatalog.vocabularyBank,
        entries: List<VocabularyEntry> = emptyList()
    ): List<VocabSeed> {
        val byId = bank.associateBy { it.id }
        val resolved = learnedWordIds.mapNotNull { id ->
            byId[id] ?: entries.firstOrNull { it.id == id }?.let { entry ->
                VocabSeed(
                    id = entry.id.ifBlank { "custom_${entry.word.lowercase()}" },
                    word = entry.word,
                    phonetic = entry.phonetic,
                    meaning = entry.meaning,
                    level = entry.level,
                    synonyms = entry.synonyms,
                    antonyms = entry.antonyms,
                    exampleSentence = entry.exampleSentence,
                    collocations = entry.collocations,
                    conversationExample = entry.conversationExample
                )
            }
        }
        return resolved.distinctBy { it.id }
    }

    /**
     * Generates a 100% speak-only cumulative assessment covering the words learned over the 7-day cycle.
     */
    fun buildWeeklyAssessmentQuestions(
        learnedSeeds: List<VocabSeed>,
        seed: Long = System.currentTimeMillis()
    ): List<WeeklySpokenQuestion> {
        if (learnedSeeds.isEmpty()) return emptyList()
        val chosen = learnedSeeds.shuffled(kotlin.random.Random(seed)).take(MAX_WEEKLY_QUESTIONS)
        return chosen.mapIndexed { idx, vocab ->
            val mode = if (idx % 2 == 0) {
                WeeklyQuestionMode.SPOKEN_WORD_RECALL
            } else {
                WeeklyQuestionMode.SPOKEN_SENTENCE_USAGE
            }
            when (mode) {
                WeeklyQuestionMode.SPOKEN_WORD_RECALL -> WeeklySpokenQuestion(
                    id = "wk_${vocab.id}_recall_$idx",
                    seed = vocab,
                    mode = mode,
                    prompt = "Speak the word you learnt that matches this definition:\n\"${vocab.meaning}\"",
                    hint = "Starts with '${vocab.word.first().uppercaseChar()}' (${vocab.word.length} letters) • ${vocab.category}",
                    audioPromptText = vocab.meaning
                )
                WeeklyQuestionMode.SPOKEN_SENTENCE_USAGE -> WeeklySpokenQuestion(
                    id = "wk_${vocab.id}_sentence_$idx",
                    seed = vocab,
                    mode = mode,
                    prompt = "Use \"${vocab.word}\" naturally by speaking a complete sentence aloud:",
                    hint = "Meaning: ${vocab.meaning}" +
                        (vocab.collocations.firstOrNull()?.let { "\nPhrase idea: \"$it\"" }.orEmpty()),
                    audioPromptText = "${vocab.word}. ${vocab.meaning}"
                )
            }
        }
    }

    /**
     * Evaluates a single spoken response during the 7-day cumulative AI Quiz assessment.
     */
    fun evaluateWeeklyQuestionResponse(
        question: WeeklySpokenQuestion,
        spokenTranscript: String,
        alternatives: List<String> = emptyList(),
        confidence: Float = 0.85f
    ): WeeklySpokenAnswerResult {
        val clean = spokenTranscript.trim()
        return when (question.mode) {
            WeeklyQuestionMode.SPOKEN_WORD_RECALL -> {
                val eval = evaluateSpokenWordAttempt(
                    seed = question.seed,
                    transcript = clean,
                    alternatives = alternatives,
                    asrConfidence = confidence
                )
                WeeklySpokenAnswerResult(
                    questionId = question.id,
                    wordId = question.seed.id,
                    word = question.seed.word,
                    mode = question.mode,
                    transcript = eval.recognizedText.ifBlank { clean },
                    passed = eval.passed,
                    isInconclusive = eval.isInconclusive,
                    score = eval.score,
                    feedback = eval.explanation,
                    improvedExample = question.seed.exampleSentence
                )
            }
            WeeklyQuestionMode.SPOKEN_SENTENCE_USAGE -> {
                val eval = LocalVocabularyEvaluator.evaluateOwnSentence(
                    targetWord = question.seed.word,
                    meaning = question.seed.meaning,
                    userSentence = clean,
                    wordForms = question.seed.wordForms,
                    synonyms = question.seed.synonyms,
                    collocations = question.seed.collocations,
                    exampleFallback = question.seed.exampleSentence,
                    asrConfidence = confidence,
                    difficulty = question.seed.level
                )
                WeeklySpokenAnswerResult(
                    questionId = question.id,
                    wordId = question.seed.id,
                    word = question.seed.word,
                    mode = question.mode,
                    transcript = eval.recognizedText.ifBlank { clean },
                    passed = eval.passed,
                    isInconclusive = eval.isInconclusive,
                    score = eval.score,
                    feedback = eval.explanation,
                    improvedExample = eval.naturalImprovedSentence.ifBlank { question.seed.exampleSentence }
                )
            }
        }
    }

    private fun parseEpochDay(dateKey: String): Long? =
        runCatching { LocalDate.parse(dateKey.trim()).toEpochDay() }.getOrNull()
}
