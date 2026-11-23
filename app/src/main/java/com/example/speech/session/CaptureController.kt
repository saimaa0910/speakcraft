package com.example.speech.session

import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.StreamingFeatureExtractor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Source of 16-bit mono PCM (CMP-26). The Android implementation wraps `AudioRecord`
 * (`AudioRecordSource`); tests use synthetic sources.
 */
interface AudioSource {
    val sampleRateHz: Int
    /** Starts capture. Throws when capture cannot start (e.g. permission revoked, device busy). */
    fun start()
    /** Blocking read. Returns samples read (> 0), 0 when no data is ready yet, or a negative error code. */
    fun read(buffer: ShortArray, offset: Int, length: Int): Int
    fun stop()
    fun release()
}

/** Receives captured PCM (e.g. the WAV writer or the recogniser pipe for tier T1). */
interface PcmSink {
    /** May throw (e.g. `IOException` when storage is full); capture then stops with [StopReason.SINK_ERROR]. */
    fun onPcm(samples: ShortArray, length: Int)
    fun close() {}
}

/** Why a recording ended (SRS-FR-SESS-003/010, SRS-ERR-004). */
enum class StopReason { USER, MAX_DURATION, INTERRUPTED, SOURCE_ERROR, SINK_ERROR }

data class CaptureResult(
    val features: AcousticFeatures,
    val stopReason: StopReason,
    val error: Throwable? = null
)

/** Progress and completion callbacks; called on the capture thread. */
interface CaptureListener {
    fun onElapsed(elapsedMs: Long) {}
    /** Fired once, 10 s before [CaptureController.maxDurationMs] for limits of 30 s or more (SRS-FR-SESS-003). */
    fun onLimitWarning(remainingMs: Long) {}
    fun onStopped(result: CaptureResult) {}
}

/**
 * Runs one recording on a dedicated thread: reads [source] in 20 ms chunks, measures acoustic
 * features as it goes ([StreamingFeatureExtractor]) and forwards PCM to [sinks]. Stops on request,
 * at the maximum duration, on interruption, or when the source or a sink fails; the result always
 * carries the features measured up to that point.
 *
 * Pure Kotlin (no Android types), so the stop rules are unit-tested on the JVM.
 */
class CaptureController(
    private val source: AudioSource,
    private val sinks: List<PcmSink> = emptyList(),
    val maxDurationMs: Long,
    private val listener: CaptureListener? = null
) {
    private val extractor = StreamingFeatureExtractor(source.sampleRateHz)
    private val chunk = source.sampleRateHz / 50 // 20 ms
    private val finished = CountDownLatch(1)

    @Volatile private var requested: StopReason? = null
    @Volatile private var result: CaptureResult? = null
    private var thread: Thread? = null

    val isRunning: Boolean get() = thread != null && result == null

    /** Starts capture; throws if the source cannot start (the caller shows the permission/error state). */
    fun start() {
        check(thread == null) { "capture already started" }
        source.start()
        thread = Thread({ loop() }, "speakcraft-capture").apply { isDaemon = true; start() }
    }

    /** Requests a stop and waits (up to [timeoutMs]) for the capture thread to finish. */
    fun stop(reason: StopReason = StopReason.USER, timeoutMs: Long = 5_000): CaptureResult {
        if (requested == null) requested = reason
        finished.await(timeoutMs, TimeUnit.MILLISECONDS)
        return checkNotNull(result) { "capture did not finish within $timeoutMs ms" }
    }

    /** Phone call, audio-focus loss or app backgrounding (SRS-FR-SESS-010). */
    fun interrupt(): CaptureResult = stop(StopReason.INTERRUPTED)

    /** Waits for an automatic stop (max duration or failure). */
    fun awaitStopped(timeoutMs: Long): CaptureResult? =
        if (finished.await(timeoutMs, TimeUnit.MILLISECONDS)) result else null

    private fun loop() {
        val buffer = ShortArray(chunk)
        val maxSamples = maxDurationMs * source.sampleRateHz / 1000
        val warnAt = if (maxDurationMs >= 30_000) (maxDurationMs - 10_000) * source.sampleRateHz / 1000 else Long.MAX_VALUE
        var warned = false
        var total = 0L
        var reason: StopReason? = null
        var error: Throwable? = null
        try {
            while (true) {
                requested?.let { reason = it }
                if (reason != null) break
                val n = source.read(buffer, 0, chunk)
                if (n < 0) { reason = StopReason.SOURCE_ERROR; error = IllegalStateException("audio read error $n"); break }
                if (n == 0) { Thread.sleep(2); continue }
                extractor.accept(buffer, n)
                try {
                    for (s in sinks) s.onPcm(buffer, n)
                } catch (e: Exception) {
                    reason = StopReason.SINK_ERROR; error = e; break
                }
                total += n
                listener?.onElapsed(total * 1000 / source.sampleRateHz)
                if (!warned && total >= warnAt) { warned = true; listener?.onLimitWarning((maxSamples - total) * 1000 / source.sampleRateHz) }
                if (total >= maxSamples) { reason = StopReason.MAX_DURATION; break }
            }
        } catch (e: Exception) {
            reason = StopReason.SOURCE_ERROR; error = e
        } finally {
            runCatching { source.stop() }
            runCatching { source.release() }
            sinks.forEach { runCatching { it.close() } }
            val r = CaptureResult(extractor.finish(), reason ?: StopReason.USER, error)
            result = r
            finished.countDown()
            runCatching { listener?.onStopped(r) }
        }
    }
}

/** Only one recording may be active app-wide (SRS-FR-SESS-014; CMP-27). */
class RecordingArbiter {
    private val owner = java.util.concurrent.atomic.AtomicReference<String?>(null)
    fun tryAcquire(attemptId: String): Boolean = owner.compareAndSet(null, attemptId)
    fun release(attemptId: String) { owner.compareAndSet(attemptId, null) }
    val activeAttemptId: String? get() = owner.get()
}
