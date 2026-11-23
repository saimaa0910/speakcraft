package com.example.data.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "TtsAndSpeech"

/**
 * Result emitted from on-device live speech capture.
 */
data class LiveSpeechCaptureResult(
    val transcript: String,
    val alternatives: List<String> = emptyList(),
    val confidence: Float = 0f,
    val offlinePreferred: Boolean = true,
    val isFinal: Boolean = true
)

class TtsAndSpeechHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _lastConfidence = MutableStateFlow(0f)
    val lastConfidence: StateFlow<Float> = _lastConfidence.asStateFlow()

    private val _lastAlternatives = MutableStateFlow<List<String>>(emptyList())
    val lastAlternatives: StateFlow<List<String>> = _lastAlternatives.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US)
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    fun speak(text: String, rate: Float = 1.0f) {
        if (!ttsReady || text.isBlank()) return
        tts?.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speakcraft_tts_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun resetCaptureState() {
        _liveTranscript.value = ""
        _partialTranscript.value = ""
        _lastConfidence.value = 0f
        _lastAlternatives.value = emptyList()
        _rmsDb.value = 0f
        _lastError.value = null
    }

    /**
     * Backwards-compatible simple dictation helper.
     */
    fun startDictation(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        startLiveSpeechCapture(
            preferOffline = true,
            maxResults = 3,
            onPartial = {},
            onFinalResult = { res ->
                if (res.transcript.isNotBlank()) onResult(res.transcript)
            },
            onError = onError
        )
    }

    /**
     * Full on-device speech capture with offline preference, partial streaming results,
     * alternative hypotheses, confidence scores, and live RMS amplitude for UI meters.
     */
    fun startLiveSpeechCapture(
        preferOffline: Boolean = true,
        maxResults: Int = 5,
        onPartial: (String) -> Unit = {},
        onFinalResult: (LiveSpeechCaptureResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isSpeechRecognitionAvailable()) {
            val msg = "On-device speech recognition is unavailable on this device. You can type your response or use the Audio Recorder."
            _lastError.value = msg
            onError(msg)
            return
        }
        stopDictation()
        resetCaptureState()

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _lastError.value = null
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsDb.value = rmsdB
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val partial = _partialTranscript.value.trim()
                    if (partial.isNotBlank() && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        // Salvage partial on-device recognition with honest conservative confidence
                        val fallback = LiveSpeechCaptureResult(
                            transcript = partial,
                            alternatives = listOf(partial),
                            confidence = 0.55f,
                            offlinePreferred = preferOffline,
                            isFinal = true
                        )
                        _liveTranscript.value = partial
                        _lastConfidence.value = 0.55f
                        _lastAlternatives.value = listOf(partial)
                        onFinalResult(fallback)
                        return
                    }
                    val message = mapSpeechError(error)
                    _lastError.value = message
                    onError(message)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val best = matches.firstOrNull().orEmpty().trim()
                    val conf = when {
                        scores != null && scores.isNotEmpty() && scores[0] > 0f -> scores[0].coerceIn(0f, 1f)
                        best.isNotBlank() -> 0.78f
                        else -> 0f
                    }
                    _liveTranscript.value = best
                    _partialTranscript.value = best
                    _lastConfidence.value = conf
                    _lastAlternatives.value = matches
                    if (best.isNotBlank()) {
                        onFinalResult(
                            LiveSpeechCaptureResult(
                                transcript = best,
                                alternatives = matches,
                                confidence = conf,
                                offlinePreferred = preferOffline,
                                isFinal = true
                            )
                        )
                    } else {
                        val msg = "No clear speech was detected. Please speak closer to the microphone and retry."
                        _lastError.value = msg
                        onError(msg)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                        .trim()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                        onPartial(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults.coerceIn(1, 5))
                if (preferOffline) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }
            speechRecognizer = recognizer
            recognizer.startListening(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "SpeechRecognizer error", e)
            _isListening.value = false
            val msg = e.localizedMessage ?: "Speech recognition error"
            _lastError.value = msg
            onError(msg)
        }
    }

    fun stopDictation() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    fun release() {
        stopDictation()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }

    private fun mapSpeechError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error. Check microphone permission and try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for speech recognition."
        SpeechRecognizer.ERROR_NO_MATCH -> "Could not recognize clear words. Speak at a steady pace and retry."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard before timeout. Tap Speak and begin talking right away."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Wait a moment and tap Speak again."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Offline speech pack not installed on this device. Install English offline speech in Android settings, or type your sentence below."
        else -> "Speech recognition could not capture clear audio (code $code). Try speaking closer or typing."
    }
}
