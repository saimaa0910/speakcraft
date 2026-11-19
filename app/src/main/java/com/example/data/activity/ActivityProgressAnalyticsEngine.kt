package com.example.data.activity

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ActivityProgressAnalyticsEngine {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun computeSnapshot(
        progressList: List<UserActivityProgress>,
        attempts: List<ActivityAttemptRecord>,
        todayDateKey: String = LocalDate.now().format(dateFormatter)
    ): ActivityAnalyticsSnapshot {
        val completedSet = progressList.filter { it.completed }.map { it.activityId }.toSet()
        val totalCompleted = completedSet.size
        val totalAttempts = attempts.size
        val totalPracticeMinutes = (attempts.sumOf { it.durationSeconds } / 60L).toInt()
            .coerceAtLeast((progressList.sumOf { it.totalPracticeSeconds } / 60L).toInt())

        val today = runCatching { LocalDate.parse(todayDateKey, dateFormatter) }.getOrElse { LocalDate.now() }
        val sevenDaysAgo = today.minusDays(6)

        val uniqueDates = attempts
            .mapNotNull { runCatching { LocalDate.parse(it.dateKey, dateFormatter) }.getOrNull() }
            .toSet()

        val activeDaysLast7 = uniqueDates.count { !it.isBefore(sevenDaysAgo) && !it.isAfter(today) }
        val weeklyAttemptsCount = attempts.count { rec ->
            val d = runCatching { LocalDate.parse(rec.dateKey, dateFormatter) }.getOrNull()
            d != null && !d.isBefore(sevenDaysAgo) && !d.isAfter(today)
        }

        // Compute consecutive daily consistency streak ending today or yesterday
        var streak = 0
        var cursor = if (today in uniqueDates) today else today.minusDays(1)
        while (cursor in uniqueDates) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val averageObjectiveScore = if (attempts.isEmpty()) {
            0
        } else {
            attempts.map { it.objectiveScore.toInt() }.average().toInt().coerceIn(0, 100)
        }

        // Confidence self-assessment is strictly separated from objective scores
        val ratedConfidence = attempts.map { it.confidenceSelfRating.toInt() }.filter { it > 0 }
        val averageConfidence = if (ratedConfidence.isEmpty()) null else ratedConfidence.average().toInt().coerceIn(1, 100)

        // Per-module completion stats across all 10 modules
        val progressByActivity = progressList.associateBy { it.activityId }
        val moduleStats = ActivityCatalogue.modules.map { module ->
            val moduleActivities = ActivityCatalogue.getActivitiesForModule(module.moduleId)
            val done = moduleActivities.count { it.activityId in completedSet }
            val scores = moduleActivities.mapNotNull { act ->
                progressByActivity[act.activityId]?.latestObjectiveScore?.toInt()?.takeIf { it > 0 }
            }
            ModuleCompletionStat(
                module = module,
                completedCount = done,
                totalCount = moduleActivities.size,
                averageObjectiveScore = if (scores.isEmpty()) 0 else scores.average().toInt()
            )
        }

        // Historical skill trends comparing earliest vs latest actual attempts
        val skillTrends = ActivitySkillCategory.entries.mapNotNull { category ->
            val catAttempts = attempts
                .filter { it.skillCategory.equals(category.name, ignoreCase = true) }
                .sortedBy { it.createdAt?.seconds ?: 0L }
            if (catAttempts.isEmpty()) return@mapNotNull null
            val earliest = catAttempts.first().objectiveScore.toInt()
            val latest = catAttempts.last().objectiveScore.toInt()
            SkillTrendComparison(
                skillCategory = category,
                earliestScore = earliest,
                latestScore = latest,
                delta = latest - earliest,
                attemptCount = catAttempts.size
            )
        }

        val improvementAreas = skillTrends
            .sortedBy { it.latestScore }
            .take(3)
            .map { it.skillCategory }

        val recordingsCount = attempts.count { it.recordedMediaPath.isNotBlank() }

        val milestones = buildList {
            if (totalCompleted >= 1) add("First Activity Completed")
            if (totalCompleted >= 10) add("10 Activities Mastered")
            if (totalCompleted >= 25) add("25-Activity Studio Milestone")
            if (totalCompleted >= 50) add("50-Activity Communication Builder")
            if (totalCompleted >= 100) add("100-Activity Centurion")
            if (totalCompleted >= 150) add("Full 150-Activity Catalogue Master")
            if (streak >= 3) add("3-Day Practice Consistency")
            if (streak >= 7) add("7-Day Communication Streak")
            if (recordingsCount >= 5) add("5+ Audio/Video Self-Audits")
            val completedModules = moduleStats.count { it.totalCount > 0 && it.completedCount >= it.totalCount }
            if (completedModules >= 1) add("$completedModules Curriculum Module(s) Completed")
        }

        return ActivityAnalyticsSnapshot(
            totalActivitiesCompleted = totalCompleted,
            totalAttempts = totalAttempts,
            totalPracticeMinutes = totalPracticeMinutes,
            activeDaysLast7 = activeDaysLast7,
            currentConsistencyStreak = streak,
            weeklyAttemptsCount = weeklyAttemptsCount,
            averageObjectiveScore = averageObjectiveScore,
            averageConfidenceSelfRating = averageConfidence,
            moduleStats = moduleStats,
            skillTrends = skillTrends,
            improvementAreas = improvementAreas,
            achievedMilestones = milestones,
            recordingsCount = recordingsCount
        )
    }
}
