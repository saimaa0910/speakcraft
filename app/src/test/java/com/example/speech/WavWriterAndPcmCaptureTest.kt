package com.example.speech

import com.example.speech.capture.AudioCaptureConfig
import com.example.speech.capture.PcmTee
import com.example.speech.capture.WavWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriterAndPcmCaptureTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `AudioCaptureConfig sets standard 16 kHz mono 20 ms frame constraints`() {
        val config = AudioCaptureConfig()
        assertEquals(16_000, config.sampleRateHz)
        assertEquals(1, config.channelCount)
        assertEquals(2, config.bytesPerSample)
        assertEquals(320, config.samplesPerFrame)
        assertEquals(640, config.bytesPerFrame)
        assertEquals(32_000, config.bytesPerSecond)
    }

    @Test
    fun `WavWriter writes standard 44-byte RIFF header and patches sizes on close`() {
        val targetFile = File(tempFolder.root, "test_output.wav")
        val writer = WavWriter(targetFile)

        // Write 10 frames of 640 bytes each = 6,400 PCM bytes (200 ms of audio)
        val frame = ByteArray(640) { 0x1A.toByte() }
        repeat(10) {
            writer.writeFrame(frame)
        }

        assertEquals(6400L, writer.bytesWritten())
        assertEquals(0.2, writer.durationSeconds(), 0.001)

        writer.close()

        // Verify file on disk
        assertTrue("Output file must exist", targetFile.exists())
        assertEquals("Total file size must be header (44) + data (6400)", 6444L, targetFile.length())

        RandomAccessFile(targetFile, "r").use { raf ->
            // RIFF header magic
            val riffBytes = ByteArray(4)
            raf.readFully(riffBytes)
            assertEquals("RIFF", String(riffBytes))

            // ChunkSize at offset 4: must equal 6400 + 36 = 6436
            raf.seek(4)
            val chunkSizeBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            raf.readFully(chunkSizeBuffer.array())
            assertEquals(6436, chunkSizeBuffer.getInt())

            // WAVE format
            raf.seek(8)
            val waveBytes = ByteArray(4)
            raf.readFully(waveBytes)
            assertEquals("WAVE", String(waveBytes))

            // Subchunk2Size (data size) at offset 40: must equal 6400
            raf.seek(40)
            val subChunk2Buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            raf.readFully(subChunk2Buffer.array())
            assertEquals(6400, subChunk2Buffer.getInt())
        }
    }

    @Test
    fun `PcmTee dispatches frames simultaneously to writer and listeners`() {
        val targetFile = File(tempFolder.root, "tee_output.wav")
        val writer = WavWriter(targetFile)
        val pipeStream = ByteArrayOutputStream()
        var listenerFramesReceived = 0

        val pcmTee = PcmTee(
            wavWriter = writer,
            pipeOutputStream = pipeStream,
            frameListener = { _, length ->
                if (length == 640) listenerFramesReceived++
            }
        )

        val frame = ByteArray(640) { 0x05 }
        repeat(5) {
            pcmTee.dispatchFrame(frame)
        }
        pcmTee.close()

        assertEquals(5, listenerFramesReceived)
        assertEquals(5 * 640, pipeStream.size())
        assertEquals(44 + 5 * 640, targetFile.length().toInt())
    }
}
