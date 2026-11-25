package com.example.speech.android

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.core.logging.AppLogger
import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.PcmFeatureSink
import com.example.speech.analysis.WavPcmReader
import java.io.File
import java.nio.ByteOrder

/**
 * Measures acoustic evidence from a finished recording file for the local intelligence engine (ARC-05 §4.1, §6):
 * the practice labs' MediaRecorder/AAC recordings, CameraX videos and clips the user attaches.
 *
 * 16-bit PCM WAV files are read directly ([WavPcmReader]); anything else is decoded on the device with the
 * platform `MediaCodec` and streamed into [PcmFeatureSink] chunk by chunk, so memory stays flat however long
 * the recording is. Returns `null` when the file has no readable audio — the caller must then report the
 * attempt as not evaluated rather than guess (SRS-BR-002). Call off the main thread.
 */
object RecordedAudioAnalyzer {

    private const val TAG = "RecordedAudioAnalyzer"
    private const val TIMEOUT_US = 10_000L
    private const val MAX_IDLE_ITERATIONS = 400

    fun analyse(file: File): AcousticFeatures? {
        if (!file.isFile || file.length() == 0L) return null
        val wav = runCatching { file.inputStream().buffered().use { WavPcmReader.analyse(it) } }.getOrNull()
        if (wav != null) return wav
        return runCatching { decodeWithMediaCodec(file) }
            .onFailure { AppLogger.w(TAG, "Recording could not be decoded for analysis: ${it.javaClass.simpleName}") }
            .getOrNull()
    }

    private fun decodeWithMediaCodec(file: File): AcousticFeatures? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            var rate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT

            val codec = MediaCodec.createDecoderByType(mime)
            var sink: PcmFeatureSink? = null
            var shorts = ShortArray(0)
            var floats = FloatArray(0)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                var idle = 0
                while (!outputDone && idle < MAX_IDLE_ITERATIONS && sink?.truncated != true) {
                    var progressed = false
                    if (!inputDone) {
                        val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIdx >= 0) {
                            val buf = codec.getInputBuffer(inIdx)
                            if (buf != null) {
                                val size = extractor.readSampleData(buf, 0)
                                if (size < 0) {
                                    codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    inputDone = true
                                } else {
                                    codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                                progressed = true
                            }
                        }
                    }
                    val outIdx = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                    when {
                        outIdx >= 0 -> {
                            val buf = codec.getOutputBuffer(outIdx)
                            if (buf != null && info.size > 0) {
                                val r = rate ?: return null
                                val c = channels ?: return null
                                val s = sink ?: PcmFeatureSink(r, c).also { sink = it }
                                buf.position(info.offset)
                                buf.limit(info.offset + info.size)
                                val ordered = buf.slice().order(ByteOrder.nativeOrder())
                                when (encoding) {
                                    AudioFormat.ENCODING_PCM_16BIT -> {
                                        val sb = ordered.asShortBuffer()
                                        val n = sb.remaining()
                                        if (shorts.size < n) shorts = ShortArray(n)
                                        sb.get(shorts, 0, n)
                                        s.accept(shorts, n)
                                    }
                                    AudioFormat.ENCODING_PCM_FLOAT -> {
                                        val fb = ordered.asFloatBuffer()
                                        val n = fb.remaining()
                                        if (floats.size < n) floats = FloatArray(n)
                                        fb.get(floats, 0, n)
                                        s.acceptFloat(floats, n)
                                    }
                                    else -> return null // an encoding we cannot measure honestly
                                }
                                progressed = true
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val out = codec.outputFormat
                            val newRate = out.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: rate
                            val newChannels = out.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: channels
                            // A format change after samples were measured would mix two scales: give up instead.
                            if (sink != null && (newRate != rate || newChannels != channels)) return null
                            rate = newRate
                            channels = newChannels
                            encoding = out.intOrNull(MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
                            progressed = true
                        }
                    }
                    idle = if (progressed) 0 else idle + 1
                }
            } finally {
                runCatching { codec.stop() }
                runCatching { codec.release() }
            }
            val measured = sink ?: return null
            if (measured.analysedSamples == 0L) return null
            return measured.finish()
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun MediaFormat.intOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull()?.takeIf { it > 0 } else null
}
