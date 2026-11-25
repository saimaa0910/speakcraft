package com.example.data.learningpaths

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.activity.LearningPathsEngine
import com.example.data.learning.LearningGoal
import com.example.data.learning.LearningLevel
import com.example.data.model.CurriculumCatalog
import com.example.data.model.ProgramEnrollment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LearningPathsCurriculumAndEngineTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun all_7_structured_programs_match_exact_ids_titles_and_durations() {
        val programs = LearningPathCurriculumRegistry.allPrograms
        assertEquals(7, programs.size)
        assertEquals(7, CurriculumCatalog.structuredPrograms.size)

        val expectedDurations = mapOf(
            "30_day_challenge" to 30,
            "60_day_fluency" to 60,
            "90_day_advanced" to 90,
            "vocab_challenge" to 21,
            "pronunciation_program" to 28,
            "public_speaking_program" to 30,
            "interview_program" to 21
        )
        assertEquals(280, programs.sumOf { p -> p.modules.sumOf { it.lessons.size } })
        assertEquals(28, programs.sumOf { it.modules.size })
        assertEquals(7, programs.sumOf { p -> p.modules.count { it.assessment.isCapstone } })

        for ((id, days) in expectedDurations) {
            val program = LearningPathCurriculumRegistry.getProgram(id)
            assertNotNull("Missing program $id", program)
            assertEquals(days, program!!.durationDays)
            val allLessons = program.modules.flatMap { it.lessons }
            assertEquals("Expected exact $days individual lessons in $id", days, allLessons.size)
            assertEquals((1..days).toList(), allLessons.map { it.dayNumber })

            val compat = CurriculumCatalog.structuredPrograms.first { it.id == id }
            assertEquals(days, compat.weeks.sumOf { it.dailyDrills.size })
        }
    }

    @Test
    fun curriculum_integrity_passes_activity_links_quiz_bounds_and_branding_blocklist() {
        val integrityErrors = LearningPathCurriculumRegistry.validateIntegrity()
        assertTrue(
            "Curriculum integrity violations: ${integrityErrors.joinToString("; ")}",
            integrityErrors.isEmpty()
        )
        val forbiddenTerms = listOf(
            "askvinh", "vinh", "giang", "youtubeintelligence",
            "3vs", "3 vs", "three vs",
            "5 vocal foundations", "five vocal foundations",
            "clear framework", "4-color communicator", "four-color communicator", "4 color communicator",
            "power zone", "4 ways of listening", "four ways of listening",
            "high low buffalo", "3-2-1 conversation", "relive vs report", "relive versus report",
            "friendly authority", "3-stage audit", "3-stage self-awareness audit", "three-stage audit"
        )
        for (program in LearningPathCurriculumRegistry.allPrograms) {
            val allText = LearningPathCurriculumRegistry.collectProgramDisplayText(program).lowercase()
            for (term in forbiddenTerms) {
                val pattern = Regex("""(?<!\w)${Regex.escape(term)}(?!\w)""", RegexOption.IGNORE_CASE)
                assertFalse(
                    "${program.programId} contains forbidden branding/blocklist term '$term'",
                    pattern.containsMatchIn(allText)
                )
            }
        }
        // Preserve untouched 10-path Activity Engine subsystem contract
        assertEquals(10, LearningPathsEngine.allPaths.size)
    }

    @Test
    fun evaluation_engine_rejects_empty_submissions_and_gives_evidence_based_feedback() {
        val lesson = LearningPathCurriculumRegistry.getLesson("30_day_challenge", 6)!!

        val emptyFeedback = LearningPathEvaluationEngine.evaluateLesson(
            lesson = lesson,
            request = LessonSubmissionRequest(
                programId = "30_day_challenge",
                dayNumber = 6,
                spokenOrWrittenText = "hi",
                confidenceSelfRating = 5
            )
        )
        assertFalse(emptyFeedback.passed)
        assertTrue(emptyFeedback.overallScore <= 25)
        assertTrue(emptyFeedback.mistakesFound.isNotEmpty())

        val correctQuizId = lesson.quizQuestions.first().questionId
        val correctQuizOpt = lesson.quizQuestions.first().correctIndex

        // Verify confidence rating stays completely separate from objective score
        val strongLowConfidence = LearningPathEvaluationEngine.evaluateLesson(
            lesson = lesson,
            request = LessonSubmissionRequest(
                programId = "30_day_challenge",
                dayNumber = 6,
                spokenOrWrittenText = "Um I adjusted our morning sync yesterday because pausing cleanly before key points gives listeners far better clarity instead of rushing.",
                quizAnswers = mapOf(correctQuizId to correctQuizOpt),
                completedChecklistIndices = setOf(0, 1, 2),
                confidenceSelfRating = 1
            )
        )
        val strongHighConfidence = LearningPathEvaluationEngine.evaluateLesson(
            lesson = lesson,
            request = LessonSubmissionRequest(
                programId = "30_day_challenge",
                dayNumber = 6,
                spokenOrWrittenText = "Um I adjusted our morning sync yesterday because pausing cleanly before key points gives listeners far better clarity instead of rushing.",
                quizAnswers = mapOf(correctQuizId to correctQuizOpt),
                completedChecklistIndices = setOf(0, 1, 2),
                confidenceSelfRating = 5
            )
        )

        assertEquals(strongLowConfidence.overallScore, strongHighConfidence.overallScore)
        assertEquals(1, strongLowConfidence.confidenceSelfRating)
        assertEquals(5, strongHighConfidence.confidenceSelfRating)
        assertTrue(strongHighConfidence.passed)
        assertTrue("Expected filler warning inline highlight on 'Um'", strongHighConfidence.transcriptInsights.isNotEmpty())
        assertTrue(strongHighConfidence.whatWentWell.isNotEmpty())
        assertTrue(strongHighConfidence.rubricBreakdown.isNotEmpty())
    }

    @Test
    fun module_assessment_evaluates_scenario_quiz_and_practical_performance_together() {
        val mod = LearningPathCurriculumRegistry.getModule("interview_program", "interview_program_mod_3")!!
        val spec = mod.assessment

        val rightAnswers = spec.scenarioQuestions.associate { it.questionId to it.correctIndex }
        val result = LearningPathEvaluationEngine.evaluateModuleAssessment(
            spec = spec,
            submission = ModuleAssessmentSubmission(
                programId = "interview_program",
                moduleId = mod.moduleId,
                scenarioAnswers = rightAnswers,
                practicalResponseText = "Over seven years building mobile tools, my strength centers on turning ambiguous reliability gaps into quantified clear gains. Previously I streamlined offline verification pipelines, cutting user setup errors by forty percent cleanly while aligning engineering and design teams diplomatically around shared weekly priorities.",
                confidenceSelfRating = 4
            )
        )

        assertEquals(100, result.quizScorePercent)
        assertTrue("Expected capstone pass for complete structured response, got ${result.combinedScore}", result.passed)
        assertTrue(result.combinedScore >= spec.passingScoreThreshold)
    }

    @Test
    fun recommendation_engine_ranks_by_goal_level_and_weakest_skill() {
        val programs = LearningPathCurriculumRegistry.allPrograms
        val emptyState = LearningPathsUserState(userId = "u1")

        val pronRecs = LearningPathRecommendationEngine.recommendPrograms(
            programs = programs,
            userLevel = LearningLevel.INTERMEDIATE,
            userGoal = LearningGoal.PRONUNCIATION,
            userState = emptyState
        )
        assertEquals("pronunciation_program", pronRecs.first().program.programId)

        val vocabRecs = LearningPathRecommendationEngine.recommendPrograms(
            programs = programs,
            userLevel = LearningLevel.BEGINNER,
            userGoal = LearningGoal.VOCABULARY,
            userState = emptyState
        )
        assertEquals("vocab_challenge", vocabRecs.first().program.programId)

        // Program completion requires BOTH all lessons AND all module assessments passed
        val vocabProgram = LearningPathCurriculumRegistry.getProgram("vocab_challenge")!!
        val daysOnlyDone = PathEnrollmentRecord(
            programId = "vocab_challenge",
            programTitle = vocabProgram.title,
            completedDays = (1..21).toList()
        )
        val incompleteStats = LearningPathRecommendationEngine.computeProgramStats(vocabProgram, daysOnlyDone)
        assertFalse("Days without assessments must NOT complete program", incompleteStats.programCompleted)

        val fullAssessments = vocabProgram.modules.associate { mod ->
            mod.moduleId to ModuleAssessmentRecord(
                assessmentId = mod.assessment.assessmentId,
                moduleId = mod.moduleId,
                isCapstone = mod.assessment.isCapstone,
                passed = true,
                bestCombinedScore = 88
            )
        }
        val completedRecord = daysOnlyDone.copy(moduleAssessments = fullAssessments)
        val completedStats = LearningPathRecommendationEngine.computeProgramStats(vocabProgram, completedRecord)
        assertTrue(completedStats.programCompleted)
        assertTrue(completedStats.capstonePassed)
    }

    @Test
    fun persistence_repository_supports_offline_lifecycle_reload_remote_merge_and_concurrency() {
        runBlocking {
        val uid = "test_lp_user_${System.currentTimeMillis()}"
        val repo = LearningPathPersistenceRepository(context)
        repo.clearUserData(uid)

        val prog = LearningPathCurriculumRegistry.getProgram("vocab_challenge")!!
        val day1 = LearningPathCurriculumRegistry.getLesson("vocab_challenge", 1)!!
        repo.enroll(uid, prog)

        val req = LessonSubmissionRequest(
            programId = prog.programId,
            dayNumber = 1,
            spokenOrWrittenText = "Your summary was remarkably thorough and delivered actionable steps clearly across the squad.",
            quizAnswers = mapOf(day1.quizQuestions.first().questionId to day1.quizQuestions.first().correctIndex),
            confidenceSelfRating = 4,
            reflectionNote = "Swapped 'good' out for 'thorough' and 'actionable'."
        )
        val fb = LearningPathEvaluationEngine.evaluateLesson(day1, req)
        repo.recordLessonSubmission(uid, prog, day1, req, fb)

        // Concurrent saves + pause toggle stability check
        (2..5).map { d ->
            async(Dispatchers.IO) {
                repo.toggleDayCompletionManual(uid, prog, d)
            }
        }.awaitAll()

        repo.setPaused(uid, prog.programId, true)

        // New repository instance reads persisted JSON cold offline
        val freshRepo = LearningPathPersistenceRepository(context)
        val loaded = freshRepo.loadUserState(uid)
        val rec = loaded.enrollments[prog.programId]
        assertNotNull(rec)
        assertTrue(rec!!.paused)
        assertTrue(rec.completedDays.containsAll(listOf(1, 2, 3, 4, 5)))
        assertEquals("Swapped 'good' out for 'thorough' and 'actionable'.", rec.dayRecords[1]?.reflectionNote)
        assertTrue((rec.dayRecords[1]?.bestScore ?: 0) >= 60)

        // Remote merge unions remote completed days without discarding local scores
        val merged = freshRepo.mergeRemoteEnrollments(
            userId = uid,
            remoteEnrollments = listOf(
                ProgramEnrollment(
                    userId = uid,
                    programId = "vocab_challenge",
                    programTitle = prog.title,
                    completedDays = listOf(1L, 6L, 7L),
                    active = true
                )
            )
        )
        val mergedDays = merged.enrollments["vocab_challenge"]!!.completedDays
        assertTrue(mergedDays.containsAll(listOf(1, 2, 3, 4, 5, 6, 7)))
        assertEquals("Swapped 'good' out for 'thorough' and 'actionable'.", merged.enrollments["vocab_challenge"]!!.dayRecords[1]?.reflectionNote)

        freshRepo.unenroll(uid, prog.programId)
        val afterLeave = freshRepo.loadUserState(uid)
        assertFalse(afterLeave.enrollments.containsKey(prog.programId))

        freshRepo.clearUserData(uid)
        }
    }
}
