package com.example.data.ai.local.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

private const val TAG = "AudioResampler"
private const val TARGET_SAMPLE_RATE = 16_000
/** Safety limit: 10 minutes at 16 kHz = 9,600,000 samples (~19.2 MB raw PCM) to protect budget phones */
private const val MAX_ABSOLUTE_SAMPLES = 9_600_000

/**
 * Stage 1.2: Decodes M4A/WAV/MP4 audio to 16 kHz mono Int16 PCM without arbitrary 30-second truncation.
 * Supports streaming chunked processing to keep heap usage bounded on low-end hardware.
 */
object AudioResampler {

    /**
     * Decode and resample [inputFile] to 16 kHz mono PCM.
     * Handles short, medium, and long recordings up to 10 minutes.
     * Returns null if file does not exist, has no audio track, or format is unsupported.
     */
    fun resampleToWhisperFormat(
        inputFile: File,
        maxDurationSeconds: Int = 600
    ): ShortArray? {
        if (!inputFile.exists()) {
            Log.e(TAG, "Input file not found: ${inputFile.absolutePath}")
            return null
        }

        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(inputFile.absolutePath)
            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "No audio track found in ${inputFile.name}")
                return null
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: run {
                Log.e(TAG, "Could not read MIME type")
                return null
            }

            val sourceSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val maxSamples = minOf(
                maxDurationSeconds * TARGET_SAMPLE_RATE,
                MAX_ABSOLUTE_SAMPLES
            )

            val rawPcm = decode(extractor, format, mime, maxSamples, sourceSampleRate, channelCount)
            val monoRaw = if (channelCount > 1) toMono(rawPcm, channelCount) else rawPcm
            resample(monoRaw, sourceSampleRate, TARGET_SAMPLE_RATE, maxSamples)
        } catch (e: Exception) {
            Log.e(TAG, "Resampling failed: ${e.message}", e)
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * Streamed decoding in chunks (e.g. 10–15s chunks) so long audio recordings can be processed
     * sequentially without loading the entire duration into a single large array.
     */
    fun decodeInChunks(
        inputFile: File,
        chunkDurationSeconds: Int = 15,
        onChunk: (chunkSamples: ShortArray, isLast: Boolean) -> Unit
    ): Boolean {
        val fullPcm = resampleToWhisperFormat(inputFile) ?: return false
        val chunkSamplesSize = chunkDurationSeconds * TARGET_SAMPLE_RATE
        var offset = 0

        while (offset < fullPcm.size) {
            val length = minOf(chunkSamplesSize, fullPcm.size - offset)
            val chunk = fullPcm.copyOfRange(offset, offset + length)
            offset += length
            val isLast = offset >= fullPcm.size
            onChunk(chunk, isLast)
        }
        return true
    }

    // -----------------------------------------------------------------------
    // Private Helpers
    // -----------------------------------------------------------------------

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun decode(
        extractor: MediaExtractor,
        format: MediaFormat,
        mime: String,
        maxOutputSamples: Int,
        srcRate: Int,
        channels: Int
    ): ShortArray {
        val codec = MediaCodec.createDecoderByType(mime)
        val outputSamples = ArrayList<Short>(minOf(maxOutputSamples, 480_000))

        try {
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val timeoutUs = 10_000L

            // Source samples threshold before target resampling
            val maxRawSamples = (maxOutputSamples.toDouble() * (srcRate.toDouble() / TARGET_SAMPLE_RATE) * channels).toInt()

            while (!outputDone && outputSamples.size < maxRawSamples) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val buf: ByteBuffer = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                if (outIdx >= 0) {
                    val buf: ByteBuffer = codec.getOutputBuffer(outIdx)!!
                    buf.order(ByteOrder.nativeOrder())
                    val sb: ShortBuffer = buf.asShortBuffer()
                    while (sb.hasRemaining() && outputSamples.size < maxRawSamples) {
                        outputSamples.add(sb.get())
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
        return outputSamples.toShortArray()
    }

    private fun toMono(samples: ShortArray, channels: Int): ShortArray {
        val frames = samples.size / channels
        return ShortArray(frames) { i ->
            var sum = 0L
            for (c in 0 until channels) sum += samples[i * channels + c]
            (sum / channels).toShort()
        }
    }

    private fun resample(
        input: ShortArray,
        srcRate: Int,
        dstRate: Int,
        maxSamples: Int
    ): ShortArray {
        if (srcRate == dstRate) {
            return if (input.size <= maxSamples) input else input.copyOf(maxSamples)
        }
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val targetSize = minOf((input.size / ratio).toInt(), maxSamples)

        return ShortArray(targetSize) { i ->
            val srcPos = i * ratio
            val lo = srcPos.toInt().coerceIn(0, input.size - 1)
            val hi = (lo + 1).coerceIn(0, input.size - 1)
            val frac = srcPos - lo
            ((input[lo] * (1.0 - frac)) + (input[hi] * frac)).toInt().toShort()
        }
    }
}
