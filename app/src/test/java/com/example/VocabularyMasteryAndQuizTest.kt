package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.learning.LearningLevel
import com.example.data.learning.LearningProgressManager
import com.example.data.learning.VocabularyMasteryEngine
import com.example.data.learning.WeeklyAssessmentEligibility
import com.example.data.learning.WeeklyVocabularyAssessmentRecord
import com.example.data.media.LiveSpeechCaptureResult
import com.example.data.model.CurriculumCatalog
import com.example.ui.screens.VocabularyScreen
import com.example.ui.theme.SpeakCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class VocabularyMasteryAndQuizTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun vocabulary_catalog_provides_21_unique_words_per_learning_level() {
        val bank = CurriculumCatalog.vocabularyBank
        assertEquals(105, bank.size)
        assertEquals(105, bank.map { it.id }.toSet().size)
        assertEquals(105, bank.map { it.word.lowercase() }.toSet().size)

        LearningLevel.entries.forEach { level ->
            val words = bank.filter { it.level.equals(level.label, ignoreCase = true) }
            assertEquals("Expected 21 words for ${level.label}", 21, words.size)
            words.forEach { w ->
                assertTrue(w.phonetic.isNotBlank())
                assertTrue(w.meaning.isNotBlank())
                assertTrue(w.exampleSentence.isNotBlank())
                assertTrue(w.collocations.isNotEmpty())
            }
        }
    }

    @Test
    fun daily_three_words_match_user_level_and_do_not_repeat_across_7_days() {
        LearningLevel.entries.forEach { level ->
            var dateDay = 10
            var shown = emptyList<String>()
            val allWordsSeenOver7Days = mutableListOf<String>()

            repeat(7) { day ->
                val dateKey = "2026-11-${dateDay + day}"
                val (assignment, updatedShown) = VocabularyMasteryEngine.ensureDailyVocabularyWords(
                    bank = CurriculumCatalog.vocabularyBank,
                    level = level,
                    todayDateKey = dateKey,
                    shownWordIds = shown,
                    previousDaysCount = day
                )
                assertEquals(3, assignment.wordIds.size)
                assertEquals(level, assignment.level)
                assignment.wordIds.forEach { id ->
                    val seed = CurriculumCatalog.vocabularyBank.first { it.id == id }
                    assertEquals(level.label, seed.level)
                    assertFalse("Word $id repeated for $level on day $day", id in allWordsSeenOver7Days)
                }
                allWordsSeenOver7Days.addAll(assignment.wordIds)
                shown = updatedShown
            }
            assertEquals(21, allWordsSeenOver7Days.distinct().size)
        }
    }

    @Test
    fun speaking_assessment_marks_word_done_only_when_spoken_correctly_and_unlocks_7_day_quiz() {
        var currentDate = "2026-11-18"
        var currentEpoch = 20_776L
        var nowMs = 1_763_400_000_000L

        val mgr = LearningProgressManager(
            storageDir = tmp.root,
            clockMs = { nowMs += 1_000L; nowMs },
            todayKeyProvider = { currentDate },
            todayEpochDayProvider = { currentEpoch }
        )

        mgr.loadForUser("vocab_user")
        mgr.updateLearningPreferences(currentLevel = LearningLevel.ELEMENTARY)

        val day1Assign = mgr.state.value.dailyVocabulary!!
        assertEquals(LearningLevel.ELEMENTARY, day1Assign.level)
        assertEquals(3, day1Assign.wordIds.size)
        assertTrue(day1Assign.completedWordIds.isEmpty())

        val firstWord = CurriculumCatalog.vocabularyBank.first { it.id == day1Assign.wordIds[0] }

        // 1. Inconclusive (blank) -> NOT marked done
        val blankEval = mgr.recordVocabularyWordSpokenAssessment(firstWord, recognizedText = "")
        assertTrue(blankEval.isInconclusive)
        assertFalse(blankEval.passed)
        assertFalse(firstWord.id in mgr.state.value.dailyVocabulary!!.completedWordIds)
        assertTrue(mgr.state.value.learnedVocabularyLog.isEmpty())

        // 2. Wrong word -> NOT marked done
        val wrongEval = mgr.recordVocabularyWordSpokenAssessment(firstWord, recognizedText = "unrelated banana")
        assertFalse(wrongEval.passed)
        assertFalse(firstWord.id in mgr.state.value.dailyVocabulary!!.completedWordIds)
        assertTrue(mgr.state.value.learnedVocabularyLog.isEmpty())

        // 3. Spoken accurately -> Marked Done!
        val passEval = mgr.recordVocabularyWordSpokenAssessment(firstWord, recognizedText = firstWord.word)
        assertTrue(passEval.passed)
        assertTrue(firstWord.id in mgr.state.value.dailyVocabulary!!.completedWordIds)
        assertTrue(mgr.state.value.learnedVocabularyLog.any { it.wordId == firstWord.id })
        assertTrue(mgr.state.value.wordProgress[firstWord.id]?.isCompleted == true)

        // Check Day 1 eligibility (not unlocked until 7 days pass)
        val day1Eligibility = mgr.getWeeklyAssessmentEligibility()
        assertFalse(day1Eligibility.isUnlocked)
        assertFalse(day1Eligibility.isDueNow)
        assertEquals(6, day1Eligibility.daysRemaining)

        // Advance through Days 2..6 learning additional words each day
        val learnedIds = mutableSetOf(firstWord.id)
        for (offset in 1..6) {
            currentEpoch += 1L
            currentDate = "2026-11-${18 + offset}"
            mgr.loadForUser("vocab_user")
            val assign = mgr.state.value.dailyVocabulary!!
            assertTrue(
                "Each day brings non-repeating words",
                assign.wordIds.intersect(learnedIds).isEmpty()
            )
            val wordSeed = CurriculumCatalog.vocabularyBank.first { it.id == assign.wordIds.first() }
            val eval = mgr.recordVocabularyWordSpokenAssessment(wordSeed, recognizedText = wordSeed.word)
            assertTrue(eval.passed)
            learnedIds.add(wordSeed.id)
            if (offset == 5) {
                val day6Elig = mgr.getWeeklyAssessmentEligibility()
                assertFalse("Should not unlock on Day 6 (only 6 days completed)", day6Elig.isUnlocked)
                assertEquals(1, day6Elig.daysRemaining)
            }
        }

        // Day 7 -> 7 distinct days reached (and next day reaches 7 elapsed days), unlocked!
        val day7Eligibility = mgr.getWeeklyAssessmentEligibility()
        assertTrue("Weekly assessment should unlock after 7 days", day7Eligibility.isUnlocked)
        assertTrue("Weekly assessment should be due with learned words", day7Eligibility.isDueNow)
        assertTrue(day7Eligibility.learnedWordIdsInCycle.containsAll(learnedIds))

        val learnedSeeds = VocabularyMasteryEngine.resolveLearnedSeeds(day7Eligibility.learnedWordIdsInCycle)
        val questions = VocabularyMasteryEngine.buildWeeklyAssessmentQuestions(learnedSeeds, seed = 42L)
        assertEquals(learnedIds.size, questions.size)

        // Grade each question with spoken answers
        val perWordOutcomes = mutableMapOf<String, Boolean>()
        val passedWordIds = mutableListOf<String>()
        questions.forEach { q ->
            val spoken = when (q.mode) {
                com.example.data.learning.WeeklyQuestionMode.SPOKEN_WORD_RECALL -> q.seed.word
                com.example.data.learning.WeeklyQuestionMode.SPOKEN_SENTENCE_USAGE -> q.seed.exampleSentence
            }
            val ans = VocabularyMasteryEngine.evaluateWeeklyQuestionResponse(q, spoken)
            assertTrue("Spoken answer should pass for ${q.seed.word}: ${ans.feedback}", ans.passed)
            perWordOutcomes[q.seed.id] = true
            passedWordIds += q.seed.id
        }

        mgr.recordWeeklyVocabularyAssessment(
            WeeklyVocabularyAssessmentRecord(
                id = "wk_test_1",
                completedDateKey = currentDate,
                completedEpochDay = currentEpoch,
                cycleNumber = day7Eligibility.cycleNumber,
                assessedWordIds = questions.map { it.seed.id },
                passedWordIds = passedWordIds,
                scorePercent = 100
            ),
            wordOutcomes = perWordOutcomes
        )

        // Cycle resets cleanly after completing the 7-day quiz
        val postAssessmentEligibility = mgr.getWeeklyAssessmentEligibility()
        assertFalse(postAssessmentEligibility.isDueNow)
        assertNotNull(postAssessmentEligibility.latestAssessment)
        assertEquals(2, postAssessmentEligibility.cycleNumber)
    }

    @Test
    fun vocabularyScreen_words_tab_shows_level_daily_3_words_marks_done_on_speak_and_ai_quiz_is_speak_only() {
        val advancedWords = CurriculumCatalog.vocabularyBank
            .filter { it.level == "Advanced" }
            .take(3)
        var nextSimulatedTranscript = advancedWords.first().word
        var quizEvaluatedSentence = ""
        var weeklyCompleted = false

        composeRule.setContent {
            SpeakCraftTheme {
                VocabularyScreen(
                    entries = emptyList(),
                    todayEpochDay = 20_783L,
                    vocabEvalState = null,
                    onSpeakWord = {},
                    onReviewCard = { _, _, _ -> },
                    onEvaluateUsage = { _, _, sent -> quizEvaluatedSentence = sent },
                    onSaveCustomWord = { _, _, _, _, _ -> },
                    onClearEval = {},
                    userLevel = LearningLevel.ADVANCED,
                    dailyVocabWords = advancedWords,
                    completedDailyWordIds = emptySet(),
                    weeklyEligibility = WeeklyAssessmentEligibility(
                        isUnlocked = true,
                        isDueNow = true,
                        daysElapsed = 7,
                        distinctActiveDays = 7,
                        daysRemaining = 0,
                        cycleNumber = 1,
                        learnedWordIdsInCycle = advancedWords.map { it.id }
                    ),
                    learnedWordsForAssessment = advancedWords,
                    onCompleteWeeklyAssessment = { _, _ -> weeklyCompleted = true },
                    onStartSpeechCaptureOverride = { _, onResult, _ ->
                        onResult(LiveSpeechCaptureResult(transcript = nextSimulatedTranscript, confidence = 0.92f))
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // Daily 3 words header & level badge rendered
        composeRule.onNodeWithTag("daily_three_words_header").assertIsDisplayed()
        composeRule.onNodeWithText("Today's 3 Daily Words").assertIsDisplayed()
        composeRule.onNodeWithText("0/3 Done").assertIsDisplayed()

        // First fail with unrelated word -> NOT marked Done
        nextSimulatedTranscript = "pineapple umbrella"
        composeRule.onNodeWithTag("vocabulary_words_list")
            .performScrollToNode(hasTestTag("speak_assess_word_${advancedWords[0].id}"))
        composeRule.onNodeWithTag("speak_assess_word_${advancedWords[0].id}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_vocab_eval_${advancedWords[0].id}").assertIsDisplayed()
        composeRule.onNodeWithText("0/3 Done").assertIsDisplayed()

        // Now speak exact word correctly -> Marked Done & 1/3 Done!
        nextSimulatedTranscript = advancedWords[0].word
        composeRule.onNodeWithTag("vocabulary_words_list")
            .performScrollToNode(hasTestTag("speak_assess_word_${advancedWords[0].id}"))
        composeRule.onNodeWithTag("speak_assess_word_${advancedWords[0].id}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vocabulary_words_list")
            .performScrollToNode(hasTestTag("daily_three_words_header"))
        composeRule.onNodeWithText("1/3 Done").assertIsDisplayed()

        // Switch to AI Quiz tab -> Verify NO text box exists & speak option works
        composeRule.onNodeWithText("AI Quiz").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vocab_quiz_input").assertDoesNotExist()
        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("vocab_quiz_speak_panel"))
        composeRule.onNodeWithTag("vocab_quiz_speak_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("vocab_quiz_speak_button"))
        composeRule.onNodeWithTag("vocab_quiz_speak_button").assertIsDisplayed()

        nextSimulatedTranscript = "Executives ${advancedWords[0].word.lowercase()} complex data into clear takeaways."
        composeRule.onNodeWithTag("vocab_quiz_speak_button").performClick()
        composeRule.waitForIdle()
        assertEquals(nextSimulatedTranscript, quizEvaluatedSentence)
        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("vocab_quiz_spoken_transcript"))
        composeRule.onNodeWithTag("vocab_quiz_spoken_transcript").assertIsDisplayed()

        // Start 7-day spoken assessment on learned words
        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("weekly_assessment_unlocked_card"))
        composeRule.onNodeWithTag("weekly_assessment_unlocked_card").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("start_7day_assessment_button"))
        composeRule.onNodeWithTag("start_7day_assessment_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("weekly_question_card"))
        composeRule.onNodeWithTag("weekly_question_card").assertIsDisplayed()

        // Answer all 3 questions via speak button (sentence containing all 3 target words satisfies both recall & usage)
        nextSimulatedTranscript = "Great leaders synthesize resonant stories with subtle nuance during keynotes."
        repeat(3) {
            composeRule.onNodeWithTag("ai_quiz_list")
                .performScrollToNode(hasTestTag("weekly_quiz_speak_button"))
            composeRule.onNodeWithTag("weekly_quiz_speak_button").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("ai_quiz_list")
                .performScrollToNode(hasTestTag("weekly_quiz_next_button"))
            composeRule.onNodeWithTag("weekly_quiz_next_button").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("weekly_assessment_summary_card"))
        composeRule.onNodeWithTag("weekly_assessment_summary_card").assertIsDisplayed()
        assertTrue(weeklyCompleted)
    }

    @Test
    fun vocabularyScreen_before_7_days_shows_countdown_and_filters_words_to_user_level() {
        val beginnerWords = CurriculumCatalog.vocabularyBank.filter { it.level == "Beginner" }.take(3)
        composeRule.setContent {
            SpeakCraftTheme {
                VocabularyScreen(
                    entries = emptyList(),
                    todayEpochDay = 20_783L,
                    vocabEvalState = null,
                    onSpeakWord = {},
                    onReviewCard = { _, _, _ -> },
                    onEvaluateUsage = { _, _, _ -> },
                    onSaveCustomWord = { _, _, _, _, _ -> },
                    onClearEval = {},
                    userLevel = LearningLevel.BEGINNER,
                    dailyVocabWords = beginnerWords,
                    weeklyEligibility = WeeklyAssessmentEligibility(
                        isUnlocked = false,
                        isDueNow = false,
                        daysElapsed = 3,
                        distinctActiveDays = 3,
                        daysRemaining = 4,
                        cycleNumber = 1,
                        learnedWordIdsInCycle = beginnerWords.map { it.id }
                    ),
                    learnedWordsForAssessment = beginnerWords
                )
            }
        }
        composeRule.waitForIdle()

        // Beginner filter chip is shown & selected
        composeRule.onNodeWithTag("vocabulary_words_list")
            .performScrollToNode(hasTestTag("vocab_level_chip_beginner"))
        composeRule.onNodeWithTag("vocab_level_chip_beginner").assertIsDisplayed()

        // Switch to AI Quiz -> Countdown shown before 7 days
        composeRule.onNodeWithText("AI Quiz").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_quiz_list")
            .performScrollToNode(hasTestTag("weekly_assessment_countdown_card"))
        composeRule.onNodeWithTag("weekly_assessment_countdown_card").assertIsDisplayed()
        composeRule.onNodeWithTag("start_7day_assessment_button").assertDoesNotExist()
        composeRule.onNodeWithTag("vocab_quiz_input").assertDoesNotExist()
    }
}
