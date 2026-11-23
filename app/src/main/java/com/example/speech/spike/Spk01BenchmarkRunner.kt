package com.example.speech.spike

import android.os.Build
import android.os.ParcelFileDescriptor
import com.example.core.logging.AppLogger
import com.example.speech.tier.SpeechTier
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private const val TAG = "Spk01BenchmarkRunner"

/**
 * Benchmark result for a device under test in SPK-01 (ARC-05 §4.4, T-05.1, RSK-01).
 */
data class Spk01DeviceBenchmarkResult(
    val deviceModel: String,
    val apiLevel: Int,
    val detectedTier: SpeechTier,
    val isPipingSupported: Boolean,
    val avgFinalizationLatencyMs: Long,
    val avgRestartGapMs: Long,
    val avgWordErrorRate: Float,
    val totalUtterancesTested: Int,
    val recommendations: String
)

/**
 * Utterance test case for SPK-01 evaluation corpus.
 */
data class SpkUtterance(
    val id: String,
    val referenceText: String,
    val speakerAccent: String, // e.g. "B1-Spanish", "B2-Hindi", "B1-German", "Native-US"
    val audioDurationMs: Long
)

/**
 * Automated test and verification harness for SPK-01 Device Spike (T-05.1).
 *
 * Evaluates:
 * 1. Audio-source piping feasibility (ParcelFileDescriptor pipe to recognizer)
 * 2. Finalization latency after Stop (NFR-PERF-006 target <= 1500 ms)
 * 3. Segment restart gap latency (target <= 150 ms)
 * 4. Word error rate (WER) across B1-B2 accented utterances
 */
class Spk01BenchmarkRunner {

    /**
     * Verifies that the platform ParcelFileDescriptor pipe can be created and streamed to
     * without blocking or crashing (T1 prerequisite).
     */
    fun testAudioSourcePipeFeasibility(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            AppLogger.i(TAG, "Audio-source pipe requires API 33+; current API=${Build.VERSION.SDK_INT}")
            return false
        }

        return try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            // Write 640 bytes (one 20 ms frame)
            ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { out ->
                val sampleFrame = ByteArray(640)
                out.write(sampleFrame)
                out.flush()
            }

            readSide.close()
            AppLogger.i(TAG, "ParcelFileDescriptor pipe test succeeded")
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "ParcelFileDescriptor pipe test failed: ${e.message}")
            false
        }
    }

    /**
     * Computes Word Error Rate (WER) between hypothesis and reference transcripts using
     * Levenshtein word-level distance.
     */
    fun computeWordErrorRate(hypothesis: String, reference: String): Float {
        val hypWords = hypothesis.trim().lowercase(Locale.ROOT)
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
        val refWords = reference.trim().lowercase(Locale.ROOT)
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

        if (refWords.isEmpty()) {
            return if (hypWords.isEmpty()) 0.0f else 1.0f
        }

        val d = Array(refWords.size + 1) { IntArray(hypWords.size + 1) }

        for (i in 0..refWords.size) d[i][0] = i
        for (j in 0..hypWords.size) d[0][j] = j

        for (i in 1..refWords.size) {
            for (j in 1..hypWords.size) {
                val cost = if (refWords[i - 1] == hypWords[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,      // deletion
                    d[i][j - 1] + 1,      // insertion
                    d[i - 1][j - 1] + cost // substitution
                )
            }
        }

        val distance = d[refWords.size][hypWords.size]
        return (distance.toFloat() / refWords.size).coerceAtMost(1.0f)
    }

    /**
     * Aggregates benchmark runs for a specific device profile.
     */
    fun aggregateDeviceRun(
        deviceModel: String,
        apiLevel: Int,
        tier: SpeechTier,
        pipingSupported: Boolean,
        latenciesMs: List<Long>,
        restartGapsMs: List<Long>,
        wers: List<Float>
    ): Spk01DeviceBenchmarkResult {
        val avgLatency = if (latenciesMs.isNotEmpty()) latenciesMs.average().toLong() else 0L
        val avgRestart = if (restartGapsMs.isNotEmpty()) restartGapsMs.average().toLong() else 0L
        val avgWer = if (wers.isNotEmpty()) wers.average().toFloat() else 0.0f

        val recommendations = when {
            tier == SpeechTier.T1_FULL_ON_DEVICE && avgLatency <= 1500L ->
                "GO for T1 audio piping on $deviceModel (latency ${avgLatency}ms <= 1500ms target)"
            tier == SpeechTier.T2_ON_DEVICE_MIC ->
                "GO for T2 restart stitching on $deviceModel (restart gap ${avgRestart}ms)"
            tier == SpeechTier.T3A_ONLINE_CONSENTED ->
                "Qualified for T3a consented online fallback (WER ${String.format(Locale.ROOT, "%.2f", avgWer)})"
            else ->
                "Audio-only T3b mode active; delivery acoustic metrics supported"
        }

        return Spk01DeviceBenchmarkResult(
            deviceModel = deviceModel,
            apiLevel = apiLevel,
            detectedTier = tier,
            isPipingSupported = pipingSupported,
            avgFinalizationLatencyMs = avgLatency,
            avgRestartGapMs = avgRestart,
            avgWordErrorRate = avgWer,
            totalUtterancesTested = latenciesMs.size,
            recommendations = recommendations
        )
    }
}
