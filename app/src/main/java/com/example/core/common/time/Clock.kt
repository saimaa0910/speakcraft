package com.example.core.common.time

import java.time.Instant

/**
 * Pluggable clock interface for testable, deterministic time (T-02.1.2 / ARC-01 §2).
 */
interface Clock {
    fun nowMillis(): Long
    fun nowInstant(): Instant = Instant.ofEpochMilli(nowMillis())
}

/**
 * Production clock backed by System.currentTimeMillis().
 */
object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/**
 * Deterministic test clock with manual advancement capability.
 */
class TestClock(var currentEpochMillis: Long = 1_700_000_000_000L) : Clock {
    override fun nowMillis(): Long = currentEpochMillis

    fun advanceByMillis(millis: Long) {
        currentEpochMillis += millis
    }
}
