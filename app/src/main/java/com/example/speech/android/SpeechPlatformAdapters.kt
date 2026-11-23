package com.example.speech.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.speech.capture.WavWriter
import com.example.speech.recognition.RecognitionError
import com.example.speech.session.AudioSource
import com.example.speech.session.PcmSink
import com.example.speech.session.RecognizerCallbacks
import com.example.speech.session.RecognizerEngine
import java.io.Closeable
import java.io.File
import java.util.Locale

/**
 * `AudioRecord`-backed [AudioSource] for tiers T1 and T3b (CMP-26; ARC-05 §4.1): 16 kHz mono PCM16
 * from the voice-recognition input. Throws from [start] when RECORD_AUDIO is not granted or the
 * microphone cannot be opened — the caller shows the permission or error state (SRS-ERR-001).
 */
class AudioRecordSource(
    private val context: Context,
    override val sampleRateHz: Int = 16_000,
    private val inputSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION
) : AudioSource {
    private var record: AudioRecord? = null

    @SuppressLint("MissingPermission") // checked explicitly below
    override fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("RECORD_AUDIO permission not granted")
        }
        val minBuffer = AudioRecord.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(minBuffer > 0) { "Unsupported audio configuration (getMinBufferSize=$minBuffer)" }
        val r = AudioRecord(inputSource, sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer, sampleRateHz / 2))
        if (r.state != AudioRecord.STATE_INITIALIZED) {
            r.release()
            throw IllegalStateException("AudioRecord could not be initialised")
        }
        r.startRecording()
        if (r.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            r.release()
            throw IllegalStateException("Microphone is busy")
        }
        record = r
    }

    override fun read(buffer: ShortArray, offset: Int, length: Int): Int = record?.read(buffer, offset, length) ?: -1

    override fun stop() { runCatching { record?.stop() } }

    override fun release() { record?.release(); record = null }
}

/** Writes captured PCM to an app-private WAV file for replay and later AAC transcoding (ADR-015). */
class WavFileSink(file: File) : PcmSink {
    private val writer = WavWriter(file)
    override fun onPcm(samples: ShortArray, length: Int) = writer.writeFrame(samples, 0, length)
    override fun close() = writer.close()
}

/**
 * Pipe that carries app-owned PCM into the on-device recogniser (tier T1, `EXTRA_AUDIO_SOURCE`;
 * ARC-05 §4.2). Hand [readSide] to [PlatformRecognizerEngine]; the capture thread writes through
 * [onPcm]. Whether a given recogniser accepts piped audio is verified per device in SPK-01.
 */
class RecognizerPipe : PcmSink, Closeable {
    private val pipe = ParcelFileDescriptor.createPipe()
    val readSide: ParcelFileDescriptor get() = pipe[0]
    private val out = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
    private var bytes = ByteArray(0)

    override fun onPcm(samples: ShortArray, length: Int) {
        if (bytes.size < length * 2) bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            val s = samples[i].toInt()
            bytes[2 * i] = (s and 0xFF).toByte()
            bytes[2 * i + 1] = ((s shr 8) and 0xFF).toByte()
        }
        out.write(bytes, 0, length * 2)
    }

    /** Closing the write side signals end of audio to the recogniser. */
    override fun close() { runCatching { out.close() } }

    fun closeReadSide() { runCatching { pipe[0].close() } }
}

/**
 * `SpeechRecognizer`-backed [RecognizerEngine] (CMP-28). All recogniser calls are posted to the main
 * thread, as the platform requires.
 *
 * - [onDevice] true: on-device recogniser (T1/T2); false: default recogniser, used only for T3a after
 *   the user consented to online transcription (SRS-BR-023).
 * - [audioPipe] non-null (T1, API 33+): app-owned PCM through `EXTRA_AUDIO_SOURCE` with a segmented
 *   session that lasts until the pipe closes.
 *
 * Confidence is passed on only when the recogniser supplies a positive value (SRS-BR-005).
 */
class PlatformRecognizerEngine(
    private val context: Context,
    private val onDevice: Boolean,
    private val locale: Locale = Locale.US,
    private val audioPipe: ParcelFileDescriptor? = null
) : RecognizerEngine {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    override fun startSegment(callbacks: RecognizerCallbacks) {
        main.post {
            try {
                val r = recognizer ?: create().also { recognizer = it }
                r.setRecognitionListener(listener(callbacks))
                r.startListening(intent())
            } catch (e: Exception) {
                callbacks.onError(RecognitionError.Client)
            }
        }
    }

    override fun stopSegment() { main.post { runCatching { recognizer?.stopListening() } } }

    override fun cancel() { main.post { runCatching { recognizer?.cancel() } } }

    override fun release() {
        main.post {
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    private fun create(): SpeechRecognizer =
        if (onDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

    private fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        if (onDevice) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        if (audioPipe != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioPipe)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16_000)
            putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
        }
    }

    private fun listener(cb: RecognizerCallbacks) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) = cb.onRms(rmsdB)
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) = cb.onError(RecognitionError.fromPlatformCode(error))
        override fun onResults(results: Bundle?) {
            val (text, confidence) = best(results)
            cb.onFinalResult(text, confidence)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            best(partialResults).first.takeIf { it.isNotBlank() }?.let(cb::onPartialResult)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onSegmentResults(segmentResults: Bundle) {
            val (text, confidence) = best(segmentResults)
            cb.onFinalResult(text, confidence)
        }
        override fun onEndOfSegmentedSession() = cb.onSessionEnded()
    }

    private fun best(bundle: Bundle?): Pair<String, Float?> {
        val text = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        val confidence = bundle?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()?.takeIf { it > 0f }
        return text to confidence
    }
}

/** Status of the offline language pack for a locale (FS-12; ARC-05 §4.3). */
enum class LanguagePackStatus { INSTALLED, DOWNLOADING, AVAILABLE_TO_DOWNLOAD, NOT_SUPPORTED, UNKNOWN }

/**
 * Offline language-pack status and install for API 33+ (`checkRecognitionSupport`,
 * `triggerModelDownload`; SRS-FR-SPCH-004). Call from the main thread. Below API 33 the status is
 * [LanguagePackStatus.UNKNOWN] and no install is offered.
 */
class LanguagePackManager(private val context: Context) {

    fun checkStatus(locale: Locale, onResult: (LanguagePackStatus) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            onResult(LanguagePackStatus.UNKNOWN); return
        }
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val tag = locale.toLanguageTag()
        recognizer.checkRecognitionSupport(intentFor(locale), ContextCompat.getMainExecutor(context),
            object : android.speech.RecognitionSupportCallback {
                override fun onSupportResult(support: android.speech.RecognitionSupport) {
                    val status = when {
                        support.installedOnDeviceLanguages.any { it.equals(tag, ignoreCase = true) } -> LanguagePackStatus.INSTALLED
                        support.pendingOnDeviceLanguages.any { it.equals(tag, ignoreCase = true) } -> LanguagePackStatus.DOWNLOADING
                        support.supportedOnDeviceLanguages.any { it.equals(tag, ignoreCase = true) } -> LanguagePackStatus.AVAILABLE_TO_DOWNLOAD
                        else -> LanguagePackStatus.NOT_SUPPORTED
                    }
                    recognizer.destroy()
                    onResult(status)
                }
                override fun onError(error: Int) {
                    recognizer.destroy()
                    onResult(LanguagePackStatus.UNKNOWN)
                }
            })
    }

    /** Asks the platform to download the pack; progress is shown by the system. */
    fun triggerDownload(locale: Locale) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        runCatching { recognizer.triggerModelDownload(intentFor(locale)) }
        recognizer.destroy()
    }

    private fun intentFor(locale: Locale) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
    }
}

/**
 * Online-transcription consent (tier T3a; SRS-FR-SPCH-005/006, SRS-BR-023). A device preference:
 * not synced, not exported, unchanged by sign-in or sign-out. Withdrawal takes effect immediately.
 */
class OnlineTranscriptionConsentStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("speech_consent", Context.MODE_PRIVATE)

    val hasConsented: Boolean get() = prefs.getBoolean(KEY, false)

    fun grant() = prefs.edit().putBoolean(KEY, true).putLong(KEY_AT, System.currentTimeMillis()).apply()

    fun revoke() = prefs.edit().putBoolean(KEY, false).remove(KEY_AT).apply()

    private companion object {
        const val KEY = "online_transcription_consent"
        const val KEY_AT = "online_transcription_consent_at"
    }
}
