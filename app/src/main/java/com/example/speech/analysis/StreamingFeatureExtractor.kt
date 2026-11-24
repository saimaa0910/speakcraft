package com.example.speech.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Streaming acoustic feature extractor for app-owned PCM (CMP-30; ARC-05 §4.1, §6).
 *
 * Accepts 16-bit mono PCM in chunks of any size (as delivered by `AudioRecord` through `PcmTee`),
 * cuts it into fixed 20 ms frames and, at [finish], computes:
 *
 * - per-frame RMS on a linear 0..1 scale;
 * - an adaptive noise floor (a low percentile of frame levels) and a voice threshold above it;
 * - voice-activity decisions with hangover smoothing (short gaps between words stay speech) and
 *   removal of isolated clicks;
 * - speaking time (M-SPK), long pauses ≥ 1.0 s inside speech (M-PAUSE), voiced-frame levels (M-LEVEL)
 *   and the clipping ratio used by the evidence gate's clipping warning.
 *
 * Pure Kotlin with no Android dependencies, so it is fully testable on the JVM. One instance
 * analyses one attempt; it keeps one Float per frame (~6 000 values for a 2-minute recording).
 * Not thread-safe: feed it from the single capture thread.
 *
 * Thresholds are proposed values (†) to be tuned on the SPK-01 recordings (ARC-05 §4.4).
 */
class StreamingFeatureExtractor(
    private val sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ,
    private val frameMs: Int = DEFAULT_FRAME_MS
) {
    private val frameSamples = sampleRateHz * frameMs / 1000
    private val frameRms = ArrayList<Float>(4096)

    // Pitch per frame (YIN over the last 40 ms), for the candidate vocal-delivery metrics (ARC-05 §8).
    private val pitchTracker = PitchTracker(sampleRateHz)
    private val pitchWindow = FloatArray(pitchTracker.windowSize)
    private val history = FloatArray(pitchTracker.windowSize)
    private var historyPos = 0
    private var historyFilled = 0
    private val framePitch = ArrayList<Float>(4096)
    private val pending = ShortArray(frameSamples)
    private var pendingCount = 0
    private var totalSamples = 0L
    private var clippedSamples = 0L
    private var finished = false

    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(frameSamples > 0) { "frame must contain at least one sample" }
    }

    /** Feeds [length] samples from [samples]. Chunk boundaries do not need to align with frames. */
    fun accept(samples: ShortArray, length: Int = samples.size) {
        check(!finished) { "finish() was already called" }
        require(length in 0..samples.size) { "length out of range" }
        var i = 0
        while (i < length) {
            val take = minOf(frameSamples - pendingCount, length - i)
            System.arraycopy(samples, i, pending, pendingCount, take)
            pendingCount += take
            i += take
            if (pendingCount == frameSamples) {
                consumeFrame(pending, frameSamples)
                pendingCount = 0
            }
        }
    }

    /** Completes the analysis. A trailing partial frame is included if it holds ≥ half a frame. */
    fun finish(): AcousticFeatures {
        check(!finished) { "finish() was already called" }
        finished = true
        if (pendingCount >= frameSamples / 2) consumeFrame(pending, pendingCount)
        val durationMs = totalSamples * 1000L / sampleRateHz
        return VoiceActivity.analyse(
            levels = frameRms.map { it.toDouble() },
            frameMs = frameMs.toLong(),
            durationMs = durationMs,
            source = FeatureSource.PCM,
            clippingRatio = if (totalSamples > 0) clippedSamples.toDouble() / totalSamples else null,
            absoluteMinVoiceLevel = MIN_VOICE_RMS,
            framePitchHz = framePitch.map { it.toDouble() }
        )
    }

    private fun consumeFrame(buf: ShortArray, count: Int) {
        var sumSq = 0.0
        for (k in 0 until count) {
            val s = buf[k].toInt()
            if (abs(s) >= CLIP_LEVEL) clippedSamples++
            sumSq += s.toDouble() * s.toDouble()
            history[historyPos] = (s / FULL_SCALE).toFloat()
            historyPos = (historyPos + 1) % history.size
            if (historyFilled < history.size) historyFilled++
        }
        totalSamples += count
        val rms = sqrt(sumSq / count) / FULL_SCALE
        frameRms.add(rms.toFloat())
        framePitch.add(pitchOfLastWindow(rms))
    }

    /** Pitch of the last 40 ms, or NaN when too quiet to be speech or not periodic. */
    private fun pitchOfLastWindow(rms: Double): Float {
        if (historyFilled < history.size || rms < MIN_VOICE_RMS) return Float.NaN
        for (k in history.indices) pitchWindow[k] = history[(historyPos + k) % history.size]
        return pitchTracker.estimate(pitchWindow)?.f0Hz?.toFloat() ?: Float.NaN
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE_HZ = 16_000
        const val DEFAULT_FRAME_MS = 20

        /** Silences at least this long, inside speech, count as long pauses (ARC-05 §6 M-PAUSE). */
        const val LONG_PAUSE_MS = 1_000L

        /** Gaps up to this long between voiced frames are treated as speech (inter-word gaps). † */
        const val HANGOVER_MS = 200L

        /** Voiced runs shorter than this are discarded as clicks or bumps. † */
        const val MIN_SPEECH_RUN_MS = 60L

        /** Voice threshold = max(noise floor × factor, absolute minimum). Factor 2.5 ≈ +8 dB. † */
        const val VOICE_FACTOR = 2.5

        /** Absolute minimum voiced level on the linear RMS scale (≈ −48 dBFS). † */
        const val MIN_VOICE_RMS = 0.004

        /** Samples at or above this magnitude count as clipped (≈ 99.8 % of full scale). */
        const val CLIP_LEVEL = 32_700

        private const val FULL_SCALE = 32_768.0

        /** Convenience for whole-buffer analysis (tests, WAV re-analysis). */
        fun analyse(samples: ShortArray, sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ): AcousticFeatures =
            StreamingFeatureExtractor(sampleRateHz).apply { accept(samples) }.finish()
    }
}

/**
 * Voice-activity decisions shared by the PCM and RMS-callback extractors. Works on a level series
 * where each value covers [frameMs] of audio.
 */
internal object VoiceActivity {

    /** Percentile of frame levels used as the noise-floor estimate. † */
    private const val NOISE_PERCENTILE = 0.10

    /** Length of the quietest stretch also considered for the noise floor. † */
    private const val QUIET_WINDOW_MS = 200L

    private fun quietestWindowMean(levels: List<Double>, window: Int): Double {
        if (levels.size <= window) return levels.average()
        var sum = 0.0
        for (i in 0 until window) sum += levels[i]
        var best = sum
        for (i in window until levels.size) {
            sum += levels[i] - levels[i - window]
            if (sum < best) best = sum
        }
        return best / window
    }

    fun analyse(
        levels: List<Double>,
        frameMs: Long,
        durationMs: Long,
        source: FeatureSource,
        clippingRatio: Double?,
        absoluteMinVoiceLevel: Double,
        /** Per-frame pitch (NaN = none) from PCM; `null` for recogniser levels, which carry no pitch. */
        framePitchHz: List<Double>? = null
    ): AcousticFeatures {
        if (levels.isEmpty()) {
            return AcousticFeatures(source, durationMs, 0L, emptyList(), emptyList(), 0.0, clippingRatio, null, null)
        }
        // Noise floor: the lower of a low percentile and the quietest ~200 ms stretch. The percentile
        // alone lands on speech when under 10 % of the recording is silent (a speaker who starts within
        // a fraction of a second and talks continuously); the quietest stretch still finds the lead-in.
        val noiseFloor = minOf(percentile(levels, NOISE_PERCENTILE), quietestWindowMean(levels, frames(QUIET_WINDOW_MS, frameMs)))
        val threshold = max(noiseFloor * StreamingFeatureExtractor.VOICE_FACTOR, absoluteMinVoiceLevel)

        val voiced = BooleanArray(levels.size) { levels[it] > threshold }
        fillShortGaps(voiced, frames(StreamingFeatureExtractor.HANGOVER_MS, frameMs))
        dropShortRuns(voiced, frames(StreamingFeatureExtractor.MIN_SPEECH_RUN_MS, frameMs))

        val vocal = framePitchHz?.let { VocalDeliveryAnalyzer.analyse(levels, it, voiced, frameMs) }
        val first = voiced.indexOfFirst { it }
        if (first < 0) {
            return AcousticFeatures(source, durationMs, 0L, emptyList(), emptyList(), noiseFloor, clippingRatio, null, null, vocal)
        }
        val last = voiced.indexOfLast { it }

        val active = ArrayList<Double>()
        var voicedFrames = 0
        val pauses = ArrayList<PauseSpan>()
        var gapStart = -1
        for (i in first..last) {
            if (voiced[i]) {
                voicedFrames++
                active.add(levels[i])
                if (gapStart >= 0) {
                    val gapMs = (i - gapStart) * frameMs
                    if (gapMs >= StreamingFeatureExtractor.LONG_PAUSE_MS) {
                        pauses.add(PauseSpan(gapStart * frameMs, i * frameMs))
                    }
                    gapStart = -1
                }
            } else if (gapStart < 0) {
                gapStart = i
            }
        }
        return AcousticFeatures(
            source = source,
            durationMs = durationMs,
            voicedDurationMs = minOf(voicedFrames * frameMs, durationMs),
            pauses = pauses,
            activeFrameRms = active,
            noiseFloorRms = noiseFloor,
            clippingRatio = clippingRatio,
            firstVoicedMs = first * frameMs,
            lastVoicedMs = minOf((last + 1) * frameMs, durationMs),
            vocal = vocal
        )
    }

    private fun frames(ms: Long, frameMs: Long): Int = ((ms + frameMs - 1) / frameMs).toInt()

    /** Marks unvoiced runs of at most [maxFrames] that lie between voiced frames as voiced. */
    private fun fillShortGaps(v: BooleanArray, maxFrames: Int) {
        var i = 0
        while (i < v.size) {
            if (!v[i]) {
                val start = i
                while (i < v.size && !v[i]) i++
                val bounded = start > 0 && i < v.size
                if (bounded && i - start <= maxFrames) for (k in start until i) v[k] = true
            } else i++
        }
    }

    /** Clears voiced runs shorter than [minFrames]. */
    private fun dropShortRuns(v: BooleanArray, minFrames: Int) {
        var i = 0
        while (i < v.size) {
            if (v[i]) {
                val start = i
                while (i < v.size && v[i]) i++
                if (i - start < minFrames) for (k in start until i) v[k] = false
            } else i++
        }
    }

    private fun percentile(values: List<Double>, p: Double): Double {
        val sorted = values.sorted()
        val idx = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }
}
