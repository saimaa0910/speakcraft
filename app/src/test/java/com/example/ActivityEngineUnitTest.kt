package com.example

import com.example.data.activity.ActivityAttemptRecord
import com.example.data.activity.ActivityCatalogue
import com.example.data.activity.ActivityDifficulty
import com.example.data.activity.ActivityDurationFilter
import com.example.data.activity.ActivityEngine
import com.example.data.activity.ActivityProgressAnalyticsEngine
import com.example.data.activity.ActivitySearchFilter
import com.example.data.activity.ActivitySkillCategory
import com.example.data.activity.ActivityUserResponse
import com.example.data.activity.ActivityValidator
import com.example.data.activity.DailyPracticeEngine
import com.example.data.activity.EvaluationStrategyType
import com.example.data.activity.ExerciseTemplateType
import com.example.data.activity.LearningPathStatus
import com.example.data.activity.LearningPathsEngine
import com.example.data.activity.LoadedActivityResult
import com.example.data.activity.PathTaskCategory
import com.example.data.activity.ScoringRubricDimension
import com.example.data.activity.UniversalEvaluationEngine
import com.example.data.activity.UserActivityProgress
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

class ActivityEngineUnitTest {

    @Test
    fun catalogue_contains_exact_10_modules_and_150_valid_core_activities() {
        assertEquals(10, ActivityCatalogue.modules.size)
        assertEquals(150, ActivityCatalogue.allActivities.size)

        val expectedModuleCounts = mapOf(
            "mod_01_english_fluency" to 15,
            "mod_02_pronunciation_articulation" to 15,
            "mod_03_voice_mastery" to 15,
            "mod_04_speaking_clarity" to 15,
            "mod_05_conversation_mastery" to 15,
            "mod_06_active_listening" to 15,
            "mod_07_storytelling" to 15,
            "mod_08_body_language" to 10,
            "mod_09_public_speaking" to 15,
            "mod_10_professional_communication" to 20
        )

        expectedModuleCounts.forEach { (moduleId, expectedCount) ->
            val actualCount = ActivityCatalogue.getActivitiesForModule(moduleId).size
            assertEquals("Unexpected activity count for $moduleId", expectedCount, actualCount)
        }

        // All activity IDs are unique
        val uniqueIds = ActivityCatalogue.allActivities.map { it.activityId }.toSet()
        assertEquals(150, uniqueIds.size)

        // All 150 activities pass pre-render validation with 0 errors
        val validation = ActivityValidator.validateCatalogue(
            ActivityCatalogue.modules,
            ActivityCatalogue.allActivities
        )
        assertTrue(
            "Catalogue validation failed: ${validation.errors.joinToString("; ")}",
            validation.valid
        )
    }

    @Test
    fun catalogue_exercises_all_25_reusable_exercise_templates_and_valid_next_ids() {
        assertEquals(25, ExerciseTemplateType.entries.size)
        val usedTemplates = ActivityCatalogue.allActivities.map { it.exerciseType }.toSet()
        val missingTemplates = ExerciseTemplateType.entries.filterNot { it in usedTemplates }
        assertTrue(
            "All 25 exercise templates must be represented in the catalogue. Missing: $missingTemplates",
            missingTemplates.isEmpty()
        )

        val allIds = ActivityCatalogue.allActivities.map { it.activityId }.toSet()
        ActivityCatalogue.allActivities.forEach { act ->
            act.nextActivityIds.forEach { nextId ->
                assertTrue(
                    "Activity ${act.activityId} points to unknown nextActivityId $nextId",
                    nextId in allIds
                )
            }
        }
    }

    @Test
    fun validator_and_engine_safely_reject_invalid_activities_without_crashing() {
        val engine = ActivityEngine(
            evaluationEngine = UniversalEvaluationEngine(aiClientOverride = { _, _, _, _ -> null })
        )
        val missing = engine.loadActivity("non_existent_activity_999")
        assertTrue(missing is LoadedActivityResult.Invalid)
        assertTrue((missing as LoadedActivityResult.Invalid).errors.isNotEmpty())

        val validBase = ActivityCatalogue.allActivities.first()
        val invalidDef = validBase.copy(
            activityId = "",
            title = "",
            duration = 0,
            instructions = emptyList(),
            scoringRubric = listOf(ScoringRubricDimension("only", "Only", 50, "Bad weight sum"))
        )
        val validation = ActivityValidator.validate(invalidDef)
        assertFalse(validation.valid)
        assertTrue(validation.errors.size >= 4)
    }

    @Test
    fun search_multi_filter_and_pagination_work_accurately() {
        val searchFord = ActivityCatalogue.searchAndFilter(ActivitySearchFilter(query = "FORD"))
        assertTrue(searchFord.any { it.activityId == "m05_ford_framework" })

        val module7 = ActivityCatalogue.searchAndFilter(
            ActivitySearchFilter(moduleId = "mod_07_storytelling", difficulty = ActivityDifficulty.INTERMEDIATE)
        )
        assertTrue(module7.isNotEmpty())
        assertTrue(module7.all { it.moduleId == "mod_07_storytelling" && it.difficulty == ActivityDifficulty.INTERMEDIATE })

        val quickDrills = ActivityCatalogue.searchAndFilter(
            ActivitySearchFilter(durationFilter = ActivityDurationFilter.QUICK)
        )
        assertTrue(quickDrills.isNotEmpty())
        assertTrue(quickDrills.all { it.duration <= 5 })

        val page1 = ActivityCatalogue.paginate(ActivityCatalogue.allActivities, page = 1, pageSize = 20)
        val page2 = ActivityCatalogue.paginate(ActivityCatalogue.allActivities, page = 2, pageSize = 20)
        assertEquals(20, page1.size)
        assertEquals(40, page2.size)
    }

    @Test
    fun universal_evaluation_engine_registers_all_9_evaluators_and_keeps_confidence_separate() = runTest {
        val evalEngine = UniversalEvaluationEngine(aiClientOverride = { _, _, _, _ -> null })
        assertEquals(9, EvaluationStrategyType.entries.size)
        assertEquals(9, evalEngine.allRegisteredStrategies().size)
        EvaluationStrategyType.entries.forEach { strategy ->
            assertEquals(strategy, evalEngine.strategyFor(strategy).strategyType)
        }

        // Pronunciation activity gets honest acoustic caveat & all 6 feedback sections
        val pronActivity = ActivityCatalogue.getActivityById("m02_tongue_twisters")!!
        val responseLowConf = ActivityUserResponse(
            activityId = pronActivity.activityId,
            textResponse = "Red leather yellow leather spoken slowly with crisp stops and clear final consonants across three clean rounds.",
            durationSeconds = 90,
            confidenceSelfRating = 25
        )
        val responseHighConf = responseLowConf.copy(confidenceSelfRating = 95)

        val evalLow = evalEngine.evaluate(pronActivity, responseLowConf)
        val evalHigh = evalEngine.evaluate(pronActivity, responseHighConf)

        // Objective score MUST NOT be altered by self-assessed confidence rating
        assertEquals(evalLow.overallObjectiveScore, evalHigh.overallObjectiveScore)
        assertEquals(25, evalLow.confidenceSelfRating)
        assertEquals(95, evalHigh.confidenceSelfRating)

        // All 6 mandatory feedback sections must be populated
        assertTrue(evalLow.whatWentWell.isNotEmpty())
        assertTrue(evalLow.specificMistakes.isNotEmpty())
        assertTrue(evalLow.correctedExamples.isNotEmpty())
        assertTrue(evalLow.explanation.isNotBlank())
        assertTrue(evalLow.improvementSuggestions.isNotEmpty())
        assertTrue(evalLow.nextPracticeTask.isNotBlank())
        assertTrue(evalLow.honestyNotice.contains("phoneme", ignoreCase = true))
        assertEquals("PronunciationEvaluator", evalLow.evaluatorName)
    }

    @Test
    fun activity_engine_end_to_end_submission_and_next_recommendations() = runTest {
        val engine = ActivityEngine(
            evaluationEngine = UniversalEvaluationEngine(aiClientOverride = { _, _, _, _ -> null })
        )
        val activity = ActivityCatalogue.getActivityById("m04_ccc_framework")!!
        val response = ActivityUserResponse(
            activityId = activity.activityId,
            textResponse = "Context: Our checkout latency rose 18 percent after Monday's release. Core message: We must roll back the caching layer today. Call to action: Approve the 2pm hotfix window so engineers can restore baseline speed.",
            durationSeconds = 120,
            confidenceSelfRating = 80
        )

        val validationError = engine.validateUserSubmission(activity, response)
        assertEquals(null, validationError)

        val outcome = engine.evaluateAndBuildOutcome(
            userId = "user_test_1",
            activity = activity,
            response = response,
            existingProgress = null,
            allProgress = emptyList(),
            recentAttempts = emptyList()
        )
        assertTrue(outcome.evaluation.overallObjectiveScore in 1..100)
        assertEquals(1L, outcome.updatedProgress.attemptsCount)
        assertEquals(activity.activityId, outcome.attemptRecord.activityId)
        assertEquals(80L, outcome.attemptRecord.confidenceSelfRating)
        assertTrue(outcome.recommendedNextActivities.isNotEmpty())
        assertTrue(outcome.recommendedNextActivities.none { it.activityId == activity.activityId })
    }

    @Test
    fun daily_practice_engine_generates_10_20_30_45_60_min_plans_avoids_repeats_and_supports_swap() {
        val profile = UserProfile(
            communicationLevel = "Intermediate",
            fluencyScore = 58,
            pronunciationScore = 52,
            vocabularyScore = 64,
            listeningScore = 60
        )
        val recentAttempt = ActivityAttemptRecord(
            activityId = "m02_pronunciation_drills",
            moduleId = "mod_02_pronunciation_articulation",
            skillCategory = ActivitySkillCategory.PRONUNCIATION.name,
            exerciseType = ExerciseTemplateType.PRONUNCIATION_DRILL.name
        )

        listOf(10, 20, 30, 45, 60).forEach { targetMins ->
            val plan = DailyPracticeEngine.generatePlan(
                dateKey = "2026-11-19",
                targetMinutes = targetMins,
                profile = profile,
                progressList = emptyList(),
                recentAttempts = listOf(recentAttempt)
            )
            assertEquals(targetMins, plan.targetMinutes)
            assertTrue("Expected non-empty plan for $targetMins min", plan.blocks.isNotEmpty())
            val uniquePlanIds = plan.blocks.map { it.activityId }.toSet()
            assertEquals("Duplicate activity inside $targetMins min plan", plan.blocks.size, uniquePlanIds.size)
            assertFalse(
                "Plan should avoid recently attempted activity when alternatives exist",
                "m02_pronunciation_drills" in uniquePlanIds
            )
        }

        // Customize plan by swapping block 0
        val plan20 = DailyPracticeEngine.generatePlan("2026-11-19", 20, profile, emptyList(), emptyList())
        val originalFirstId = plan20.blocks.first().activityId
        val swapped = DailyPracticeEngine.swapBlockInPlan(plan20, originalFirstId, emptyList())
        assertEquals(plan20.blocks.size, swapped.blocks.size)
        assertNotEquals(originalFirstId, swapped.blocks.first().activityId)
    }

    @Test
    fun learning_paths_engine_has_10_valid_paths_with_milestones_and_prerequisites() {
        assertEquals(10, LearningPathsEngine.allPaths.size)
        val allIds = ActivityCatalogue.allActivities.map { it.activityId }.toSet()

        LearningPathsEngine.allPaths.forEach { path ->
            assertTrue("Path ${path.pathId} must have ordered activities", path.orderedActivityIds.size >= 8)
            assertTrue("Path ${path.pathId} must define milestones", path.milestones.isNotEmpty())
            path.orderedActivityIds.forEach { id ->
                assertTrue("Path ${path.pathId} references unknown activity $id", id in allIds)
            }
        }

        val beginnerPath = LearningPathsEngine.allPaths.first { it.pathId == "path_01_beginner_english_fluency" }
        val completedFirst4 = beginnerPath.orderedActivityIds.take(4).map { id ->
            UserActivityProgress(activityId = id, completed = true, bestObjectiveScore = 82)
        }
        val summary = LearningPathsEngine.evaluatePathProgress(beginnerPath, completedFirst4)
        assertEquals(4, summary.completedCount)
        assertEquals(beginnerPath.orderedActivityIds.size, summary.totalCount)
        assertEquals(beginnerPath.orderedActivityIds[4], summary.nextRecommendedActivityId)
        assertTrue(summary.unlockedMilestones.isNotEmpty())
    }

    @Test
    fun learning_paths_engine_computes_all_4_pillars_activity_exercise_techniques_tasks() {
        val beginnerPath = LearningPathsEngine.allPaths.first { it.pathId == "path_01_beginner_english_fluency" }
        val completedFirst4 = beginnerPath.orderedActivityIds.take(4).map { id ->
            UserActivityProgress(
                activityId = id,
                completed = true,
                bestObjectiveScore = 85,
                latestObjectiveScore = 85,
                attemptsCount = 2,
                totalPracticeSeconds = 300
            )
        }

        val summary = LearningPathsEngine.evaluatePathProgress(beginnerPath, completedFirst4)

        // Pillar 1: Activity Progress
        assertEquals(4, summary.activityProgress.completedActivities)
        assertEquals(12, summary.activityProgress.totalActivities)
        assertEquals(33, summary.activityProgress.percentComplete)
        assertEquals(85, summary.activityProgress.averageScore)
        assertEquals(85, summary.activityProgress.bestScore)
        assertEquals(8, summary.activityProgress.totalAttempts)
        assertEquals(20, summary.activityProgress.totalPracticeMinutes)
        assertEquals(12, summary.activityProgress.activities.size)
        assertTrue(summary.activityProgress.activities[0].isCompleted)
        assertFalse(summary.activityProgress.activities[4].isCompleted)

        // Pillar 2: Exercise Progress
        assertEquals(12, summary.exerciseProgress.totalExercises)
        assertEquals(4, summary.exerciseProgress.completedExercises)
        assertEquals(33, summary.exerciseProgress.percentComplete)
        assertTrue(summary.exerciseProgress.distinctExerciseTypesCount > 0)
        assertTrue(summary.exerciseProgress.exerciseTypeBreakdown.isNotEmpty())
        summary.exerciseProgress.exerciseTypeBreakdown.forEach { ex ->
            assertNotNull(ex.typeName)
            assertNotNull(ex.family)
            assertTrue(ex.totalCount >= 1)
        }

        // Pillar 3: Technique Progress
        assertTrue("Path must track core techniques", summary.techniqueProgress.totalTechniques >= 3)
        assertTrue("Practiced techniques must be counted", summary.techniqueProgress.practicedTechniques >= 1)
        assertTrue(summary.techniqueProgress.techniques.isNotEmpty())
        summary.techniqueProgress.techniques.forEach { tech ->
            assertTrue(tech.title.isNotBlank())
            assertTrue(tech.summary.isNotBlank())
            assertTrue(tech.coreRule.isNotBlank())
            assertTrue(tech.associatedActivityIds.isNotEmpty())
        }

        // Pillar 4: Task Progress
        assertTrue("Path must define comprehensive tasks", summary.taskProgress.totalTasks > 12)
        assertTrue("Completed tasks must include completed activities", summary.taskProgress.completedTasks >= 4)
        assertTrue(summary.taskProgress.tasksByCategory.containsKey(PathTaskCategory.ACTIVITY_TASK))
        assertTrue(summary.taskProgress.tasksByCategory.containsKey(PathTaskCategory.MILESTONE_CHECKPOINT))
        assertTrue(summary.taskProgress.tasksByCategory.containsKey(PathTaskCategory.REAL_WORLD_APPLICATION))

        // Overall Weighted Metric & Status
        assertTrue(summary.overallWeightedProgressPercent > 0)
        assertEquals(LearningPathStatus.IN_PROGRESS, summary.status)
        assertTrue(summary.nextActionRecommendation.isNotBlank())
    }

    @Test
    fun learning_paths_engine_all_10_paths_have_complete_4_pillar_coverage() {
        val allSummaries = LearningPathsEngine.evaluateAllPaths(emptyList())
        assertEquals(10, allSummaries.size)

        allSummaries.forEach { summary ->
            val path = summary.path
            // Activities
            assertEquals(path.orderedActivityIds.size, summary.activityProgress.totalActivities)
            assertEquals(0, summary.activityProgress.completedActivities)
            assertEquals(0, summary.activityProgress.percentComplete)

            // Exercises
            assertEquals(path.orderedActivityIds.size, summary.exerciseProgress.totalExercises)
            assertTrue("Path ${path.pathId} must have exercise breakdown", summary.exerciseProgress.exerciseTypeBreakdown.isNotEmpty())

            // Techniques
            assertTrue("Path ${path.pathId} must have techniques", summary.techniqueProgress.totalTechniques >= 3)
            summary.techniqueProgress.techniques.forEach { tech ->
                assertTrue(tech.title.isNotBlank())
                assertTrue(tech.coreRule.isNotBlank())
            }

            // Tasks
            assertTrue("Path ${path.pathId} must have activity + milestone + mission tasks", summary.taskProgress.totalTasks > path.orderedActivityIds.size)
            assertTrue(summary.nextActionRecommendation.isNotBlank())
        }

        // Verify Locked State for path requiring prerequisite
        val intermediatePath = allSummaries.first { it.path.pathId == "path_02_intermediate_english_fluency" }
        assertFalse(intermediatePath.prerequisitesMet)
        assertEquals(LearningPathStatus.LOCKED, intermediatePath.status)
        assertTrue(intermediatePath.nextActionRecommendation.contains("Complete at least 70% of Beginner English Fluency"))
    }

    @Test
    fun analytics_engine_computes_real_deltas_streaks_and_keeps_confidence_distinct() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val today = LocalDate.of(2026, 11, 19)
        val todayKey = today.format(formatter)
        val yesterdayKey = today.minusDays(1).format(formatter)
        val nowInstant = Instant.now()
        val yesterdayInstant = nowInstant.minus(1, ChronoUnit.DAYS)

        val progress = listOf(
            UserActivityProgress(
                activityId = "m01_daily_english_speaking",
                moduleId = "mod_01_english_fluency",
                skillCategory = ActivitySkillCategory.ENGLISH_FLUENCY.name,
                completed = true,
                attemptsCount = 2,
                bestObjectiveScore = 84,
                latestObjectiveScore = 84,
                totalPracticeSeconds = 420
            ),
            UserActivityProgress(
                activityId = "m02_mouth_movement_exercises",
                moduleId = "mod_02_pronunciation_articulation",
                skillCategory = ActivitySkillCategory.PRONUNCIATION.name,
                completed = true,
                attemptsCount = 1,
                bestObjectiveScore = 72,
                latestObjectiveScore = 72,
                totalPracticeSeconds = 180
            )
        )
        val attempts = listOf(
            ActivityAttemptRecord(
                activityId = "m01_daily_english_speaking",
                moduleId = "mod_01_english_fluency",
                skillCategory = ActivitySkillCategory.ENGLISH_FLUENCY.name,
                objectiveScore = 64,
                confidenceSelfRating = 45,
                durationSeconds = 180,
                dateKey = yesterdayKey,
                createdAt = Timestamp(Date.from(yesterdayInstant))
            ),
            ActivityAttemptRecord(
                activityId = "m01_daily_english_speaking",
                moduleId = "mod_01_english_fluency",
                skillCategory = ActivitySkillCategory.ENGLISH_FLUENCY.name,
                objectiveScore = 84,
                confidenceSelfRating = 85,
                durationSeconds = 240,
                dateKey = todayKey,
                createdAt = Timestamp(Date.from(nowInstant))
            ),
            ActivityAttemptRecord(
                activityId = "m02_mouth_movement_exercises",
                moduleId = "mod_02_pronunciation_articulation",
                skillCategory = ActivitySkillCategory.PRONUNCIATION.name,
                objectiveScore = 72,
                confidenceSelfRating = 60,
                durationSeconds = 180,
                dateKey = todayKey,
                createdAt = Timestamp(Date.from(nowInstant))
            )
        )

        val snapshot = ActivityProgressAnalyticsEngine.computeSnapshot(progress, attempts, todayKey)
        assertEquals(2, snapshot.totalActivitiesCompleted)
        assertEquals(10, snapshot.totalPracticeMinutes) // (420 + 180) / 60
        assertEquals(2, snapshot.currentConsistencyStreak)
        assertEquals(10, snapshot.moduleStats.size)

        // Objective avg = (64 + 84 + 72) / 3 = 73; Confidence avg = (45 + 85 + 60) / 3 = 63
        assertEquals(73, snapshot.averageObjectiveScore)
        assertEquals(63, snapshot.averageConfidenceSelfRating)
        assertNotEquals(snapshot.averageObjectiveScore, snapshot.averageConfidenceSelfRating)

        val fluencyTrend = snapshot.skillTrends.first { it.skillCategory == ActivitySkillCategory.ENGLISH_FLUENCY }
        assertEquals(64, fluencyTrend.earliestScore)
        assertEquals(84, fluencyTrend.latestScore)
        assertEquals(20, fluencyTrend.delta)
        assertTrue(snapshot.achievedMilestones.any { it.contains("First Activity", ignoreCase = true) })
    }
}
