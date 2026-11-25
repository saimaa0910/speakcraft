package com.example.speech.analysis

import java.io.EOFException
import java.io.InputStream

/**
 * Acoustic evidence from finished recordings (ARC-05 §4.1, §6) — the MediaRecorder/AAC files of the practice
 * labs and clips the user attaches — once they are decoded to PCM.
 *
 * Interleaved PCM of any channel count is down-mixed to mono and, above [ANALYSIS_RATE_HZ], resampled to
 * [ANALYSIS_RATE_HZ] by linear interpolation, so [StreamingFeatureExtractor] (voice activity, pauses, levels,
 * clipping and pitch) runs at the rate its thresholds and pitch tracker were designed for. Streaming: memory
 * does not grow with the recording apart from the extractor's one value per 20 ms frame.
 *
 * Every value comes from the samples; nothing is defaulted (SRS-BR-002). At most [maxDurationMs] of audio is
 * analysed; the rest is ignored and [truncated] reports it.
 */
class PcmFeatureSink(
    sourceRateHz: Int,
    private val channels: Int,
    private val maxDurationMs: Long = MAX_ANALYSED_MS
) {
    init {
        require(sourceRateHz > 0) { "sourceRateHz must be positive" }
        require(channels in 1..8) { "channels must be 1..8" }
    }

    /** Rate the extractor analyses at: [ANALYSIS_RATE_HZ], or the source rate when that is lower. */
    val analysisRateHz: Int = minOf(sourceRateHz, ANALYSIS_RATE_HZ)

    private val extractor = StreamingFeatureExtractor(analysisRateHz)
    private val step = sourceRateHz.toDouble() / analysisRateHz // ≥ 1
    private val maxOutputSamples = maxDurationMs * analysisRateHz / 1000L

    // Down-mix state (a frame of interleaved samples may span two chunks).
    private var channelSum = 0L
    private var channelIndex = 0

    // Resampler state.
    private var monoIndex = 0L
    private var nextPos = 0.0
    private var prev = 0

    private val out = ShortArray(OUT_BUFFER)
    private var outCount = 0
    private var emitted = 0L
    private var finished = false

    /** True when more audio arrived than [maxDurationMs] and the excess was ignored. */
    var truncated: Boolean = false
        private set

    /** Total mono samples emitted to the extractor so far. */
    val analysedSamples: Long get() = emitted + outCount

    /** Feeds [length] interleaved 16-bit samples. */
    fun accept(samples: ShortArray, length: Int = samples.size) {
        check(!finished) { "finish() was already called" }
        require(length in 0..samples.size) { "length out of range" }
        for (i in 0 until length) pushInterleaved(samples[i].toInt())
    }

    /** Feeds [length] interleaved float samples in −1..1 (decoders that output float PCM). */
    fun acceptFloat(samples: FloatArray, length: Int = samples.size) {
        check(!finished) { "finish() was already called" }
        require(length in 0..samples.size) { "length out of range" }
        for (i in 0 until length) {
            val s = (samples[i] * FULL_SCALE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            pushInterleaved(s)
        }
    }

    /** Completes the analysis. */
    fun finish(): AcousticFeatures {
        check(!finished) { "finish() was already called" }
        finished = true
        flush()
        return extractor.finish()
    }

    private fun pushInterleaved(sample: Int) {
        if (channels == 1) {
            pushMono(sample)
            return
        }
        channelSum += sample
        channelIndex++
        if (channelIndex == channels) {
            pushMono((channelSum / channels).toInt())
            channelSum = 0L
            channelIndex = 0
        }
    }

    private fun pushMono(s: Int) {
        if (truncated) return
        if (step == 1.0) {
            emit(s)
        } else {
            val i = monoIndex
            if (i == 0L) {
                while (nextPos <= 0.0) {
                    emit(s)
                    nextPos += step
                }
            } else {
                while (nextPos <= i) {
                    val frac = nextPos - (i - 1)
                    emit((prev + (s - prev) * frac).toInt())
                    nextPos += step
                }
            }
        }
        prev = s
        monoIndex++
    }

    private fun emit(value: Int) {
        if (analysedSamples >= maxOutputSamples) {
            truncated = true
            return
        }
        out[outCount++] = value.toShort()
        if (outCount == out.size) flush()
    }

    private fun flush() {
        if (outCount == 0) return
        extractor.accept(out, outCount)
        emitted += outCount
        outCount = 0
    }

    companion object {
        /** Rate the acoustic thresholds and the pitch tracker were designed for (ARC-05 §4.1). */
        const val ANALYSIS_RATE_HZ = StreamingFeatureExtractor.DEFAULT_SAMPLE_RATE_HZ

        /** Longest stretch analysed: 10 minutes, the practice labs' upper bound (bounds CPU on budget phones). */
        const val MAX_ANALYSED_MS = 10L * 60L * 1000L

        private const val OUT_BUFFER = 4_096
        private const val FULL_SCALE = 32_767.0f
    }
}

/**
 * Reads a RIFF/WAVE file with 16-bit PCM samples (any rate, any channel count) and measures it through
 * [PcmFeatureSink]. Returns `null` when the stream is not such a file, so the caller can try a platform
 * decoder instead. Streaming: the file is never loaded into memory.
 */
object WavPcmReader {

    private const val FORMAT_PCM = 1
    private const val FORMAT_EXTENSIBLE = 0xFFFE
    private const val CHUNK_BYTES = 8_192

    fun analyse(input: InputStream, maxDurationMs: Long = PcmFeatureSink.MAX_ANALYSED_MS): AcousticFeatures? {
        val header = ByteArray(12)
        if (!readFully(input, header)) return null
        if (ascii(header, 0) != "RIFF" || ascii(header, 8) != "WAVE") return null

        var channels = -1
        var sampleRate = -1
        val chunkHeader = ByteArray(8)
        while (readFully(input, chunkHeader)) {
            val id = ascii(chunkHeader, 0)
            val size = le32(chunkHeader, 4)
            when (id) {
                "fmt " -> {
                    if (size < 16 || size > 1024) return null
                    val fmt = ByteArray(size.toInt())
                    if (!readFully(input, fmt)) return null
                    val format = le16(fmt, 0)
                    channels = le16(fmt, 2)
                    sampleRate = le32(fmt, 4).toInt()
                    val bits = le16(fmt, 14)
                    if ((format != FORMAT_PCM && format != FORMAT_EXTENSIBLE) || bits != 16) return null
                    if (channels !in 1..8 || sampleRate !in 4_000..192_000) return null
                    if (size % 2 == 1L) skipFully(input, 1)
                }
                "data" -> {
                    if (channels < 0) return null // data before fmt: not a file we understand
                    val sink = PcmFeatureSink(sampleRate, channels, maxDurationMs)
                    // A size of 0 or 0xFFFFFFFF means "until the end" (header never patched by the writer).
                    val declared = if (size == 0L || size == 0xFFFFFFFFL) Long.MAX_VALUE else size
                    streamSamples(input, declared, sink)
                    if (sink.analysedSamples == 0L) return null
                    return sink.finish()
                }
                else -> {
                    if (!skipFully(input, size + (size % 2))) return null
                }
            }
        }
        return null
    }

    private fun streamSamples(input: InputStream, declaredBytes: Long, sink: PcmFeatureSink) {
        val bytes = ByteArray(CHUNK_BYTES)
        val shorts = ShortArray(CHUNK_BYTES / 2)
        var remaining = declaredBytes
        var carry = -1 // a lone byte left over when a read ends mid-sample
        while (remaining > 0 && !sink.truncated) {
            val want = minOf(remaining, bytes.size.toLong()).toInt()
            val n = input.read(bytes, 0, want)
            if (n <= 0) break
            remaining -= n
            var count = 0
            var i = 0
            if (carry >= 0) {
                shorts[count++] = ((carry and 0xFF) or (bytes[0].toInt() shl 8)).toShort()
                carry = -1
                i = 1
            }
            while (i + 1 < n) {
                shorts[count++] = ((bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)).toShort()
                i += 2
            }
            if (i < n) carry = bytes[i].toInt()
            sink.accept(shorts, count)
        }
    }

    private fun readFully(input: InputStream, buf: ByteArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val n = try {
                input.read(buf, off, buf.size - off)
            } catch (_: EOFException) {
                -1
            }
            if (n <= 0) return false
            off += n
        }
        return true
    }

    private fun skipFully(input: InputStream, count: Long): Boolean {
        var left = count
        val scratch = ByteArray(1_024)
        while (left > 0) {
            val n = input.read(scratch, 0, minOf(left, scratch.size.toLong()).toInt())
            if (n <= 0) return false
            left -= n
        }
        return true
    }

    private fun ascii(b: ByteArray, off: Int) = String(b, off, 4, Charsets.US_ASCII)
    private fun le16(b: ByteArray, off: Int) = (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)
    private fun le32(b: ByteArray, off: Int): Long =
        (le16(b, off).toLong()) or (le16(b, off + 2).toLong() shl 16)
}
