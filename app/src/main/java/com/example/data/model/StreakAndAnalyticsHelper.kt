package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object StreakAndAnalyticsHelper {

    private fun parseUtcDateMillis(dateKey: String): Long? {
        if (dateKey.isBlank()) return null
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(dateKey.trim())?.time
        }.getOrNull()
    }

    /**
     * Computes consecutive-day streak cleanly:
     * - Same day practice keeps existing streak (>= 1)
     * - Consecutive next day increments streak (+1)
     * - Gap >= 2 days resets streak to 1
     * - No completed steps keeps existing streak unchanged
     */
    fun computeUpdatedStreak(
        prevLastDateKey: String,
        todayDateKey: String,
        prevStreak: Long,
        hasCompletedSteps: Boolean
    ): Long {
        if (!hasCompletedSteps) return prevStreak.coerceAtLeast(0L)
        if (prevLastDateKey == todayDateKey) return prevStreak.coerceAtLeast(1L)
        val prevMs = parseUtcDateMillis(prevLastDateKey) ?: return 1L
        val todayMs = parseUtcDateMillis(todayDateKey) ?: return 1L
        val daysBetween = TimeUnit.MILLISECONDS.toDays(todayMs - prevMs)
        return when {
            daysBetween == 1L -> prevStreak.coerceAtLeast(0L) + 1L
            daysBetween <= 0L -> prevStreak.coerceAtLeast(1L)
            else -> 1L
        }
    }

    fun computeMinutesDelta(previousDayMinutes: Long, updatedDayMinutes: Long): Long =
        (updatedDayMinutes - previousDayMinutes).coerceAtLeast(0L)

    /**
     * Filters speaking sessions by time window ("Daily" = 24h, "Weekly" = 7d, "Monthly" = 30d).
     * Sessions with null createdAt (fresh local or test objects) are included in the active window.
     */
    fun filterSessionsByPeriod(
        sessions: List<SpeakingSession>,
        period: String,
        nowMs: Long = System.currentTimeMillis()
    ): List<SpeakingSession> {
        val windowMs = when (period.lowercase(Locale.US)) {
            "daily" -> TimeUnit.DAYS.toMillis(1L)
            "weekly" -> TimeUnit.DAYS.toMillis(7L)
            "monthly" -> TimeUnit.DAYS.toMillis(30L)
            else -> return sessions
        }
        val cutoffMs = nowMs - windowMs
        return sessions.filter { sess ->
            val createdMs = sess.createdAt?.toDate()?.time ?: return@filter true
            createdMs >= cutoffMs
        }
    }
}
