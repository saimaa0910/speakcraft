package com.example.speech.recognition

/**
 * Recognized speech segment produced by a speech recognition session (ARC-05 §4.2, CMP-25).
 */
data class TranscriptSegment(
    val segmentIndex: Int,
    val text: String,
    val isFinal: Boolean,
    val confidence: Float? = null, // null if recognizer does not supply confidence (SRS-BR-005)
    val startOffsetMs: Long = 0L,
    val endOffsetMs: Long = 0L
)

/**
 * Standardized recognition errors mapped from platform [android.speech.SpeechRecognizer] codes
 * (API_AND_INTEGRATION_SPEC §3.2, SRS-FR-SPCH-010, ARC-05 §4.2).
 */
sealed class RecognitionError(val message: String, val canRetrySegment: Boolean) {
    object NoMatch : RecognitionError("No speech match found for audio segment", canRetrySegment = true)
    object SpeechTimeout : RecognitionError("No speech detected within timeout", canRetrySegment = true)
    object Network : RecognitionError("Network error during recognition", canRetrySegment = false)
    object Busy : RecognitionError("Recognizer service busy", canRetrySegment = true)
    object Audio : RecognitionError("Audio recording/transmission error", canRetrySegment = false)
    object Client : RecognitionError("Client-side recognition exception", canRetrySegment = false)
    object Permission : RecognitionError("Audio recording permission missing or revoked", canRetrySegment = false)
    object LanguageUnavailable : RecognitionError("Language model unavailable on device", canRetrySegment = false)
    object CannotCheckSupport : RecognitionError("Platform unable to verify recognition support", canRetrySegment = false)
    data class Unknown(val errorCode: Int) : RecognitionError("Unknown recognition error: $errorCode", canRetrySegment = false)

    companion object {
        fun fromPlatformCode(errorCode: Int): RecognitionError = when (errorCode) {
            7 -> NoMatch // ERROR_NO_MATCH
            6 -> SpeechTimeout // ERROR_SPEECH_TIMEOUT
            1, 2, 4, 11 -> Network // ERROR_NETWORK_TIMEOUT, ERROR_NETWORK, ERROR_SERVER, ERROR_SERVER_DISCONNECTED
            8, 10 -> Busy // ERROR_RECOGNIZER_BUSY, ERROR_TOO_MANY_REQUESTS
            3 -> Audio // ERROR_AUDIO
            5 -> Client // ERROR_CLIENT
            9 -> Permission // ERROR_INSUFFICIENT_PERMISSIONS
            12, 13 -> LanguageUnavailable // ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE
            14, 15 -> CannotCheckSupport // ERROR_CANNOT_CHECK_SUPPORT, ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS
            else -> Unknown(errorCode)
        }
    }
}
