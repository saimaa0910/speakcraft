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
private const val MAX_IDLE_ITERATIONS = 400

/**
 * Decodes M4A/WAV/MP4 audio to 16 kHz mono Int16 PCM without arbitrary 30-second truncation.
 * Includes a direct zero-codec RIFF/WAV fast path (reliable in both Android runtime and JVM/Robolectric)
 * plus hardened MediaExtractor/MediaCodec decoding with output format tracking and deadlock guards.
 */
object AudioResampler {

    fun resampleToWhisperFormat(
        inputFile: File,
        maxDurationSeconds: Int = 600
    ): ShortArray? {
        if (!inputFile.exists() || inputFile.length() == 0L) {
            Log.e(TAG, "Input file not found or empty: ${inputFile.absolutePath}")
            return null
        }

        val maxSamples = minOf(
            maxDurationSeconds.coerceAtLeast(1) * TARGET_SAMPLE_RATE,
            MAX_ABSOLUTE_SAMPLES
        )

        // Fast path: direct 16-bit PCM WAV decoding (no MediaCodec overhead or device codec quirks)
        tryDecodeWavDirect(inputFile, maxSamples)?.let { wavPcm ->
            if (wavPcm.isNotEmpty()) return wavPcm
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

            val initialSampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8_000)
            } else TARGET_SAMPLE_RATE

            val initialChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
            } else 1

            val decoded = decode(extractor, format, mime, maxSamples, initialSampleRate, initialChannels)
            if (decoded.samples.isEmpty()) return null
            val monoRaw = if (decoded.channels > 1) toMono(decoded.samples, decoded.channels) else decoded.samples
            resample(monoRaw, decoded.sampleRate, TARGET_SAMPLE_RATE, maxSamples)
        } catch (e: Exception) {
            Log.e(TAG, "Resampling failed: ${e.message}", e)
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    fun decodeInChunks(
        inputFile: File,
        chunkDurationSeconds: Int = 15,
        onChunk: (chunkSamples: ShortArray, isLast: Boolean) -> Unit
    ): Boolean {
        val fullPcm = resampleToWhisperFormat(inputFile) ?: return false
        val chunkSamplesSize = chunkDurationSeconds.coerceAtLeast(1) * TARGET_SAMPLE_RATE
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
    // Direct PCM WAV Reader (works on every device & JVM unit test)
    // -----------------------------------------------------------------------

    internal fun tryDecodeWavDirect(file: File, maxSamples: Int): ShortArray? {
        return runCatching {
            val bytes = file.readBytes()
            if (bytes.size < 44) return null
            if (bytes[0].toInt().toChar() != 'R' || bytes[1].toInt().toChar() != 'I' ||
                bytes[2].toInt().toChar() != 'F' || bytes[3].toInt().toChar() != 'F'
            ) return null
            if (bytes[8].toInt().toChar() != 'W' || bytes[9].toInt().toChar() != 'A' ||
                bytes[10].toInt().toChar() != 'V' || bytes[11].toInt().toChar() != 'E'
            ) return null

            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            var offset = 12
            var audioFormat = 1
            var channels = 1
            var sampleRate = TARGET_SAMPLE_RATE
            var bitsPerSample = 16
            var dataOffset = -1
            var dataSize = 0

            while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
                val chunkSize = bb.getInt(offset + 4).coerceAtLeast(0)
                val chunkStart = offset + 8
                if (chunkId == "fmt " && chunkStart + 16 <= bytes.size) {
                    audioFormat = bb.getShort(chunkStart).toInt() and 0xFFFF
                    channels = (bb.getShort(chunkStart + 2).toInt() and 0xFFFF).coerceIn(1, 8)
                    sampleRate = bb.getInt(chunkStart + 4).coerceAtLeast(8_000)
                    bitsPerSample = bb.getShort(chunkStart + 14).toInt() and 0xFFFF
                } else if (chunkId == "data") {
                    dataOffset = chunkStart
                    dataSize = minOf(chunkSize, bytes.size - chunkStart)
                    break
                }
                offset = chunkStart + chunkSize + (chunkSize % 2)
            }

            if (audioFormat != 1 || bitsPerSample != 16 || dataOffset < 0 || dataSize <= 0) return null

            val totalShorts = dataSize / 2
            val rawSamples = ShortArray(totalShorts)
            val dataBuf = ByteBuffer.wrap(bytes, dataOffset, totalShorts * 2).order(ByteOrder.LITTLE_ENDIAN)
            dataBuf.asShortBuffer().get(rawSamples)
            val mono = if (channels > 1) toMono(rawSamples, channels) else rawSamples
            resample(mono, sampleRate, TARGET_SAMPLE_RATE, maxSamples)
        }.getOrNull()
    }

    // -----------------------------------------------------------------------
    // Private MediaCodec Helpers
    // -----------------------------------------------------------------------

    private data class DecodedPcm(
        val samples: ShortArray,
        val sampleRate: Int,
        val channels: Int
    )

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
    ): DecodedPcm {
        val codec = MediaCodec.createDecoderByType(mime)
        val outputSamples = ArrayList<Short>(minOf(maxOutputSamples, 480_000))
        var actualRate = srcRate
        var actualChannels = channels

        try {
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var idleCycles = 0
            val timeoutUs = 10_000L

            val maxRawSamples = (maxOutputSamples.toDouble() * (srcRate.toDouble() / TARGET_SAMPLE_RATE) * channels)
                .toInt()
                .coerceAtLeast(TARGET_SAMPLE_RATE)

            while (!outputDone && outputSamples.size < maxRawSamples && idleCycles < MAX_IDLE_ITERATIONS) {
                var madeProgress = false

                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val buf: ByteBuffer? = codec.getInputBuffer(inIdx)
                        if (buf != null) {
                            val sampleSize = extractor.readSampleData(buf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                            madeProgress = true
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                when {
                    outIdx >= 0 -> {
                        val buf: ByteBuffer? = codec.getOutputBuffer(outIdx)
                        if (buf != null && info.size > 0) {
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            val slice = buf.slice().order(ByteOrder.nativeOrder())
                            val sb: ShortBuffer = slice.asShortBuffer()
                            while (sb.hasRemaining() && outputSamples.size < maxRawSamples) {
                                outputSamples.add(sb.get())
                            }
                            madeProgress = true
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            actualRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8_000)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            actualChannels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                        }
                        madeProgress = true
                    }
                }

                if (madeProgress) idleCycles = 0 else idleCycles++
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
        return DecodedPcm(outputSamples.toShortArray(), actualRate, actualChannels)
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
        if (input.isEmpty()) return ShortArray(0)
        if (srcRate == dstRate) {
            return if (input.size <= maxSamples) input else input.copyOf(maxSamples)
        }
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val targetSize = minOf((input.size / ratio).toInt(), maxSamples).coerceAtLeast(1)

        return ShortArray(targetSize) { i ->
            val srcPos = i * ratio
            val lo = srcPos.toInt().coerceIn(0, input.size - 1)
            val hi = (lo + 1).coerceIn(0, input.size - 1)
            val frac = srcPos - lo
            ((input[lo] * (1.0 - frac)) + (input[hi] * frac)).toInt().toShort()
        }
    }
}
