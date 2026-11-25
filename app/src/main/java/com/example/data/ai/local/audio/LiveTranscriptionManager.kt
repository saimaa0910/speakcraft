package com.example.data.ai.local.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.preferences.AISettings
import com.example.data.preferences.RecognitionEnginePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class LiveTranscriptionStatus {
    DISABLED,
    IDLE,
    INITIALIZING,
    LISTENING,
    PARTIAL_RESULT,
    FINAL_RESULT,
    ERROR,
    UNAVAILABLE
}

data class LiveTranscriptionState(
    val status: LiveTranscriptionStatus = LiveTranscriptionStatus.DISABLED,
    val sessionId: String = "",
    val finalizedText: String = "",
    val partialText: String = "",
    val activeEngineLabel: String = "Unverified",
    val offlineVerified: Boolean = false,
    val hasMicPermission: Boolean = false,
    val languageTag: String = "en-US",
    val errorMessage: String? = null
) {
    val displayText: String
        get() = listOf(finalizedText.trim(), partialText.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
}

interface LiveRecognizerListener {
    fun onReady()
    fun onPartial(text: String)
    fun onFinal(text: String)
    fun onError(code: Int, message: String)
}

interface LiveRecognizerBackend {
    fun isRecognitionAvailable(): Boolean
    fun isOfflineRecognitionAvailable(): Boolean
    fun startListening(
        languageTag: String,
        preferOffline: Boolean,
        listener: LiveRecognizerListener
    ): Result<Unit>
    fun stopListening()
    fun release()
}

class AndroidLiveRecognizerBackend(
    private val context: Context
) : LiveRecognizerBackend {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    override fun isRecognitionAvailable(): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    override fun isOfflineRecognitionAvailable(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)
        } else {
            false
        }

    override fun startListening(
        languageTag: String,
        preferOffline: Boolean,
        listener: LiveRecognizerListener
    ): Result<Unit> {
        if (!isRecognitionAvailable()) {
            return Result.failure(IllegalStateException("Android SpeechRecognizer service is unavailable on this device."))
        }
        return runCatching {
            val startTask = Runnable {
                try {
                    speechRecognizer?.destroy()
                    val sr = if (preferOffline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isOfflineRecognitionAvailable()) {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                    speechRecognizer = sr
                    sr.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            listener.onReady()
                        }

                        override fun onBeginningOfSpeech() = Unit
                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                        override fun onEndOfSpeech() = Unit

                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO ->
                                    "Microphone audio is busy or unavailable for live speech recognition."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                    "Microphone permission is required for live transcription."
                                SpeechRecognizer.ERROR_NO_MATCH ->
                                    "No recognizable words detected yet. Tap Retry to listen again."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                    "Listening timed out waiting for speech. Tap Retry to continue."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                    "Speech recognition engine is busy."
                                SpeechRecognizer.ERROR_NETWORK,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                    "Speech recognition engine requires an offline pack or network access."
                                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                                    "Selected language ($languageTag) offline pack is not installed on this device."
                                else -> "Speech recognition error (code $error)."
                            }
                            listener.onError(error, msg)
                        }

                        override fun onResults(results: Bundle?) {
                            val spoken = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                                .orEmpty()
                            listener.onFinal(spoken)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                                .orEmpty()
                            if (partial.isNotBlank()) {
                                listener.onPartial(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    })

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
                    }
                    sr.startListening(intent)
                } catch (e: Exception) {
                    listener.onError(-1, e.localizedMessage ?: "Failed to initialize Android SpeechRecognizer.")
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                startTask.run()
            } else {
                mainHandler.post(startTask)
            }
        }
    }

    override fun stopListening() {
        val task = Runnable {
            runCatching { speechRecognizer?.stopListening() }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) task.run() else mainHandler.post(task)
    }

    override fun release() {
        val task = Runnable {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
        if (Looper.myLooper() == Looper.getMainLooper()) task.run() else mainHandler.post(task)
    }
}

/**
 * Manages real-time speech transcription during practice sessions with explicit
 * session isolation, honest capability checks, permission gating, and cleanup.
 */
class LiveTranscriptionManager(
    private val context: Context? = null,
    private val capabilityManager: IntelligenceCapabilityManager? = null,
    private val backend: LiveRecognizerBackend? = context?.let { AndroidLiveRecognizerBackend(it) },
    private val micPermissionCheck: (() -> Boolean)? = null
) {
    private val _state = MutableStateFlow(LiveTranscriptionState())
    val state: StateFlow<LiveTranscriptionState> = _state.asStateFlow()

    private var lastSettings: AISettings = AISettings()
    private var activeSessionId: String = UUID.randomUUID().toString()

    fun refreshFromSettings(settings: AISettings) {
        lastSettings = settings
        val hasMic = checkMicPermission()
        val offlineOk = backend?.isOfflineRecognitionAvailable() == true
        val engineAvailable = backend?.isRecognitionAvailable() == true
        val current = _state.value

        val engineLabel = buildEngineLabel(engineAvailable, offlineOk)

        val nextStatus = when {
            !settings.liveTranscriptionEnabled -> LiveTranscriptionStatus.DISABLED
            !hasMic -> LiveTranscriptionStatus.UNAVAILABLE
            !engineAvailable -> LiveTranscriptionStatus.UNAVAILABLE
            current.status == LiveTranscriptionStatus.DISABLED ||
                current.status == LiveTranscriptionStatus.UNAVAILABLE -> LiveTranscriptionStatus.IDLE
            else -> current.status
        }

        val errMsg = when {
            !settings.liveTranscriptionEnabled -> null
            !hasMic -> "Microphone permission (RECORD_AUDIO) is required for live transcription."
            !engineAvailable -> "No speech recognition engine is available on this device."
            else -> current.errorMessage
        }

        _state.value = current.copy(
            status = nextStatus,
            sessionId = activeSessionId,
            activeEngineLabel = engineLabel,
            offlineVerified = offlineOk,
            hasMicPermission = hasMic,
            languageTag = settings.selectedRecognitionLanguage,
            errorMessage = errMsg
        )
    }

    fun prepareNewSession(sessionId: String, settings: AISettings = lastSettings) {
        activeSessionId = sessionId
        backend?.stopListening()
        lastSettings = settings
        val hasMic = checkMicPermission()
        val offlineOk = backend?.isOfflineRecognitionAvailable() == true
        val engineAvailable = backend?.isRecognitionAvailable() == true
        val nextStatus = when {
            !settings.liveTranscriptionEnabled -> LiveTranscriptionStatus.DISABLED
            !hasMic || !engineAvailable -> LiveTranscriptionStatus.UNAVAILABLE
            else -> LiveTranscriptionStatus.IDLE
        }
        _state.value = LiveTranscriptionState(
            status = nextStatus,
            sessionId = sessionId,
            finalizedText = "",
            partialText = "",
            activeEngineLabel = buildEngineLabel(engineAvailable, offlineOk),
            offlineVerified = offlineOk,
            hasMicPermission = hasMic,
            languageTag = settings.selectedRecognitionLanguage,
            errorMessage = when {
                !settings.liveTranscriptionEnabled -> null
                !hasMic -> "Microphone permission required for live transcription."
                !engineAvailable -> "Speech recognition engine unavailable on this device."
                else -> null
            }
        )
    }

    fun startTranscription(
        sessionId: String = activeSessionId,
        settings: AISettings = lastSettings
    ) {
        activeSessionId = sessionId
        lastSettings = settings
        val hasMic = checkMicPermission()
        val engine = backend
        val offlineOk = engine?.isOfflineRecognitionAvailable() == true
        val engineAvailable = engine?.isRecognitionAvailable() == true
        val engineLabel = buildEngineLabel(engineAvailable, offlineOk)

        if (!settings.liveTranscriptionEnabled) {
            _state.value = LiveTranscriptionState(
                status = LiveTranscriptionStatus.DISABLED,
                sessionId = sessionId,
                activeEngineLabel = engineLabel,
                offlineVerified = offlineOk,
                hasMicPermission = hasMic,
                languageTag = settings.selectedRecognitionLanguage
            )
            return
        }

        if (!hasMic) {
            _state.value = LiveTranscriptionState(
                status = LiveTranscriptionStatus.UNAVAILABLE,
                sessionId = sessionId,
                activeEngineLabel = engineLabel,
                offlineVerified = offlineOk,
                hasMicPermission = false,
                languageTag = settings.selectedRecognitionLanguage,
                errorMessage = "Microphone permission (RECORD_AUDIO) is denied. Grant permission to transcribe."
            )
            return
        }

        if (engine == null || !engineAvailable) {
            capabilityManager?.recordRecognizerInitFailure("SpeechRecognizer unavailable on this device.")
            _state.value = LiveTranscriptionState(
                status = LiveTranscriptionStatus.UNAVAILABLE,
                sessionId = sessionId,
                activeEngineLabel = "Unavailable",
                offlineVerified = false,
                hasMicPermission = true,
                languageTag = settings.selectedRecognitionLanguage,
                errorMessage = "Speech recognition service is unavailable on this device."
            )
            return
        }

        _state.value = _state.value.copy(
            status = LiveTranscriptionStatus.INITIALIZING,
            sessionId = sessionId,
            activeEngineLabel = engineLabel,
            offlineVerified = offlineOk,
            hasMicPermission = true,
            languageTag = settings.selectedRecognitionLanguage,
            errorMessage = null
        )

        val listenerSessionId = sessionId
        // Platform recogniser only. The retired LOCAL_WHISPER preference behaves as AUTO (FND-AI-03).
        val preferOffline = RecognitionEnginePreference.fromStored(settings.selectedRecognitionEngine.name) ==
            RecognitionEnginePreference.ANDROID_OFFLINE || offlineOk
        val startResult = engine.startListening(
            languageTag = settings.selectedRecognitionLanguage,
            preferOffline = preferOffline,
            listener = object : LiveRecognizerListener {
                override fun onReady() {
                    if (activeSessionId != listenerSessionId) return
                    capabilityManager?.recordRecognizerInitFailure(null)
                    _state.value = _state.value.copy(
                        status = LiveTranscriptionStatus.LISTENING,
                        errorMessage = null
                    )
                }

                override fun onPartial(text: String) {
                    if (activeSessionId != listenerSessionId) return
                    _state.value = _state.value.copy(
                        status = LiveTranscriptionStatus.PARTIAL_RESULT,
                        partialText = text,
                        errorMessage = null
                    )
                }

                override fun onFinal(text: String) {
                    if (activeSessionId != listenerSessionId) return
                    val currentFinal = _state.value.finalizedText
                    val merged = listOf(currentFinal, text).filter { it.isNotBlank() }.joinToString(" ").trim()
                    if (merged.isBlank()) {
                        _state.value = _state.value.copy(
                            status = LiveTranscriptionStatus.ERROR,
                            partialText = "",
                            errorMessage = "No speech words were recognized."
                        )
                    } else {
                        _state.value = _state.value.copy(
                            status = LiveTranscriptionStatus.FINAL_RESULT,
                            finalizedText = merged,
                            partialText = "",
                            errorMessage = null
                        )
                    }
                }

                override fun onError(code: Int, message: String) {
                    if (activeSessionId != listenerSessionId) return
                    capabilityManager?.recordRecognizerInitFailure(message)
                    _state.value = _state.value.copy(
                        status = LiveTranscriptionStatus.ERROR,
                        partialText = "",
                        errorMessage = message
                    )
                }
            }
        )

        startResult.onFailure { err ->
            val msg = err.localizedMessage ?: "Failed to initialize live speech recognition."
            capabilityManager?.recordRecognizerInitFailure(msg)
            _state.value = _state.value.copy(
                status = LiveTranscriptionStatus.ERROR,
                errorMessage = msg
            )
        }
    }

    fun retryTranscription() {
        startTranscription(sessionId = activeSessionId, settings = lastSettings)
    }

    fun updateEditedTranscript(newText: String) {
        val current = _state.value
        _state.value = current.copy(
            finalizedText = newText,
            partialText = "",
            status = if (newText.isNotBlank()) LiveTranscriptionStatus.FINAL_RESULT else current.status
        )
    }

    fun stopTranscription() {
        backend?.stopListening()
        val current = _state.value
        if (current.status == LiveTranscriptionStatus.LISTENING ||
            current.status == LiveTranscriptionStatus.INITIALIZING ||
            current.status == LiveTranscriptionStatus.PARTIAL_RESULT
        ) {
            val merged = current.displayText
            _state.value = current.copy(
                status = if (merged.isNotBlank()) LiveTranscriptionStatus.FINAL_RESULT else LiveTranscriptionStatus.IDLE,
                finalizedText = merged,
                partialText = ""
            )
        }
    }

    fun release() {
        backend?.release()
    }

    private fun checkMicPermission(): Boolean {
        micPermissionCheck?.let { return it() }
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun buildEngineLabel(
        engineAvailable: Boolean,
        offlineOk: Boolean
    ): String = when {
        !engineAvailable -> "Unavailable"
        offlineOk -> "Android On-Device SpeechRecognizer"
        else -> "Android SpeechRecognizer (Offline Unverified)"
    }
}
