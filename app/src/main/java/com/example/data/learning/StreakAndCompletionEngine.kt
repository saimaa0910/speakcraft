package com.example.data.learning

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Deterministic, timezone-aware Streak & Daily Completion Engine.
 *
 * Rules:
 * - Streak ONLY increments when the daily completion requirement is met.
 * - Multiple completions on the same local date are deduplicated (no double-increment).
 * - Missing 1+ days resets currentStreakDays to 0 (or 1 when today is newly completed)
 *   while strictly preserving longestStreakDays and historical completedDateKeys.
 * - Tracks milestones at 3, 7, 14, 30, 60, and 100 consecutive days.
 */
object StreakAndCompletionEngine {

    val MILESTONE_DAYS = listOf(3, 7, 14, 30, 60, 100)
    private val ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKey(zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.now(zoneId).format(ISO_FORMAT)

    fun parseDate(dateKey: String): LocalDate? =
        runCatching { LocalDate.parse(dateKey.trim(), ISO_FORMAT) }.getOrNull()

    /**
     * Recomputes streak state cleanly when the app opens or a day rolls over,
     * WITHOUT modifying historical dates or longest streak.
     */
    fun refreshForDate(
        current: LearningStreakState,
        todayDateKey: String = todayKey()
    ): LearningStreakState {
        val today = parseDate(todayDateKey) ?: return current
        val completedDates = current.completedDateKeys.mapNotNull { parseDate(it) }.distinct().sorted()
        val todayMet = today in completedDates
        val computedCurrent = computeActiveStreakEndingAt(completedDates, today)
        val computedLongest = maxOf(current.longestStreakDays, computeLongestStreak(completedDates), computedCurrent)
        val weekMap = buildWeeklyCompletionMap(completedDates, today)

        return current.copy(
            currentStreakDays = computedCurrent,
            longestStreakDays = computedLongest,
            lastCompletedDateKey = completedDates.lastOrNull()?.format(ISO_FORMAT).orEmpty(),
            completedDateKeys = completedDates.map { it.format(ISO_FORMAT) },
            weeklyCompletionMap = weekMap,
            todayRequirementMet = todayMet
        )
    }

    /**
     * Records daily completion for [todayDateKey] iff [requirementMet] is true.
     * Idempotent for multiple calls on the same date.
     */
    fun recordDailyCompletion(
        current: LearningStreakState,
        todayDateKey: String = todayKey(),
        requirementMet: Boolean
    ): LearningStreakState {
        val refreshed = refreshForDate(current, todayDateKey)
        if (!requirementMet) return refreshed

        val today = parseDate(todayDateKey) ?: return refreshed
        val existingDates = refreshed.completedDateKeys.mapNotNull { parseDate(it) }.toMutableSet()
        val alreadyRecordedToday = today in existingDates
        existingDates.add(today)

        val sortedDates = existingDates.sorted()
        val newCurrent = computeActiveStreakEndingAt(sortedDates, today)
        val newLongest = maxOf(refreshed.longestStreakDays, computeLongestStreak(sortedDates), newCurrent)
        val weekMap = buildWeeklyCompletionMap(sortedDates, today)

        val newlyUnlocked = if (!alreadyRecordedToday && newCurrent in MILESTONE_DAYS && newCurrent !in refreshed.achievedMilestones) {
            newCurrent
        } else null

        val updatedMilestones = (refreshed.achievedMilestones + listOfNotNull(newlyUnlocked)).distinct().sorted()

        return refreshed.copy(
            currentStreakDays = newCurrent,
            longestStreakDays = newLongest,
            lastCompletedDateKey = sortedDates.lastOrNull()?.format(ISO_FORMAT).orEmpty(),
            completedDateKeys = sortedDates.map { it.format(ISO_FORMAT) },
            weeklyCompletionMap = weekMap,
            achievedMilestones = updatedMilestones,
            latestMilestoneUnlocked = newlyUnlocked ?: refreshed.latestMilestoneUnlocked,
            todayRequirementMet = true
        )
    }

    /**
     * Number of whole calendar days missed between the last completed date and [todayDateKey]
     * (returns 0 if practiced today or yesterday, or if brand new user).
     */
    fun missedDaysCount(current: LearningStreakState, todayDateKey: String = todayKey()): Int {
        val last = parseDate(current.lastCompletedDateKey) ?: return 0
        val today = parseDate(todayDateKey) ?: return 0
        val diff = ChronoUnit.DAYS.between(last, today).toInt()
        return (diff - 1).coerceAtLeast(0)
    }

    /**
     * Active streak count as of [referenceDate]:
     * - If practiced on [referenceDate], count consecutive days backwards from [referenceDate].
     * - If practiced yesterday (not yet today), streak stays alive at yesterday's count.
     * - If last practice was 2+ days ago, active streak is 0 (broken).
     */
    private fun computeActiveStreakEndingAt(sortedDates: List<LocalDate>, referenceDate: LocalDate): Int {
        if (sortedDates.isEmpty()) return 0
        val dateSet = sortedDates.toHashSet()
        var cursor = when {
            referenceDate in dateSet -> referenceDate
            referenceDate.minusDays(1) in dateSet -> referenceDate.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (cursor in dateSet) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun computeLongestStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        var longest = 1
        var run = 1
        for (i in 1 until sortedDates.size) {
            val diff = ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
            if (diff == 1L) {
                run++
                if (run > longest) longest = run
            } else if (diff > 1L) {
                run = 1
            }
        }
        return longest
    }

    private fun buildWeeklyCompletionMap(sortedDates: List<LocalDate>, referenceDate: LocalDate): Map<String, Boolean> {
        val dateSet = sortedDates.toHashSet()
        val monday = referenceDate.with(DayOfWeek.MONDAY)
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return labels.mapIndexed { idx, label ->
            val day = monday.plusDays(idx.toLong())
            label to (day in dateSet)
        }.toMap()
    }
}
