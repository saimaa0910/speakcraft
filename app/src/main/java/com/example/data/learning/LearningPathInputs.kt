package com.example.data.learning

import com.example.data.activity.ActivityAttemptRecord
import com.example.data.activity.UserActivityProgress

/**
 * Everything the learning-path engine needs from the locally persisted learning state, so every pillar
 * (activities, exercises, techniques, tasks) is computed from progress that survives restarts.
 */
data class LearningPathInputs(
    val progress: List<UserActivityProgress>,
    val attempts: List<ActivityAttemptRecord>,
    val dailyTasks: List<DailyPracticeTask>,
    val techniquePractice: Map<String, TechniquePracticeStat>
) {
    companion object {
        val EMPTY = LearningPathInputs(emptyList(), emptyList(), emptyList(), emptyMap())

        fun from(snapshot: PersistedLearningSnapshot): LearningPathInputs {
            val uid = snapshot.userId
            return LearningPathInputs(
                progress = snapshot.activityProgress.values.map { it.toUserActivityProgress(uid) },
                attempts = snapshot.activityAttempts.map { it.toAttemptRecord(uid) },
                // Current set first so its live state wins; then completed tasks of earlier sets and days.
                dailyTasks = (snapshot.dailyPlan?.tasks.orEmpty() + snapshot.completedDailyTasks).distinctBy { it.id },
                techniquePractice = snapshot.techniquePractice
            )
        }
    }
}

fun LocalActivityProgress.toUserActivityProgress(userId: String): UserActivityProgress = UserActivityProgress(
    id = activityId,
    userId = userId,
    activityId = activityId,
    moduleId = moduleId,
    skillCategory = skillCategory,
    completed = completed,
    attemptsCount = attemptsCount.toLong(),
    bestObjectiveScore = bestObjectiveScore.toLong(),
    latestObjectiveScore = latestObjectiveScore.toLong(),
    latestConfidenceSelfRating = latestConfidenceSelfRating.toLong(),
    totalPracticeSeconds = totalPracticeSeconds,
    lastPracticedDateKey = lastPracticedDateKey
)

fun LocalActivityAttempt.toAttemptRecord(userId: String): ActivityAttemptRecord = ActivityAttemptRecord(
    id = id,
    userId = userId,
    activityId = activityId,
    moduleId = moduleId,
    skillCategory = skillCategory,
    exerciseType = exerciseType,
    evaluatorName = evaluatorName,
    dateKey = dateKey,
    durationSeconds = durationSeconds,
    objectiveScore = objectiveScore.toLong(),
    confidenceSelfRating = confidenceSelfRating.toLong(),
    passed = passed,
    aiEvaluated = aiEvaluated,
    wordsPerMinute = wordsPerMinute.toLong(),
    fillerCount = fillerCount.toLong()
)
