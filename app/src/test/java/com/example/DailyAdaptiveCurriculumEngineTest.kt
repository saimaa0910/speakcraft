package com.example

import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.ActivityDifficulty
import com.example.data.learning.DailyAdaptiveCurriculumEngine
import com.example.data.learning.LearningLevel
import com.example.data.model.CurriculumCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAdaptiveCurriculumEngineTest {

    @Test
    fun level_sentence_bank_covers_all_5_learning_levels() {
        val bank = DailyAdaptiveCurriculumEngine.levelSentenceBank
        assertEquals(5, bank.size)

        LearningLevel.entries.forEach { level ->
            val sentences = bank[level]
            assertNotNull("Sentence bank must contain sentences for level $level", sentences)
            assertTrue("Expected at least 3 sentences for level $level", sentences!!.size >= 3)

            sentences.forEach { sentence ->
                assertEquals(level, sentence.level)
                assertTrue("Sentence text must not be blank", sentence.text.isNotBlank())
                assertTrue("Focus word/topic must not be blank", sentence.focusWordOrTopic.isNotBlank())
                assertTrue("Grammar focus must not be blank", sentence.grammarFocus.isNotBlank())
                assertTrue("Speaking tip must not be blank", sentence.speakingTip.isNotBlank())
            }
        }
    }

    @Test
    fun level_mission_bank_covers_all_5_learning_levels() {
        val bank = DailyAdaptiveCurriculumEngine.levelMissionBank
        assertEquals(5, bank.size)

        LearningLevel.entries.forEach { level ->
            val missions = bank[level]
            assertNotNull("Mission bank must contain missions for level $level", missions)
            assertTrue("Expected at least 3 missions for level $level", missions!!.size >= 3)

            missions.forEach { mission ->
                assertEquals(level, mission.level)
                assertTrue("Mission title must not be blank", mission.title.isNotBlank())
                assertTrue("Mission description must not be blank", mission.description.isNotBlank())
                assertTrue("Mission category must not be blank", mission.contextCategory.isNotBlank())
                assertTrue("Estimated minutes must be positive", mission.estimatedMinutes > 0)
            }
        }
    }

    @Test
    fun daily_words_rotate_deterministically_by_dateKey_and_match_user_level() {
        val date1 = "2026-11-24"
        val date2 = "2026-11-25"

        // Beginner calibration
        val begWords1 = DailyAdaptiveCurriculumEngine.getDailyWords(date1, LearningLevel.BEGINNER, count = 3)
        val begWords2 = DailyAdaptiveCurriculumEngine.getDailyWords(date2, LearningLevel.BEGINNER, count = 3)
        assertEquals(3, begWords1.size)
        assertEquals(3, begWords2.size)

        // All returned words for Beginner are from beginner/elementary vocabulary bank
        begWords1.forEach { w ->
            assertTrue("Expected beginner-friendly word level for ${w.word}", w.level == "Beginner" || w.level == "Elementary")
        }

        // Advanced calibration
        val advWords1 = DailyAdaptiveCurriculumEngine.getDailyWords(date1, LearningLevel.ADVANCED, count = 3)
        val advWords2 = DailyAdaptiveCurriculumEngine.getDailyWords(date2, LearningLevel.ADVANCED, count = 3)
        assertEquals(3, advWords1.size)
        assertEquals(3, advWords2.size)

        advWords1.forEach { w ->
            assertTrue("Expected advanced/upper-intermediate word level for ${w.word}", w.level == "Advanced" || w.level == "Upper Intermediate")
        }

        // Words differ across different levels on the same day
        val begWordNames = begWords1.map { it.word }.toSet()
        val advWordNames = advWords1.map { it.word }.toSet()
        val overlap = begWordNames.intersect(advWordNames)
        assertTrue("Beginner and Advanced words should not overlap completely", overlap.size < 3)

        // Words rotate across consecutive dates
        val wordsDate1 = begWords1.map { it.word }
        val wordsDate2 = begWords2.map { it.word }
        assertNotEquals("Daily words should rotate across different dates", wordsDate1, wordsDate2)
    }

    @Test
    fun daily_sentences_rotate_deterministically_by_dateKey_and_match_user_level() {
        val date1 = "2026-11-24"
        val date2 = "2026-11-25"

        val begSent1 = DailyAdaptiveCurriculumEngine.getDailySentences(date1, LearningLevel.BEGINNER, count = 3)
        val advSent1 = DailyAdaptiveCurriculumEngine.getDailySentences(date1, LearningLevel.ADVANCED, count = 3)

        assertEquals(3, begSent1.size)
        assertEquals(3, advSent1.size)

        begSent1.forEach { assertEquals(LearningLevel.BEGINNER, it.level) }
        advSent1.forEach { assertEquals(LearningLevel.ADVANCED, it.level) }

        // Sentences rotate across different dates
        val begSent2 = DailyAdaptiveCurriculumEngine.getDailySentences(date2, LearningLevel.BEGINNER, count = 3)
        val sents1Ids = begSent1.map { it.id }
        val sents2Ids = begSent2.map { it.id }
        assertNotEquals("Daily sentences should rotate on different days", sents1Ids, sents2Ids)
    }

    @Test
    fun daily_activities_match_user_level_and_rotate_daily() {
        val date1 = "2026-11-24"
        val date2 = "2026-11-25"

        val begActivities = DailyAdaptiveCurriculumEngine.getDailyActivities(date1, LearningLevel.BEGINNER, count = 3)
        val advActivities = DailyAdaptiveCurriculumEngine.getDailyActivities(date1, LearningLevel.ADVANCED, count = 3)

        assertEquals(3, begActivities.size)
        assertEquals(3, advActivities.size)

        // Beginner activities should be BEGINNER or INTERMEDIATE difficulty
        begActivities.forEach { item ->
            assertNotNull(ActivityCatalogue.getActivity(item.activity.activityId))
            assertTrue(
                "Beginner activities should not be ADVANCED difficulty, got ${item.activity.difficulty}",
                item.activity.difficulty != ActivityDifficulty.ADVANCED
            )
            assertTrue("Duration should be positive", item.durationMinutes > 0)
            assertTrue("Reason should not be blank", item.reason.isNotBlank())
        }

        // Advanced activities should prioritize INTERMEDIATE or ADVANCED
        advActivities.forEach { item ->
            assertNotNull(ActivityCatalogue.getActivity(item.activity.activityId))
            assertTrue("Duration should be positive", item.durationMinutes > 0)
        }

        // Activities rotate across different dates
        val begActivities2 = DailyAdaptiveCurriculumEngine.getDailyActivities(date2, LearningLevel.BEGINNER, count = 3)
        val actIds1 = begActivities.map { it.activity.activityId }
        val actIds2 = begActivities2.map { it.activity.activityId }
        assertNotEquals("Activities should rotate across consecutive dates", actIds1, actIds2)
    }

    @Test
    fun daily_technique_and_mission_rotate_and_match_level() {
        val date1 = "2026-11-24"
        val date2 = "2026-11-25"

        val tech1 = DailyAdaptiveCurriculumEngine.getDailyTechnique(date1, LearningLevel.INTERMEDIATE)
        val tech2 = DailyAdaptiveCurriculumEngine.getDailyTechnique(date2, LearningLevel.INTERMEDIATE)

        assertTrue(tech1.title.isNotBlank())
        assertTrue(tech1.coreRule.isNotBlank())
        assertTrue(tech1.suggestedDailyAction.isNotBlank())
        assertNotEquals("Technique should rotate across days", tech1.techniqueId, tech2.techniqueId)

        val missionBeg = DailyAdaptiveCurriculumEngine.getDailyMission(date1, LearningLevel.BEGINNER)
        val missionAdv = DailyAdaptiveCurriculumEngine.getDailyMission(date1, LearningLevel.ADVANCED)
        assertEquals(LearningLevel.BEGINNER, missionBeg.level)
        assertEquals(LearningLevel.ADVANCED, missionAdv.level)
        assertNotEquals("Missions should differ for different levels", missionBeg.id, missionAdv.id)
    }

    @Test
    fun generate_daily_package_creates_complete_curriculum_package_with_no_duplicates() {
        val dateKey = "2026-11-24"
        val pkg = DailyAdaptiveCurriculumEngine.generateDailyPackage(
            dateKey = dateKey,
            level = LearningLevel.INTERMEDIATE,
            targetMinutes = 25
        )

        assertEquals(dateKey, pkg.dateKey)
        assertEquals(LearningLevel.INTERMEDIATE, pkg.userLevel)
        assertTrue(pkg.headlineTheme.isNotBlank())
        assertEquals(25, pkg.targetMinutes)

        // 3 words, all unique
        assertEquals(3, pkg.dailyWords.size)
        val uniqueWords = pkg.dailyWords.map { it.word }.toSet()
        assertEquals(3, uniqueWords.size)

        // 3 sentences, all unique
        assertEquals(3, pkg.dailySentences.size)
        val uniqueSentences = pkg.dailySentences.map { it.id }.toSet()
        assertEquals(3, uniqueSentences.size)

        // 3 activities, all unique
        assertEquals(3, pkg.dailyActivities.size)
        val uniqueActivities = pkg.dailyActivities.map { it.activity.activityId }.toSet()
        assertEquals(3, uniqueActivities.size)

        // Technique & mission present
        assertNotNull(pkg.dailyTechnique)
        assertNotNull(pkg.dailyMission)

        // Tasks checklist populated
        assertTrue("Package tasks checklist should contain multiple items", pkg.dailyTasks.size >= 5)
        val taskCategories = pkg.dailyTasks.map { it.category }
        assertTrue("Tasks must contain Vocabulary", taskCategories.contains("Vocabulary"))
        assertTrue("Tasks must contain Speaking", taskCategories.contains("Speaking"))
        assertTrue("Tasks must contain Technique", taskCategories.contains("Technique"))
        assertTrue("Tasks must contain Mission", taskCategories.contains("Mission"))
    }

    @Test
    fun learning_level_from_label_parses_all_strings_gracefully() {
        assertEquals(LearningLevel.BEGINNER, LearningLevel.fromLabel("Beginner"))
        assertEquals(LearningLevel.BEGINNER, LearningLevel.fromLabel("beginner"))
        assertEquals(LearningLevel.ELEMENTARY, LearningLevel.fromLabel("Elementary"))
        assertEquals(LearningLevel.INTERMEDIATE, LearningLevel.fromLabel("Intermediate"))
        assertEquals(LearningLevel.UPPER_INTERMEDIATE, LearningLevel.fromLabel("Upper Intermediate"))
        assertEquals(LearningLevel.ADVANCED, LearningLevel.fromLabel("Advanced"))
        assertEquals(LearningLevel.ADVANCED, LearningLevel.fromLabel("advanced"))

        // Graceful fallback for unknown string
        assertEquals(LearningLevel.INTERMEDIATE, LearningLevel.fromLabel("Master"))
        assertEquals(LearningLevel.INTERMEDIATE, LearningLevel.fromLabel(""))
    }
}
