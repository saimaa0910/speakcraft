package com.example.data.learning

/**
 * Completion-driven daily task sets (pure, deterministic).
 *
 * - A day starts with set 1, chosen from the date and the user's persisted [DailyRotationState.sequencePosition].
 * - When every task of the current set is completed, the position advances and the next set is chosen straight
 *   away for the same day ([advance]); items of completed tasks are avoided when choosing it.
 * - Partial completion keeps the current set; the stored item ids rebuild exactly the same set after a restart.
 * - A new day always starts a fresh set (the date is part of the seed), from the same position.
 */
object DailySetRotation {

    /** Makes sure [rotation] holds a set for [todayKey]; returns it with the package it describes. */
    fun ensureSetForDate(
        rotation: DailyRotationState,
        todayKey: String,
        level: LearningLevel,
        targetMinutes: Int
    ): Pair<DailyRotationState, DailyAdaptiveCurriculumPackage> {
        val sameDay = rotation.dateKey == todayKey
        if (sameDay && rotation.items != null) {
            val rebuilt = DailyAdaptiveCurriculumEngine.rebuildPackage(
                dateKey = todayKey,
                items = rotation.items,
                setIndexToday = rotation.setIndexToday,
                sequencePosition = rotation.sequencePosition,
                targetMinutes = targetMinutes
            )
            if (rebuilt != null) return rotation to rebuilt
        }
        val setIndex = if (sameDay) rotation.setIndexToday else 0
        val pkg = DailyAdaptiveCurriculumEngine.generateDailyPackage(
            dateKey = todayKey,
            level = level,
            targetMinutes = targetMinutes,
            sequencePosition = rotation.sequencePosition,
            setIndexToday = setIndex,
            recentItemIds = rotation.recentItemIds
        )
        return rotation.copy(dateKey = todayKey, setIndexToday = setIndex, items = pkg.items()) to pkg
    }

    /** The current set was completed: advance the sequence and choose the next set for [todayKey]. */
    fun advance(
        rotation: DailyRotationState,
        todayKey: String,
        level: LearningLevel,
        targetMinutes: Int,
        nowMs: Long
    ): Pair<DailyRotationState, DailyAdaptiveCurriculumPackage> {
        val recent = withRecent(rotation.recentItemIds, rotation.items?.allIds().orEmpty())
        val sameDay = rotation.dateKey == todayKey
        val next = rotation.copy(
            dateKey = todayKey,
            sequencePosition = rotation.sequencePosition + 1,
            setIndexToday = if (sameDay) rotation.setIndexToday + 1 else 0,
            items = null,
            recentItemIds = recent,
            lastSetCompletedAtMs = nowMs
        )
        val pkg = DailyAdaptiveCurriculumEngine.generateDailyPackage(
            dateKey = todayKey,
            level = level,
            targetMinutes = targetMinutes,
            sequencePosition = next.sequencePosition,
            setIndexToday = next.setIndexToday,
            recentItemIds = recent
        )
        return next.copy(items = pkg.items()) to pkg
    }

    /** Adds [itemIds] to the front of the recent list (most recent first), bounded. */
    fun withRecent(recent: List<String>, itemIds: List<String>): List<String> =
        (itemIds.filter { it.isNotBlank() } + recent).distinct().take(DailyRotationState.MAX_RECENT_ITEMS)

    /** The daily plan (task checklist) for a set. */
    fun planFrom(
        pkg: DailyAdaptiveCurriculumPackage,
        goals: List<LearningGoal>,
        targetMinutes: Int,
        missedDaysBeforeToday: Int,
        nowMs: Long
    ): PersonalizedDailyPlan {
        val isRecovery = pkg.setIndexToday == 0 && missedDaysBeforeToday >= 2
        val minutes = pkg.dailyTasks.sumOf { it.durationMinutes }
        val summary = when {
            pkg.setIndexToday > 0 ->
                "Set ${pkg.setIndexToday + 1} today — you finished ${pkg.setIndexToday} set${if (pkg.setIndexToday > 1) "s" else ""}. " +
                    "Fresh ${pkg.userLevel.label} words, sentences, drills and a new mission (~$minutes min)."
            isRecovery ->
                "Welcome back after $missedDaysBeforeToday missed days! Today's ${pkg.userLevel.label} set: ${pkg.headlineTheme} (~$minutes min)."
            else ->
                "Today's ${pkg.userLevel.label} set: ${pkg.headlineTheme} (~$minutes min). Finish it to unlock the next set."
        }
        return PersonalizedDailyPlan(
            dateKey = pkg.dateKey,
            level = pkg.userLevel,
            goals = goals,
            targetMinutes = targetMinutes,
            isRecoveryPlan = isRecovery,
            missedDaysBeforeToday = missedDaysBeforeToday,
            adaptiveSummary = summary,
            tasks = pkg.dailyTasks,
            generatedAtMs = nowMs
        )
    }
}
