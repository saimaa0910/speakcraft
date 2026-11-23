package com.example.core.common.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Provides ISO-8601 date keys (YYYY-MM-DD) for progress tracking, streaks, and practice plans (T-02.1.2).
 */
interface DateKeyProvider {
    fun todayDateKey(): String
    fun dateKeyFor(epochMillis: Long): String
}

class SystemDateKeyProvider(
    private val clock: Clock = SystemClock,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : DateKeyProvider {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun todayDateKey(): String = dateKeyFor(clock.nowMillis())

    override fun dateKeyFor(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return localDate.format(formatter)
    }
}
