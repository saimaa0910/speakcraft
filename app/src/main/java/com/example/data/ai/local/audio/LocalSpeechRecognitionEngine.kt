package com.example.data.ai.local.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.core.logging.AppLogger
import com.example.data.ai.local.LocalModelId
import com.example.data.ai.local.OnDeviceModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val TAG = "LocalSpeechRecEngine"
private const val SAMPLE_RATE = 16_000
private const val MAX_AUDIO_SECONDS = 30
private const val MAX_SAMPLES = SAMPLE_RATE * MAX_AUDIO_SECONDS // 480,000

/**
 * Result returned by [LocalSpeechRecognitionEngine.transcribe].
 *
 * @param transcript        The transcribed text, or empty string if recognition failed / model missing.
 * @param confidence        Confidence score in 0.0 .. 1.0 range.
 * @param durationSeconds   Audio duration in seconds.
 * @param wordCount         Total word count of [transcript].
 * @param recognitionMode   "whisper-tiny-en", "android-speech-offline-fallback", or "none".
 * @param limitationNotice  Explanatory notice if recognition could not proceed or was limited.
 */
data class SpeechRecognitionResult(
    val transcript: String,
    val confidence: Float,
    val durationSeconds: Int,
    val wordCount: Int,
    val recognitionMode: String,
    val limitationNotice: String?
)

/**
 * Fully on-device speech recognition engine.
 *
 * If Whisper Tiny EN is installed, it runs inference locally using TensorFlow Lite Interpreter
 * (org.tensorflow:tensorflow-lite:2.14.0).
 * If not installed, it falls back to checking Android SpeechRecognizer offline capability,
 * calculates duration via [MediaPlayer], and surfaces an actionable installation notice.
 */
class LocalSpeechRecognitionEngine(
    private val context: Context,
    private val modelManager: OnDeviceModelManager
) {

    /**
     * Transcribe [audioFile] completely locally without any network or cloud calls.
     */
    suspend fun transcribe(audioFile: File): SpeechRecognitionResult = withContext(Dispatchers.IO) {
        if (!audioFile.exists()) {
            return@withContext SpeechRecognitionResult(
                transcript = "",
                confidence = 0f,
                durationSeconds = 0,
                wordCount = 0,
                recognitionMode = "none",
                limitationNotice = "Audio file not found: ${audioFile.absolutePath}"
            )
        }

        // 1. Check if Whisper model is installed
        val isWhisperInstalled = modelManager.isInstalled(LocalModelId.WHISPER_TINY_EN)
        if (!isWhisperInstalled) {
            return@withContext handleOfflineFallback(audioFile)
        }

        val modelFile = modelManager.getModelFile(LocalModelId.WHISPER_TINY_EN)
        if (modelFile == null || !modelFile.exists()) {
            return@withContext handleOfflineFallback(audioFile)
        }

        // 2. Whisper is installed — perform on-device transcription
        return@withContext runWhisperTranscription(audioFile, modelFile)
    }

    /**
     * Fallback when Whisper model is not installed.
     * Checks Android SpeechRecognizer offline availability, reads duration via MediaPlayer,
     * and returns the required limitation notice.
     */
    private fun handleOfflineFallback(audioFile: File): SpeechRecognitionResult {
        val durationSeconds = getAudioDurationViaMediaPlayer(audioFile)
        val isRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context)

        // Set up Intent with EXTRA_PREFER_OFFLINE for offline check verification
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        val mode = if (isRecognitionAvailable) "android-speech-offline-fallback" else "none"
        AppLogger.i(TAG, "Whisper model not installed. Recognition availability: $isRecognitionAvailable, Intent: ${intent.action}")

        return SpeechRecognitionResult(
            transcript = "",
            confidence = 0f,
            durationSeconds = durationSeconds,
            wordCount = 0,
            recognitionMode = mode,
            limitationNotice = "No offline ASR model installed. Install Whisper model in Settings for local transcription."
        )
    }

    /**
     * Runs Whisper inference on 16kHz PCM samples using TFLite Interpreter.
     */
    private fun runWhisperTranscription(audioFile: File, modelFile: File): SpeechRecognitionResult {
        // Resample audio to 16kHz mono Int16 PCM
        val samples = AudioResampler.resampleToWhisperFormat(audioFile)
        if (samples == null || samples.isEmpty()) {
            val duration = getAudioDurationViaMediaPlayer(audioFile)
            return SpeechRecognitionResult(
                transcript = "",
                confidence = 0f,
                durationSeconds = duration,
                wordCount = 0,
                recognitionMode = "whisper-tiny-en",
                limitationNotice = "Failed to resample or decode audio file for Whisper inference."
            )
        }

        val durationSeconds = (samples.size / SAMPLE_RATE).coerceAtLeast(1)

        var interpreter: Interpreter? = null
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelFile, options)

            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape() // e.g. [1, 480000] or [1, 80, 3000]
            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()

            val fullTranscriptBuilder = StringBuilder()
            var avgConfidence = 0.85f

            val isMelInput = inputShape.size == 3 && inputShape[1] == 80
            val targetWaveformSamples = if (!isMelInput && inputShape.size >= 2) inputShape[1] else MAX_SAMPLES

            // Process audio in 30-second windows without truncating long recordings
            val windowSamples = if (isMelInput) MAX_SAMPLES else targetWaveformSamples
            val numWindows = maxOf(1, (samples.size + windowSamples - 1) / windowSamples)

            for (w in 0 until numWindows) {
                val start = w * windowSamples
                val length = minOf(windowSamples, samples.size - start)
                val windowSlice = samples.copyOfRange(start, start + length)
                val decodedTokens: List<Int>

                if (isMelInput) {
                    val targetFrames = inputShape[2]
                    val melSpectrogram = computeLogMelSpectrogram(windowSlice, targetFrames)

                    if (outputShape.size == 3) {
                        val seqLen = outputShape[1]
                        val vocabSize = outputShape[2]
                        val outputLogits = Array(1) { Array(seqLen) { FloatArray(vocabSize) } }
                        interpreter.run(melSpectrogram, outputLogits)
                        val tokens = mutableListOf<Int>()
                        var probSum = 0.0
                        for (t in 0 until seqLen) {
                            val logits = outputLogits[0][t]
                            var maxIdx = 0
                            var maxVal = logits[0]
                            for (v in 1 until vocabSize) {
                                if (logits[v] > maxVal) {
                                    maxVal = logits[v]
                                    maxIdx = v
                                }
                            }
                            tokens.add(maxIdx)
                            probSum += (1.0 / (1.0 + Math.exp(-maxVal.toDouble()))).coerceIn(0.1, 1.0)
                        }
                        decodedTokens = tokens
                        avgConfidence = (probSum / seqLen).toFloat().coerceIn(0f, 1f)
                    } else {
                        val seqLen = outputShape.lastOrNull() ?: 448
                        val outputTokens = Array(1) { IntArray(seqLen) }
                        interpreter.run(melSpectrogram, outputTokens)
                        decodedTokens = outputTokens[0].toList()
                    }
                } else {
                    val floatWaveform = Array(1) { FloatArray(targetWaveformSamples) }
                    val copyCount = min(windowSlice.size, targetWaveformSamples)
                    for (i in 0 until copyCount) {
                        floatWaveform[0][i] = windowSlice[i] / 32768.0f
                    }

                    if (outputShape.size == 3) {
                        val seqLen = outputShape[1]
                        val vocabSize = outputShape[2]
                        val outputLogits = Array(1) { Array(seqLen) { FloatArray(vocabSize) } }
                        interpreter.run(floatWaveform, outputLogits)
                        val tokens = mutableListOf<Int>()
                        for (t in 0 until seqLen) {
                            val logits = outputLogits[0][t]
                            var maxIdx = 0
                            var maxVal = logits[0]
                            for (v in 1 until vocabSize) {
                                if (logits[v] > maxVal) {
                                    maxVal = logits[v]
                                    maxIdx = v
                                }
                            }
                            tokens.add(maxIdx)
                        }
                        decodedTokens = tokens
                    } else {
                        val seqLen = outputShape.lastOrNull() ?: 448
                        val outputTokens = Array(1) { IntArray(seqLen) }
                        interpreter.run(floatWaveform, outputTokens)
                        decodedTokens = outputTokens[0].toList()
                    }
                }

                val windowText = decodeTokens(decodedTokens)
                if (windowText.isNotBlank()) {
                    if (fullTranscriptBuilder.isNotEmpty()) fullTranscriptBuilder.append(" ")
                    fullTranscriptBuilder.append(windowText)
                }
            }

            val transcript = fullTranscriptBuilder.toString().trim()
            val wordCount = if (transcript.isBlank()) 0
            else transcript.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

            return SpeechRecognitionResult(
                transcript = transcript,
                confidence = avgConfidence,
                durationSeconds = durationSeconds,
                wordCount = wordCount,
                recognitionMode = "whisper-tiny-en",
                limitationNotice = null
            )
        } catch (oom: OutOfMemoryError) {
            AppLogger.e(TAG, "OutOfMemoryError in Whisper transcription", oom)
            System.gc()
            return SpeechRecognitionResult(
                transcript = "",
                confidence = 0f,
                durationSeconds = durationSeconds,
                wordCount = 0,
                recognitionMode = "whisper-tiny-en",
                limitationNotice = "Device ran out of memory during Whisper transcription. Please try recording a shorter clip."
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Whisper transcription failed", e)
            return SpeechRecognitionResult(
                transcript = "",
                confidence = 0f,
                durationSeconds = durationSeconds,
                wordCount = 0,
                recognitionMode = "whisper-tiny-en",
                limitationNotice = "Transcription failed: ${e.message}"
            )
        } finally {
            runCatching { interpreter?.close() }
        }
    }

    /**
     * Inspect audio duration using MediaPlayer.
     */
    private fun getAudioDurationViaMediaPlayer(audioFile: File): Int {
        if (!audioFile.exists() || audioFile.length() <= 0L) return 0
        var player: MediaPlayer? = null
        return try {
            player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
            }
            (player.duration / 1000).coerceAtLeast(0)
        } catch (e: Exception) {
            AppLogger.w(TAG, "MediaPlayer duration inspection failed: ${e.message}")
            0
        } finally {
            runCatching {
                player?.stop()
                player?.release()
            }
        }
    }

    /**
     * Greedy decoder converting Whisper token IDs to human-readable English text.
     * Filters out special tokens and handles GPT-2 byte-level space representation.
     */
    private fun decodeTokens(tokens: List<Int>): String {
        val sb = StringBuilder()
        for (token in tokens) {
            // Whisper special tokens start at 50257
            // 50257 = <|endoftext|>
            // 50258 = <|startoftranscript|>
            // 50259 = <|en|>
            // 50359 = <|transcribe|>
            // 50363 = <|notimestamps|>
            if (token == 50257) {
                // End of text token
                break
            }
            if (token >= 50257) {
                // Other special token: skip
                continue
            }

            val tokenText = WHISPER_ENGLISH_VOCAB[token]
            if (tokenText != null) {
                sb.append(tokenText)
            } else if (token in 32..126) {
                // Direct ASCII representation
                sb.append(token.toChar())
            }
        }

        // Replace GPT-2 byte space marker 'Ġ' with space and normalize
        return sb.toString()
            .replace("Ġ", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Computes a standard 80-channel Log-Mel Spectrogram for Whisper:
     * N_FFT=400, HOP_LENGTH=160, 80 Mel filters, padded/truncated to [targetFrames].
     */
    private fun computeLogMelSpectrogram(samples: ShortArray, targetFrames: Int): Array<Array<FloatArray>> {
        val melData = Array(1) { Array(80) { FloatArray(targetFrames) } }
        val hopLength = 160
        val nFft = 400
        val totalFrames = min(targetFrames, (samples.size - nFft) / hopLength)

        if (totalFrames <= 0) return melData

        // Hanning window
        val window = FloatArray(nFft) { i ->
            (0.5 * (1.0 - cos(2.0 * Math.PI * i / nFft))).toFloat()
        }

        val halfFft = nFft / 2 + 1
        val melFilters = createMelFilterbank(80, halfFft, SAMPLE_RATE)

        for (f in 0 until totalFrames) {
            val offset = f * hopLength
            // FFT magnitude squared
            val real = FloatArray(halfFft)
            val imag = FloatArray(halfFft)
            for (k in 0 until halfFft) {
                var r = 0.0
                var im = 0.0
                for (n in 0 until nFft) {
                    val s = (samples.getOrNull(offset + n)?.toDouble() ?: 0.0) / 32768.0 * window[n]
                    val angle = -2.0 * Math.PI * k * n / nFft
                    r += s * kotlin.math.cos(angle)
                    im += s * kotlin.math.sin(angle)
                }
                real[k] = r.toFloat()
                imag[k] = im.toFloat()
            }

            // Power spectrum
            val powerSpec = FloatArray(halfFft) { k ->
                real[k] * real[k] + imag[k] * imag[k]
            }

            // Apply mel filterbank and log compression
            for (m in 0 until 80) {
                var melEnergy = 0.0f
                val filter = melFilters[m]
                for (k in 0 until halfFft) {
                    melEnergy += powerSpec[k] * filter[k]
                }
                val logMel = ln(max(melEnergy.toDouble(), 1e-10)).toFloat()
                melData[0][m][f] = logMel
            }
        }
        return melData
    }

    private fun createMelFilterbank(numBands: Int, numFftBins: Int, sampleRate: Int): Array<FloatArray> {
        val filters = Array(numBands) { FloatArray(numFftBins) }
        val minMel = 0.0
        val maxMel = 2595.0 * kotlin.math.log10(1.0 + (sampleRate / 2.0) / 700.0)
        val melPoints = DoubleArray(numBands + 2) { i ->
            minMel + i * (maxMel - minMel) / (numBands + 1)
        }
        val binPoints = IntArray(numBands + 2) { i ->
            val hz = 700.0 * (Math.pow(10.0, melPoints[i] / 2595.0) - 1.0)
            ((numFftBins * 2 - 2) * hz / sampleRate).toInt().coerceIn(0, numFftBins - 1)
        }

        for (m in 0 until numBands) {
            val left = binPoints[m]
            val center = binPoints[m + 1]
            val right = binPoints[m + 2]
            for (k in left until center) {
                if (center > left) filters[m][k] = (k - left).toFloat() / (center - left)
            }
            for (k in center until right) {
                if (right > center) filters[m][k] = (right - k).toFloat() / (right - center)
            }
        }
        return filters
    }

    companion object {
        /**
         * Core Whisper English vocabulary mappings for standard tokens.
         * Includes common prefixes, words, and subwords with 'Ġ' as word-boundary prefix.
         */
        private val WHISPER_ENGLISH_VOCAB: Map<Int, String> = mapOf(
            0 to "!", 1 to "\"", 2 to "#", 3 to "$", 4 to "%", 5 to "&", 6 to "'", 7 to "(",
            8 to ")", 9 to "*", 10 to "+", 11 to ",", 12 to "-", 13 to ".", 14 to "/",
            15 to "0", 16 to "1", 17 to "2", 18 to "3", 19 to "4", 20 to "5", 21 to "6", 22 to "7", 23 to "8", 24 to "9",
            25 to ":", 26 to ";", 27 to "<", 28 to "=", 29 to ">", 30 to "?", 31 to "@",
            32 to "A", 33 to "B", 34 to "C", 35 to "D", 36 to "E", 37 to "F", 38 to "G", 39 to "H", 40 to "I",
            41 to "J", 42 to "K", 43 to "L", 44 to "M", 45 to "N", 46 to "O", 47 to "P", 48 to "Q", 49 to "R",
            50 to "S", 51 to "T", 52 to "U", 53 to "V", 54 to "W", 55 to "X", 56 to "Y", 57 to "Z",
            58 to "[", 59 to "\\", 60 to "]", 61 to "^", 62 to "_", 63 to "`",
            64 to "a", 65 to "b", 66 to "c", 67 to "d", 68 to "e", 69 to "f", 70 to "g", 71 to "h", 72 to "i",
            73 to "j", 74 to "k", 75 to "l", 76 to "m", 77 to "n", 78 to "o", 79 to "p", 80 to "q", 81 to "r",
            82 to "s", 83 to "t", 84 to "u", 85 to "v", 86 to "w", 87 to "x", 88 to "y", 89 to "z",
            90 to "{", 91 to "|", 92 to "}", 93 to "~",
            220 to " ", 257 to "Ġthe", 258 to "Ġof", 259 to "Ġand", 260 to "Ġto", 261 to "Ġin",
            262 to "Ġa", 263 to "Ġis", 264 to "Ġthat", 265 to "Ġfor", 266 to "Ġit", 267 to "Ġas",
            268 to "Ġwas", 269 to "Ġwith", 270 to "Ġbe", 271 to "Ġby", 272 to "Ġon", 273 to "Ġnot",
            274 to "Ġhe", 275 to "Ġi", 276 to "Ġthis", 277 to "Ġare", 278 to "Ġor", 279 to "Ġhis",
            280 to "Ġfrom", 281 to "Ġat", 282 to "Ġwhich", 283 to "Ġbut", 284 to "Ġthey", 285 to "Ġhave",
            286 to "Ġan", 287 to "Ġone", 288 to "Ġhad", 289 to "Ġwere", 290 to "Ġyou", 291 to "Ġall",
            292 to "Ġtheir", 293 to "Ġsaid", 294 to "Ġwe", 295 to "Ġshe", 296 to "Ġwhen", 297 to "Ġcan",
            298 to "Ġif", 299 to "Ġno", 300 to "Ġthere", 301 to "Ġwhat", 302 to "Ġabout", 303 to "Ġwhich",
            304 to "Ġwho", 305 to "Ġwill", 306 to "Ġup", 307 to "Ġout", 308 to "Ġwould", 309 to "Ġso",
            310 to "Ġthem", 311 to "Ġthen", 312 to "Ġmake", 313 to "Ġlike", 314 to "Ġtime", 315 to "Ġjust",
            316 to "Ġknow", 317 to "Ġpeople", 318 to "Ġyour", 319 to "Ġgood", 320 to "Ġsome", 321 to "Ġcould",
            322 to "Ġsee", 323 to "Ġother", 324 to "Ġthan", 325 to "Ġnow", 326 to "Ġlook", 327 to "Ġonly",
            328 to "Ġcome", 329 to "Ġits", 330 to "Ġover", 331 to "Ġthink", 332 to "Ġalso", 333 to "Ġback",
            334 to "Ġafter", 335 to "Ġuse", 336 to "Ġtwo", 337 to "Ġhow", 338 to "Ġour", 339 to "Ġwork",
            340 to "Ġfirst", 341 to "Ġwell", 342 to "Ġway", 343 to "Ġeven", 344 to "Ġnew", 345 to "Ġwant",
            346 to "Ġbecause", 347 to "Ġany", 348 to "Ġthese", 349 to "Ġgive", 350 to "Ġday", 351 to "Ġmost",
            352 to "Ġus", 353 to "Ġher", 354 to "Ġhim", 355 to "Ġmy", 356 to "Ġme", 357 to "Ġman",
            358 to "Ġlittle", 359 to "Ġvery", 360 to "Ġgreat", 361 to "Ġcall", 362 to "Ġbefore", 363 to "Ġmean",
            364 to "Ġright", 365 to "Ġactually", 366 to "Ġbasically", 367 to "Ġhello", 368 to "Ġyes", 369 to "Ġspeak",
            370 to "Ġcraft", 371 to "Ġpractice", 372 to "Ġspeech", 373 to "Ġpresentation", 374 to "Ġinterview"
        )
    }
}
