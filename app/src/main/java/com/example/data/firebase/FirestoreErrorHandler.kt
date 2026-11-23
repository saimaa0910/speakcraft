package com.example.data.firebase

import com.example.core.logging.AppLogger
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/**
 * Handles and logs Firestore errors safely without logging PII (SEC-01 §10.1, GAP-06).
 *
 * Replaces the legacy implementation that leaked userId, email, and providerInfo.
 * Only operation type, sanitized collection path, and error code/message are logged.
 */
fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val firestoreError = causes.firstOrNull { it is FirebaseFirestoreException } as? FirebaseFirestoreException
    val rootError = firestoreError
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val errorCode = firestoreError?.code?.name ?: rootError.javaClass.simpleName
    val sanitizedPath = path?.let { AppLogger.redact(it) } ?: "unknown"
    val safeErrorMessage = AppLogger.redact(rootError.message ?: rootError.toString())

    val errorInfoJson = JSONObject().apply {
        put("operationType", operationType.value)
        put("errorCode", errorCode)
        put("path", sanitizedPath)
        put("errorMessage", safeErrorMessage)
    }

    val jsonString = errorInfoJson.toString()
    AppLogger.e(
        "FirestoreError",
        "Firestore ${operationType.value} failed at $sanitizedPath: $errorCode ($safeErrorMessage)"
    )
    return jsonString
}
