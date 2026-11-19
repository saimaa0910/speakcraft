package com.example.data.ai.local.audio

import android.util.Log
import java.io.File
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

private const val TAG = "AudioFeatureEngine"
private const val SAMPLE_RATE_DEFAULT = 16_000

/**
 * Complete acoustic feature bundle computed entirely on-device without cloud AI.
 *
 * **Scientific Integrity Invariant:** Pitch and loudness reflect vocal dynamics and projection.
 * They do NOT measure psychological confidence, honesty, or personality.
 */
data class AcousticFeatures(
    val durationSeconds: Double = 0.0,
    val speechDurationSeconds: Double = 0.0,
    val silenceDurationSeconds: Double = 0.0,
    val speechSilenceRatio: Double = 0.0,
    val wordCount: Int = 0,
    val sentenceCount: Int = 0,
    val wordsPerMinute: Int = 0,
    val articulationRateEstimate: Double = 0.0,
    val meanPitchHz: Double = 0.0,
    val pitchVariation: Double = 0.0,
    val pitchVariationHz: Double = pitchVariation,
    val rmsEnergy: Float = 0f,
    val relativeLoudness: Double = -90.0,
    val rmsVolumeDb: Double = relativeLoudness,
    val clippingDetected: Boolean = false,
    val backgroundNoiseLevel: Float = 0f,
    val pauseCount: Int = 0,
    val meanPauseDurationMs: Double = 0.0,
    val speechContinuityScore: Double = 0.0,
    val volumeVariation: Double = 0.0,
    val longPauseCount: Int = 0,
    val shortPauseCount: Int = 0,
    val speakingRateConsistencyScore: Double = 1.0,
    val averageSentenceLength: Double = 0.0
)

class AudioFeatureExtractionEngine {

    companion object {
        private const val FRAME_SIZE_MS = 20
        private const val VAD_RMS_THRESHOLD = 0.015f
        private const val SHORT_PAUSE_MIN_MS = 300
        private const val LONG_PAUSE_MIN_MS = 750
        private const val PITCH_MIN_HZ = 80
        private const val PITCH_MAX_HZ = 400
    }

    /** Extracts acoustic features from raw 16-bit PCM audio. */
    fun extract(
        audioSamples: ShortArray,
        sampleRate: Int = SAMPLE_RATE_DEFAULT,
        transcript: String = ""
    ): AcousticFeatures {
        val totalSamples = audioSamples.size
        val durationSeconds = totalSamples.toDouble() / sampleRate
        val samplesPerFrame = (FRAME_SIZE_MS * sampleRate / 1000).coerceAtLeast(1)

        var sumSq = 0.0
        var clippingCount = 0
        for (s in audioSamples) {
            val v = s.toDouble()
            sumSq += v * v
            if (abs(s.toInt()) > 32_000) clippingCount++
        }
        val rmsRaw = if (totalSamples > 0) sqrt(sumSq / totalSamples) else 0.0
        val rmsEnergy = (rmsRaw / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
        val clippingDetected = clippingCount > 0
        val relativeLoudness = if (rmsEnergy > 0f) {
            (20.0 * ln((rmsEnergy + 1e-9).toDouble()) / ln(10.0)).coerceIn(-90.0, 0.0)
        } else -90.0

        val frameCount = totalSamples / samplesPerFrame
        val frameRms = FloatArray(frameCount)
        val frameActive = BooleanArray(frameCount)
        var activeFrames = 0
        var silentFrames = 0
        var silentRmsSum = 0.0

        for (fi in 0 until frameCount) {
            val start = fi * samplesPerFrame
            val end = minOf(start + samplesPerFrame, totalSamples)
            var fSum = 0.0
            for (i in start until end) {
                val s = audioSamples[i].toDouble()
                fSum += s * s
            }
            val fRms = (sqrt(fSum / (end - start)) / Short.MAX_VALUE).toFloat()
            frameRms[fi] = fRms
            val isActive = fRms >= VAD_RMS_THRESHOLD
            frameActive[fi] = isActive
            if (isActive) activeFrames++ else {
                silentFrames++
                silentRmsSum += fRms
            }
        }

        val speechDuration = (activeFrames * FRAME_SIZE_MS) / 1000.0
        val silenceDuration = (silentFrames * FRAME_SIZE_MS) / 1000.0
        val speechSilenceRatio = speechDuration / maxOf(silenceDuration, 0.001)
        val backgroundNoiseLevel = if (silentFrames > 0) (silentRmsSum / silentFrames).toFloat() else 0.005f

        val meanRms = if (frameCount > 0) frameRms.average() else 0.0
        val volumeVariation = if (frameCount > 1) {
            sqrt(frameRms.map { (it - meanRms).let { d -> d * d } }.average())
        } else 0.0

        var totalPauseCount = 0
        var shortPauseCount = 0
        var longPauseCount = 0
        var totalPauseFrames = 0
        var consecutiveSilent = 0
        for (fi in 0 until frameCount) {
            if (!frameActive[fi]) consecutiveSilent++ else {
                if (consecutiveSilent * FRAME_SIZE_MS >= SHORT_PAUSE_MIN_MS) {
                    val pauseMs = consecutiveSilent * FRAME_SIZE_MS
                    totalPauseCount++
                    totalPauseFrames += consecutiveSilent
                    if (pauseMs >= LONG_PAUSE_MIN_MS) longPauseCount++ else shortPauseCount++
                }
                consecutiveSilent = 0
            }
        }
        if (consecutiveSilent * FRAME_SIZE_MS >= SHORT_PAUSE_MIN_MS) {
            val pauseMs = consecutiveSilent * FRAME_SIZE_MS
            totalPauseCount++
            totalPauseFrames += consecutiveSilent
            if (pauseMs >= LONG_PAUSE_MIN_MS) longPauseCount++ else shortPauseCount++
        }

        val meanPauseDurationMs = if (totalPauseCount > 0) {
            (totalPauseFrames.toDouble() * FRAME_SIZE_MS) / totalPauseCount
        } else 0.0
        val speechContinuityScore = if (frameCount > 0) activeFrames.toDouble() / frameCount else 0.0

        val pitchEstimates = mutableListOf<Double>()
        val minLag = sampleRate / PITCH_MAX_HZ
        val maxLag = sampleRate / PITCH_MIN_HZ
        for (fi in 0 until frameCount step 2) {
            if (!frameActive[fi]) continue
            val start = fi * samplesPerFrame
            val end = minOf(start + samplesPerFrame * 2, totalSamples)
            val frameLen = end - start
            if (frameLen < maxLag * 2) continue

            val diffs = DoubleArray(maxLag - minLag + 1) { lagIdx ->
                val lag = minLag + lagIdx
                var diff = 0.0
                val N = frameLen / 2
                for (i in 0 until N) {
                    val d = audioSamples[start + i].toDouble() - audioSamples[start + i + lag].toDouble()
                    diff += d * d
                }
                diff
            }

            val threshold = (diffs.firstOrNull() ?: continue) * 0.20
            var bestLag = -1
            var bestVal = Double.MAX_VALUE
            for (i in 1 until diffs.size - 1) {
                val v = diffs[i]
                if (v < threshold && v < bestVal) {
                    bestLag = minLag + i
                    bestVal = v
                }
            }
            if (bestLag > 0) {
                val f0 = sampleRate.toDouble() / bestLag
                if (f0 in PITCH_MIN_HZ.toDouble()..PITCH_MAX_HZ.toDouble()) pitchEstimates.add(f0)
            }
        }

        val meanPitchHz = if (pitchEstimates.isNotEmpty()) pitchEstimates.average() else 0.0
        val pitchVariation = if (pitchEstimates.size > 1) {
            sqrt(pitchEstimates.map { (it - meanPitchHz).let { d -> d * d } }.average())
        } else 0.0

        val words = if (transcript.isBlank()) emptyList() else transcript.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val wordCount = words.size
        val sentenceCount = if (transcript.isBlank()) 0 else transcript.count { it == '.' || it == '!' || it == '?' }.coerceAtLeast(1)
        val avgSentenceLength = if (sentenceCount > 0) wordCount.toDouble() / sentenceCount else 0.0
        val wordsPerMinute = if (wordCount > 0 && durationSeconds > 0) wordCount / (durationSeconds / 60.0) else 0.0
        val articulationRate = if (wordCount > 0 && speechDuration > 0) wordCount / (speechDuration / 60.0) else 0.0

        val rateConsistency = if (wordsPerMinute > 0 && durationSeconds >= 20.0) {
            val expectedWpmDev = abs(wordsPerMinute - articulationRate) / maxOf(wordsPerMinute, 1.0)
            (1.0 - expectedWpmDev * 0.5).coerceIn(0.5, 1.0)
        } else 1.0

        return AcousticFeatures(
            durationSeconds = durationSeconds,
            speechDurationSeconds = speechDuration,
            silenceDurationSeconds = silenceDuration,
            speechSilenceRatio = speechSilenceRatio,
            wordCount = wordCount,
            sentenceCount = sentenceCount,
            wordsPerMinute = wordsPerMinute.toInt(),
            articulationRateEstimate = articulationRate,
            speakingRateConsistencyScore = rateConsistency,
            meanPitchHz = meanPitchHz,
            pitchVariation = pitchVariation,
            pitchVariationHz = pitchVariation,
            rmsEnergy = rmsEnergy,
            relativeLoudness = relativeLoudness,
            rmsVolumeDb = relativeLoudness,
            clippingDetected = clippingDetected,
            backgroundNoiseLevel = backgroundNoiseLevel,
            pauseCount = totalPauseCount,
            longPauseCount = longPauseCount,
            shortPauseCount = shortPauseCount,
            meanPauseDurationMs = meanPauseDurationMs,
            speechContinuityScore = speechContinuityScore,
            volumeVariation = volumeVariation,
            averageSentenceLength = avgSentenceLength
        )
    }

    fun extractFromFile(audioFile: File, transcript: String = ""): AcousticFeatures? {
        val samples = AudioResampler.resampleToWhisperFormat(audioFile) ?: run {
            Log.e(TAG, "Resampling failed for ${audioFile.name}")
            return null
        }
        return extract(samples, SAMPLE_RATE_DEFAULT, transcript)
    }
}
