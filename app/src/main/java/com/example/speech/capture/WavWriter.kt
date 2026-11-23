package com.example.speech.capture

import com.example.core.logging.AppLogger
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "WavWriter"

/**
 * Constant-memory streaming WAV file writer adhering to standard 44-byte RIFF/WAVE specifications
 * (ARC-05 §4.1, ADR-015, CMP-26, FND-AI-13).
 *
 * Automatically writes standard header upon creation and patches chunk size headers upon closure.
 */
class WavWriter(
    private val outputFile: File,
    private val config: AudioCaptureConfig = AudioCaptureConfig()
) : Closeable {

    private val outputStream: FileOutputStream = FileOutputStream(outputFile)
    private var totalPcmBytesWritten: Long = 0L
    private var isClosed = false

    init {
        writePlaceholderHeader()
    }

    /**
     * Appends a 20 ms frame of raw PCM16 bytes.
     */
    fun writeFrame(frameBytes: ByteArray, offset: Int = 0, length: Int = frameBytes.size) {
        check(!isClosed) { "Cannot write to closed WavWriter" }
        if (length <= 0) return

        outputStream.write(frameBytes, offset, length)
        totalPcmBytesWritten += length
    }

    /**
     * Appends a frame of PCM16 shorts by serializing to little-endian bytes.
     */
    fun writeFrame(frameShorts: ShortArray, offset: Int = 0, length: Int = frameShorts.size) {
        check(!isClosed) { "Cannot write to closed WavWriter" }
        if (length <= 0) return

        val buffer = ByteBuffer.allocate(length * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in offset until (offset + length)) {
            buffer.putShort(frameShorts[i])
        }
        val bytes = buffer.array()
        writeFrame(bytes, 0, bytes.size)
    }

    fun bytesWritten(): Long = totalPcmBytesWritten

    fun durationSeconds(): Double = totalPcmBytesWritten.toDouble() / config.bytesPerSecond

    override fun close() {
        if (isClosed) return
        isClosed = true

        try {
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error closing stream", e)
        }

        patchHeaderSizes()
    }

    private fun writePlaceholderHeader() {
        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(36) // Placeholder: 36 + SubChunk2Size
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt sub-chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat: 1 = PCM
        buffer.putShort(config.channelCount.toShort()) // NumChannels
        buffer.putInt(config.sampleRateHz) // SampleRate
        buffer.putInt(config.bytesPerSecond) // ByteRate
        buffer.putShort((config.channelCount * config.bytesPerSample).toShort()) // BlockAlign
        buffer.putShort((config.bytesPerSample * 8).toShort()) // BitsPerSample

        // data sub-chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(0) // Placeholder: Subchunk2Size (data size)

        outputStream.write(header)
    }

    private fun patchHeaderSizes() {
        if (!outputFile.exists() || totalPcmBytesWritten == 0L) return

        try {
            RandomAccessFile(outputFile, "rw").use { raf ->
                val totalDataLen = totalPcmBytesWritten
                val totalChunkSize = totalDataLen + 36

                // Patch RIFF Chunk Size at byte 4-7
                raf.seek(4)
                raf.write(
                    byteArrayOf(
                        (totalChunkSize and 0xff).toByte(),
                        ((totalChunkSize shr 8) and 0xff).toByte(),
                        ((totalChunkSize shr 16) and 0xff).toByte(),
                        ((totalChunkSize shr 24) and 0xff).toByte()
                    )
                )

                // Patch data Subchunk2Size at byte 40-43
                raf.seek(40)
                raf.write(
                    byteArrayOf(
                        (totalDataLen and 0xff).toByte(),
                        ((totalDataLen shr 8) and 0xff).toByte(),
                        ((totalDataLen shr 16) and 0xff).toByte(),
                        ((totalDataLen shr 24) and 0xff).toByte()
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to patch WAV header chunk sizes", e)
        }
    }
}
