package com.example.speech.capture

import java.io.Closeable
import java.io.OutputStream

/**
 * High-performance frame distributor fanning 16 kHz mono PCM frames to multiple outputs
 * with zero heap boxing or unbounded buffer growth (ARC-05 §4.1, CMP-26, FND-AI-13).
 */
class PcmTee(
    private val wavWriter: WavWriter? = null,
    private val pipeOutputStream: OutputStream? = null,
    private val frameListener: ((ByteArray, Int) -> Unit)? = null
) : Closeable {

    /**
     * Feeds an incoming PCM frame to all registered sinks.
     */
    fun dispatchFrame(frame: ByteArray, length: Int = frame.size) {
        if (length <= 0) return

        // 1. Sink: WavWriter (disk persistence)
        wavWriter?.writeFrame(frame, 0, length)

        // 2. Sink: Recognizer Pipe (T1 streaming)
        pipeOutputStream?.let { stream ->
            runCatching {
                stream.write(frame, 0, length)
                stream.flush()
            }
        }

        // 3. Sink: In-memory streaming acoustic listener (VAD / RMS)
        frameListener?.invoke(frame, length)
    }

    override fun close() {
        runCatching { wavWriter?.close() }
        runCatching { pipeOutputStream?.close() }
    }
}
