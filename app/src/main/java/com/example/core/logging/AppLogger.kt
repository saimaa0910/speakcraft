package com.example.core.logging

import android.util.Log
import com.example.BuildConfig

/**
 * Centralized logging entry point with automatic PII redaction and release suppression.
 *
 * Implements SEC-01 §10.1, ADR-007, SEC-REQ-34, SEC-REQ-35, and TC-SEC-040/041:
 * - Single entry point for all logging; direct [android.util.Log] or [println] outside
 *   this class is prohibited and enforced by CI static checks.
 * - In release builds (`!BuildConfig.DEBUG`), [v], [d], and [i] are no-ops.
 *   [w] and [e] are logged to Logcat strictly AFTER PII redaction.
 * - Strips emails, 28-character Firebase UIDs, JWTs, Firestore user paths, UUIDs,
 *   transcripts, and URL query strings.
 */
object AppLogger {

    @Volatile
    var isDebug: Boolean = BuildConfig.DEBUG

    // Regex patterns per SEC-01 §10.1 & TC-SEC-040
    private val URL_QUERY_REGEX = Regex("""(https?://[^\s?#]+)\?[^\s]*""")
    private val JWT_REGEX = Regex("""\beyJ[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\b""")
    private val EMAIL_REGEX = Regex("""[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+""")
    private val USERS_PATH_REGEX = Regex("""users/[^/\s,"]+""")
    private val FIREBASE_UID_REGEX = Regex("""\b[a-zA-Z0-9]{28}\b""")
    private val UUID_REGEX = Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b""")
    private val TRANSCRIPT_PARAM_REGEX = Regex("""(?i)(transcript\s*[:=]\s*)("[^"]*"|'[^']*'|[^\s,{}]+)""")

    /**
     * Pluggable log consumer for tests or local diagnostic log sink.
     * Default prints to Android platform Log.
     */
    @Volatile
    internal var printer: (priority: Int, tag: String, message: String, throwable: Throwable?) -> Unit =
        { priority, tag, message, throwable ->
            when (priority) {
                Log.VERBOSE -> if (throwable != null) Log.v(tag, message, throwable) else Log.v(tag, message)
                Log.DEBUG -> if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
                Log.INFO -> if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
                Log.WARN -> if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
                Log.ERROR -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
                else -> Log.println(priority, tag, message)
            }
        }

    /**
     * Applies redaction filter rules to any string:
     * - URL query strings stripped
     * - JWT tokens -> `<jwt>`
     * - Emails -> `<email>`
     * - `users/{uid}` -> `users/<uid>`
     * - 28-character Firebase UIDs -> `<uid>`
     * - UUIDs -> `<id>`
     * - Tagged transcripts -> `<transcript>`
     */
    fun redact(input: String?): String {
        if (input.isNullOrEmpty()) return input ?: ""
        var result = input

        // 1. URL query strings: strip parameters from URLs
        result = URL_QUERY_REGEX.replace(result) { match -> match.groupValues[1] }

        // 2. JWT pattern: eyJ...
        result = JWT_REGEX.replace(result, "<jwt>")

        // 3. Email addresses
        result = EMAIL_REGEX.replace(result, "<email>")

        // 4. Firestore user paths: users/<uid>
        result = USERS_PATH_REGEX.replace(result, "users/<uid>")

        // 5. Firebase UID (28-char alphanumeric tokens)
        result = FIREBASE_UID_REGEX.replace(result, "<uid>")

        // 6. UUIDs
        result = UUID_REGEX.replace(result, "<id>")

        // 7. Spoken transcripts
        result = TRANSCRIPT_PARAM_REGEX.replace(result, "$1<transcript>")

        return result
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (!isDebug) return
        dispatch(Log.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (!isDebug) return
        dispatch(Log.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (!isDebug) return
        dispatch(Log.INFO, tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        dispatch(Log.WARN, tag, message, throwable)
    }

    fun w(tag: String, throwable: Throwable) {
        dispatch(Log.WARN, tag, throwable.message ?: throwable.javaClass.simpleName, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        dispatch(Log.ERROR, tag, message, throwable)
    }

    private fun dispatch(priority: Int, tag: String, message: String, throwable: Throwable?) {
        val safeTag = redact(tag)
        val safeMessage = redact(message)

        val safeThrowable = if (throwable != null) {
            if (isDebug) {
                throwable
            } else {
                // In release, never dump full stack traces to logcat (SEC-01 §10.1)
                null
            }
        } else null

        val finalMessage = if (!isDebug && throwable != null) {
            val rootMsg = throwable.message?.let { redact(it) } ?: ""
            "$safeMessage [${throwable.javaClass.simpleName}: $rootMsg]".trim()
        } else {
            safeMessage
        }

        printer(priority, safeTag, finalMessage, safeThrowable)
    }
}
