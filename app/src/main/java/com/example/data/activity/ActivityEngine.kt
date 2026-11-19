package com.example.data.activity

import com.example.data.model.UserProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class LoadedActivityResult {
    data class Valid(
        val activity: ActivityDefinition,
        val module: ActivityModuleDefinition?
    ) : LoadedActivityResult()

    data class Invalid(
        val activityId: String,
        val errors: List<String>
    ) : LoadedActivityResult()
}

data class ActivitySubmissionOutcome(
    val evaluation: ActivityEvaluationResult,
    val updatedProgress: UserActivityProgress,
    val attemptRecord: ActivityAttemptRecord,
    val recommendedNextActivities: List<ActivityDefinition>
)

class ActivityEngine(
    private val evaluationEngine: UniversalEvaluationEngine = UniversalEvaluationEngine()
) {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun loadActivity(activityId: String): LoadedActivityResult {
        val (definition, validation) = ActivityCatalogue.getValidatedActivity(activityId)
        return if (definition != null && validation.valid) {
            LoadedActivityResult.Valid(
                activity = definition,
                module = ActivityCatalogue.getModuleById(definition.moduleId)
            )
        } else {
            LoadedActivityResult.Invalid(
                activityId = activityId,
                errors = validation.errors.ifEmpty { listOf("Activity '$activityId' was not found in the catalogue.") }
            )
        }
    }

    fun validateUserSubmission(
        activity: ActivityDefinition,
        response: ActivityUserResponse
    ): String? {
        val hasText = response.combinedTextForAnalysis.isNotBlank()
        val hasMedia = response.recordedAudioPath.isNotBlank() || response.recordedVideoPath.isNotBlank() || response.mediaBytes != null
        val hasChecklist = response.completedChecklistItems.isNotEmpty()
        val hasDialogue = response.dialogueTurns.any { it.role == "user" && it.text.isNotBlank() }

        if (!hasText && !hasMedia && !hasChecklist && !hasDialogue) {
            return "Please record your response, enter text, complete a dialogue turn, or check off practice cues before submitting."
        }
        if (activity.completionCriteria.requiresChecklistCompletion &&
            activity.interactionConfiguration.checklistItems.isNotEmpty() &&
            response.completedChecklistItems.isEmpty()
        ) {
            return "Please check off at least one guided practice cue before submitting."
        }
        return null
    }

    suspend fun evaluateAndBuildOutcome(
        userId: String,
        activity: ActivityDefinition,
        response: ActivityUserResponse,
        existingProgress: UserActivityProgress?,
        allProgress: List<UserActivityProgress>,
        recentAttempts: List<ActivityAttemptRecord>,
        todayDateKey: String = LocalDate.now().format(dateFormatter)
    ): ActivitySubmissionOutcome {
        val recommendedNext = recommendNextActivities(
            currentActivity = activity,
            allProgress = allProgress,
            recentAttempts = recentAttempts,
            limit = 3
        )
        val evaluation = evaluationEngine.evaluate(
            activity = activity,
            response = response,
            recommendedNextIds = recommendedNext.map { it.activityId }
        )

        val prevAttempts = existingProgress?.attemptsCount ?: 0L
        val prevBest = existingProgress?.bestObjectiveScore ?: 0L
        val prevSeconds = existingProgress?.totalPracticeSeconds ?: 0L
        val durationSec = response.durationSeconds.coerceAtLeast(15).toLong()
        val nowCompleted = (existingProgress?.completed == true) || evaluation.passedCriteria

        val updatedProgress = UserActivityProgress(
            id = activity.activityId,
            userId = userId,
            activityId = activity.activityId,
            moduleId = activity.moduleId,
            skillCategory = activity.skillCategory.name,
            completed = nowCompleted,
            attemptsCount = prevAttempts + 1L,
            bestObjectiveScore = maxOf(prevBest, evaluation.overallObjectiveScore.toLong()),
            latestObjectiveScore = evaluation.overallObjectiveScore.toLong(),
            latestConfidenceSelfRating = (response.confidenceSelfRating ?: 0).toLong().coerceIn(0L, 100L),
            totalPracticeSeconds = prevSeconds + durationSec,
            lastPracticedDateKey = todayDateKey,
            createdAt = existingProgress?.createdAt
        )

        val attemptRecord = ActivityAttemptRecord(
            userId = userId,
            activityId = activity.activityId,
            moduleId = activity.moduleId,
            skillCategory = activity.skillCategory.name,
            exerciseType = activity.exerciseType.name,
            evaluatorName = evaluation.evaluatorName,
            dateKey = todayDateKey,
            durationSeconds = durationSec,
            objectiveScore = evaluation.overallObjectiveScore.toLong(),
            confidenceSelfRating = (response.confidenceSelfRating ?: 0).toLong().coerceIn(0L, 100L),
            passed = evaluation.passedCriteria,
            aiEvaluated = evaluation.aiEvaluated,
            responseSummary = response.combinedTextForAnalysis.take(600),
            whatWentWell = evaluation.whatWentWell.take(4),
            specificMistakes = evaluation.specificMistakes.take(4),
            improvementSuggestions = evaluation.improvementSuggestions.take(4),
            nextPracticeTask = evaluation.nextPracticeTask.take(300),
            recordedMediaPath = response.recordedVideoPath.ifBlank { response.recordedAudioPath }.take(400),
            wordsPerMinute = evaluation.wordsPerMinute.toLong(),
            fillerCount = evaluation.fillerCount.toLong()
        )

        return ActivitySubmissionOutcome(
            evaluation = evaluation,
            updatedProgress = updatedProgress,
            attemptRecord = attemptRecord,
            recommendedNextActivities = recommendedNext
        )
    }

    fun recommendNextActivities(
        currentActivity: ActivityDefinition?,
        allProgress: List<UserActivityProgress>,
        recentAttempts: List<ActivityAttemptRecord>,
        profile: UserProfile? = null,
        limit: Int = 5
    ): List<ActivityDefinition> {
        val completedIds = allProgress.filter { it.completed }.map { it.activityId }.toSet()
        val recentIds = recentAttempts.take(8).map { it.activityId }.toSet()
        val results = mutableListOf<ActivityDefinition>()
        val seen = mutableSetOf<String>()
        currentActivity?.activityId?.let { seen.add(it) }

        // 1. Direct curriculum follow-ups defined on the current activity
        currentActivity?.nextActivityIds?.forEach { nextId ->
            val candidate = ActivityCatalogue.getActivityById(nextId)
            if (candidate != null && seen.add(candidate.activityId)) {
                results += candidate
            }
        }

        // 2. Weakest skill area activities not yet completed
        val weakestSkill = recentAttempts
            .groupBy { it.skillCategory }
            .mapValues { (_, list) -> list.map { it.objectiveScore }.average() }
            .minByOrNull { it.value }
            ?.key

        if (weakestSkill != null) {
            ActivityCatalogue.allActivities
                .filter {
                    it.skillCategory.name.equals(weakestSkill, ignoreCase = true) &&
                        it.activityId !in completedIds &&
                        it.activityId !in recentIds
                }
                .take(limit)
                .forEach { act ->
                    if (results.size < limit && seen.add(act.activityId)) {
                        results += act
                    }
                }
        }

        // 3. Fill remaining with uncompleted activities aligned to learner level
        val preferredDiff = when (profile?.communicationLevel?.lowercase()) {
            "beginner" -> ActivityDifficulty.BEGINNER
            "advanced" -> ActivityDifficulty.ADVANCED
            else -> ActivityDifficulty.INTERMEDIATE
        }
        ActivityCatalogue.allActivities
            .filter { it.activityId !in completedIds && it.activityId !in seen }
            .sortedBy { kotlin.math.abs(it.difficulty.rank - preferredDiff.rank) }
            .forEach { act ->
                if (results.size < limit && seen.add(act.activityId)) {
                    results += act
                }
            }

        return results.take(limit)
    }
}
