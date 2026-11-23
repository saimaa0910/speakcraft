package com.example

import android.util.Log
import com.example.core.logging.AppLogger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies Stage B Sprint 2 Task T-04.2.3 (GAP-06, SEC-01 §10.1, SEC-REQ-34, SEC-REQ-35, TC-SEC-040):
 * - Log redaction strips emails, Firebase UIDs (28 chars), JWT tokens, Firestore user paths, UUIDs,
 *   transcripts, and URL query strings.
 * - Release builds suppress VERBOSE, DEBUG, and INFO logs.
 * - Release builds emit WARN and ERROR logs with redaction and without leaking raw stack traces.
 * - Debug builds allow all log levels with redaction applied.
 */
class AppLoggerTest {

    private data class LogEntry(
        val priority: Int,
        val tag: String,
        val message: String,
        val throwable: Throwable?
    )

    private val capturedLogs = mutableListOf<LogEntry>()
    private val originalPrinter = AppLogger.printer
    private val originalIsDebug = AppLogger.isDebug

    @Before
    fun setUp() {
        capturedLogs.clear()
        AppLogger.printer = { priority, tag, message, throwable ->
            capturedLogs.add(LogEntry(priority, tag, message, throwable))
        }
    }

    @After
    fun tearDown() {
        AppLogger.printer = originalPrinter
        AppLogger.isDebug = originalIsDebug
    }

    @Test
    fun `redact replaces email addresses with email token`() {
        val input = "Failed to sync account for user.doe+test@domain.co.uk and backup admin@speakcraft.app"
        val result = AppLogger.redact(input)

        assertFalse("Must not contain original email", result.contains("user.doe+test@domain.co.uk"))
        assertFalse("Must not contain admin email", result.contains("admin@speakcraft.app"))
        assertTrue("Must contain <email> token", result.contains("<email>"))
        assertEquals("Failed to sync account for <email> and backup <email>", result)
    }

    @Test
    fun `redact replaces 28-character Firebase UIDs with uid token`() {
        val uid = "W2mKx6Z1Nq9LbVp0RtY3X8Q4ZaBc" // 28 chars
        val input = "Fetching document for uid: $uid"
        val result = AppLogger.redact(input)

        assertFalse("Must not contain raw Firebase UID", result.contains(uid))
        assertEquals("Fetching document for uid: <uid>", result)
    }

    @Test
    fun `redact replaces JWT bearer tokens with jwt token`() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakThisTokenSignature"
        val input = "Authorization: Bearer $jwt"
        val result = AppLogger.redact(input)

        assertFalse("Must not contain raw JWT", result.contains(jwt))
        assertEquals("Authorization: Bearer <jwt>", result)
    }

    @Test
    fun `redact replaces Firestore users paths with sanitized users path`() {
        val input = "Access denied on users/some_custom_user_id_123/speakingSessions"
        val result = AppLogger.redact(input)

        assertFalse("Must not contain custom user ID in path", result.contains("some_custom_user_id_123"))
        assertEquals("Access denied on users/<uid>/speakingSessions", result)
    }

    @Test
    fun `redact replaces UUIDs with id token`() {
        val uuid = "12345678-abcd-ef01-2345-6789abcdef01"
        val input = "Generated session id $uuid"
        val result = AppLogger.redact(input)

        assertFalse("Must not contain raw UUID", result.contains(uuid))
        assertEquals("Generated session id <id>", result)
    }

    @Test
    fun `redact strips URL query strings from http and https URLs`() {
        val input = "Fetching update from https://releases.speakcraft.app/download?sig=secret123&user=42"
        val result = AppLogger.redact(input)

        assertFalse("Must not contain query parameter values", result.contains("secret123"))
        assertEquals("Fetching update from https://releases.speakcraft.app/download", result)
    }

    @Test
    fun `redact replaces tagged spoken transcripts`() {
        val input = "Evaluation complete for transcript: \"Hello world this is private speech\""
        val result = AppLogger.redact(input)

        assertFalse("Must not leak transcript content", result.contains("Hello world this is private speech"))
        assertEquals("Evaluation complete for transcript:<transcript>", result)
    }

    @Test
    fun `release mode suppresses verbose debug and info logs`() {
        AppLogger.isDebug = false

        AppLogger.v("TestTag", "Verbose message")
        AppLogger.d("TestTag", "Debug message")
        AppLogger.i("TestTag", "Info message")

        assertTrue("Release mode must drop verbose, debug, and info logs", capturedLogs.isEmpty())
    }

    @Test
    fun `release mode emits warn and error logs with redaction`() {
        AppLogger.isDebug = false

        AppLogger.w("TestTag", "Warning for user@example.com")
        AppLogger.e("TestTag", "Error occurred for uid W2mKx6Z1Nq9LbVp0RtY3X8Q4ZaBc")

        assertEquals(2, capturedLogs.size)
        assertEquals(Log.WARN, capturedLogs[0].priority)
        assertEquals("Warning for <email>", capturedLogs[0].message)

        assertEquals(Log.ERROR, capturedLogs[1].priority)
        assertEquals("Error occurred for uid <uid>", capturedLogs[1].message)
    }

    @Test
    fun `release mode redacts throwable message and omits raw stack trace`() {
        AppLogger.isDebug = false

        val exception = RuntimeException("Auth failed for secret-user@speakcraft.app")
        AppLogger.e("AuthTag", "Operation failed", exception)

        assertEquals(1, capturedLogs.size)
        val entry = capturedLogs[0]
        assertEquals(Log.ERROR, entry.priority)
        assertNull("Release mode must not pass raw throwable to printer", entry.throwable)
        assertTrue("Must include exception class and redacted message", entry.message.contains("RuntimeException: Auth failed for <email>"))
        assertFalse("Must not leak sensitive email in throwable message", entry.message.contains("secret-user@speakcraft.app"))
    }

    @Test
    fun `debug mode emits all log levels with redaction`() {
        AppLogger.isDebug = true

        AppLogger.v("Tag", "V log for user@example.com")
        AppLogger.d("Tag", "D log for user@example.com")
        AppLogger.i("Tag", "I log for user@example.com")
        AppLogger.w("Tag", "W log for user@example.com")
        AppLogger.e("Tag", "E log for user@example.com")

        assertEquals(5, capturedLogs.size)
        assertEquals(Log.VERBOSE, capturedLogs[0].priority)
        assertEquals(Log.DEBUG, capturedLogs[1].priority)
        assertEquals(Log.INFO, capturedLogs[2].priority)
        assertEquals(Log.WARN, capturedLogs[3].priority)
        assertEquals(Log.ERROR, capturedLogs[4].priority)

        capturedLogs.forEach { entry ->
            assertEquals("V log for <email>", entry.message.replace(Regex("^[VDIWE] log"), "V log"))
        }
    }
}
