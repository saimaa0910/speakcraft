package com.example

import com.example.core.common.dispatch.StandardDispatcherProvider
import com.example.core.common.dispatch.TestDispatcherProvider
import com.example.core.common.time.SystemClock
import com.example.core.common.time.SystemDateKeyProvider
import com.example.core.common.time.TestClock
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * Verifies Stage B Sprint 3 Task T-02.1.2 (ARC-01 §2, TST-01):
 * - Clock utilities (SystemClock, TestClock)
 * - DateKeyProvider (SystemDateKeyProvider)
 * - DispatcherProvider (StandardDispatcherProvider, TestDispatcherProvider)
 */
class CoreUtilitiesTest {

    @Test
    fun `SystemClock reports current realistic epoch millis and instant`() {
        val now = SystemClock.nowMillis()
        assertTrue("SystemClock epoch millis must be greater than year 2026 threshold", now > 1_700_000_000_000L)

        val instant = SystemClock.nowInstant()
        assertEquals(now / 1000L, instant.epochSecond)
    }

    @Test
    fun `TestClock advances time deterministically`() {
        val baseMillis = 1_700_000_000_000L
        val testClock = TestClock(baseMillis)

        assertEquals(baseMillis, testClock.nowMillis())
        assertEquals(baseMillis / 1000L, testClock.nowInstant().epochSecond)

        testClock.advanceByMillis(5_000L)
        assertEquals(baseMillis + 5_000L, testClock.nowMillis())

        testClock.advanceByMillis(86_400_000L) // 1 day
        assertEquals(baseMillis + 5_000L + 86_400_000L, testClock.nowMillis())
    }

    @Test
    fun `DateKeyProvider formats dates into YYYY-MM-DD pattern and advances with clock`() {
        // 2026-11-23 12:00:00 UTC = 1795435200000L
        val fixedMillis = 1795435200000L
        val testClock = TestClock(fixedMillis)
        val utcZone = ZoneId.of("UTC")
        val dateKeyProvider = SystemDateKeyProvider(clock = testClock, zoneId = utcZone)

        assertEquals("2026-11-23", dateKeyProvider.todayDateKey())

        // Advance 24 hours
        testClock.advanceByMillis(24 * 60 * 60 * 1000L)
        assertEquals("2026-11-24", dateKeyProvider.todayDateKey())

        // Arbitrary epoch conversion
        assertEquals("2026-11-23", dateKeyProvider.dateKeyFor(fixedMillis))
        assertEquals("2026-11-24", dateKeyProvider.dateKeyFor(fixedMillis + 24 * 60 * 60 * 1000L))
    }

    @Test
    fun `StandardDispatcherProvider provides standard coroutine dispatchers`() {
        assertSame(Dispatchers.Main, StandardDispatcherProvider.main)
        assertSame(Dispatchers.IO, StandardDispatcherProvider.io)
        assertSame(Dispatchers.Default, StandardDispatcherProvider.default)
        assertSame(Dispatchers.Unconfined, StandardDispatcherProvider.unconfined)
    }

    @Test
    fun `TestDispatcherProvider routes to configured test dispatcher`() {
        val customDispatcher = Dispatchers.Unconfined
        val testProvider = TestDispatcherProvider(customDispatcher)

        assertSame(customDispatcher, testProvider.main)
        assertSame(customDispatcher, testProvider.io)
        assertSame(customDispatcher, testProvider.default)
        assertSame(customDispatcher, testProvider.unconfined)
    }
}
