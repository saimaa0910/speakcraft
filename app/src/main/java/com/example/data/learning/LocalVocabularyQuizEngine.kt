package com.example.data.learning

import com.example.data.ai.local.LocalExecutionPolicy
import com.example.data.ai.local.language.LocalVocabularyEvaluator
import com.example.data.model.VocabSeed
import kotlin.random.Random

/**
 * 100% On-Device Deterministic Smart Local Vocabulary Quiz Engine.
 * Supports all 9 quiz formats (Meaning Selection, Word Selection, Fill-in-the-Blank,
 * Synonym, Antonym, Correct Sentence, Context Usage, Listening Recognition, Speaking Recall)
 * with zero cloud LLM calls and instant explanations.
 */
object LocalVocabularyQuizEngine {

    fun generateQuiz(
        bank: List<VocabSeed>,
        difficultyFilter: String = "All",
        questionCount: Int = 8,
        includeListeningAndSpeaking: Boolean = true,
        seed: Long = System.currentTimeMillis()
    ): List<QuizQuestion> {
        LocalExecutionPolicy.verifyLocalOnly()
        if (bank.isEmpty()) return emptyList()

        val filtered = if (difficultyFilter.equals("All", ignoreCase = true)) {
            bank
        } else {
            bank.filter { it.level.equals(difficultyFilter, ignoreCase = true) }.ifEmpty { bank }
        }

        val rng = Random(seed)
        val shuffledPool = filtered.shuffled(rng)
        val selected = (0 until questionCount.coerceIn(3, 15)).map { idx ->
            shuffledPool[idx % shuffledPool.size]
        }

        val typesPool = buildList {
            add(QuizQuestionType.MEANING_SELECTION)
            add(QuizQuestionType.WORD_SELECTION)
            add(QuizQuestionType.FILL_IN_THE_BLANK)
            add(QuizQuestionType.SYNONYM)
            add(QuizQuestionType.ANTONYM)
            add(QuizQuestionType.CORRECT_SENTENCE)
            add(QuizQuestionType.CONTEXT_USAGE)
            if (includeListeningAndSpeaking) {
                add(QuizQuestionType.LISTENING_RECOGNITION)
                add(QuizQuestionType.SPEAKING_RECALL)
            }
        }

        return selected.mapIndexed { idx, target ->
            val type = typesPool[idx % typesPool.size]
            buildQuestion(
                index = idx,
                target = target,
                allWords = bank,
                type = type,
                rng = rng
            )
        }
    }

    fun evaluateAnswer(question: QuizQuestion, userAnswer: String): QuizAnswerReview {
        val cleanUser = userAnswer.trim()
        val isCorrect = when (question.type) {
            QuizQuestionType.SPEAKING_RECALL -> {
                val eval = LocalVocabularyEvaluator.evaluateWordSpeaking(
                    targetWord = question.correctAnswer,
                    recognizedText = cleanUser
                )
                eval.passed
            }
            else -> cleanUser.equals(question.correctAnswer.trim(), ignoreCase = true)
        }
        return QuizAnswerReview(
            questionId = question.id,
            word = question.word,
            type = question.type,
            userAnswer = cleanUser.ifBlank { "(No answer)" },
            correctAnswer = question.correctAnswer,
            isCorrect = isCorrect,
            explanation = question.explanation
        )
    }

    private fun buildQuestion(
        index: Int,
        target: VocabSeed,
        allWords: List<VocabSeed>,
        type: QuizQuestionType,
        rng: Random
    ): QuizQuestion {
        val distractors = allWords.filter { it.id != target.id }.shuffled(rng).take(3)
        val qId = "q_${index}_${target.id}_${type.name.lowercase()}"

        return when (type) {
            QuizQuestionType.MEANING_SELECTION -> {
                val options = (listOf(target.meaning) + distractors.map { it.meaning }).distinct().shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Choose the clearest definition of \"${target.word}\":",
                    options = options,
                    correctAnswer = target.meaning,
                    explanation = "\"${target.word}\" (${target.phonetic}) means: ${target.meaning}",
                    difficulty = target.level
                )
            }
            QuizQuestionType.WORD_SELECTION -> {
                val options = (listOf(target.word) + distractors.map { it.word }).distinct().shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Which word matches this definition?\n\"${target.meaning}\"",
                    options = options,
                    correctAnswer = target.word,
                    explanation = "\"${target.word}\" is the best match. Example: ${target.exampleSentence}",
                    difficulty = target.level
                )
            }
            QuizQuestionType.FILL_IN_THE_BLANK -> {
                val blanked = target.exampleSentence.replace(
                    Regex("\\b${Regex.escape(target.word)}[a-z]*\\b", RegexOption.IGNORE_CASE),
                    "_____"
                ).let { if (it.contains("_____")) it else "Strong speakers use _____ to make their ideas stand out." }
                val options = (listOf(target.word) + distractors.map { it.word }).distinct().shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Fill in the blank:\n\"$blanked\"",
                    options = options,
                    correctAnswer = target.word,
                    explanation = "Complete sentence: \"${target.exampleSentence}\"",
                    difficulty = target.level
                )
            }
            QuizQuestionType.SYNONYM -> {
                val correctSyn = target.synonyms.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Clear"
                val wrongSyns = distractors.mapNotNull { it.synonyms.firstOrNull()?.replaceFirstChar { c -> c.uppercase() } }
                    .filter { !it.equals(correctSyn, ignoreCase = true) }
                    .take(3)
                val options = (listOf(correctSyn) + wrongSyns).distinct().shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Select the closest synonym for \"${target.word}\":",
                    options = options,
                    correctAnswer = correctSyn,
                    explanation = "Synonyms of \"${target.word}\" include: ${target.synonyms.joinToString(", ")}.",
                    difficulty = target.level
                )
            }
            QuizQuestionType.ANTONYM -> {
                val correctAnt = target.antonyms.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Unclear"
                val wrongAnts = distractors.mapNotNull { it.antonyms.firstOrNull()?.replaceFirstChar { c -> c.uppercase() } }
                    .filter { !it.equals(correctAnt, ignoreCase = true) }
                    .take(3)
                val options = (listOf(correctAnt) + wrongAnts).distinct().shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Select the opposite (antonym) of \"${target.word}\":",
                    options = options,
                    correctAnswer = correctAnt,
                    explanation = "Antonyms of \"${target.word}\" include: ${target.antonyms.joinToString(", ")}.",
                    difficulty = target.level
                )
            }
            QuizQuestionType.CORRECT_SENTENCE -> {
                val good = target.exampleSentence
                val bad1 = "Yesterday I ${target.word.lowercase()}ed to the grocery aisle without buying milk."
                val bad2 = "My laptop charger is very ${target.word.lowercase()} when plugged into the wall socket."
                val bad3 = "She drank a tall glass of ${target.word.lowercase()} before jogging."
                val options = listOf(good, bad1, bad2, bad3).shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Which sentence uses \"${target.word}\" naturally and accurately?",
                    options = options,
                    correctAnswer = good,
                    explanation = "\"$good\" uses \"${target.word}\" in a natural communication context.",
                    difficulty = target.level
                )
            }
            QuizQuestionType.CONTEXT_USAGE -> {
                val collocation = target.collocations.firstOrNull() ?: target.word.lowercase()
                val options = (listOf(target.word) + distractors.map { it.word }).distinct().shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Context scenario:\n${target.conversationExample.replace(Regex(Regex.escape(target.word), RegexOption.IGNORE_CASE), "_____")}\n\nWhich word best fits the blank?",
                    options = options,
                    correctAnswer = target.word,
                    explanation = "Notice the natural phrase \"$collocation\" in context.",
                    difficulty = target.level
                )
            }
            QuizQuestionType.LISTENING_RECOGNITION -> {
                val options = (listOf(target.word) + distractors.map { it.word }).distinct().shuffled(rng)
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Tap Listen to hear the spoken word, then select what you heard:",
                    audioPromptText = target.word,
                    options = options,
                    correctAnswer = target.word,
                    explanation = "You heard \"${target.word}\" (${target.phonetic}) — ${target.meaning}",
                    difficulty = target.level
                )
            }
            QuizQuestionType.SPEAKING_RECALL -> {
                QuizQuestion(
                    id = qId,
                    wordId = target.id,
                    word = target.word,
                    type = type,
                    prompt = "Read the clue and SPEAK (or type) the exact word aloud:\n\"${target.meaning}\"",
                    audioPromptText = target.meaning,
                    options = emptyList(),
                    correctAnswer = target.word,
                    explanation = "Target word: \"${target.word}\" (${target.phonetic}). Example: ${target.exampleSentence}",
                    difficulty = target.level
                )
            }
        }
    }
}
