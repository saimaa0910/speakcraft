package com.example.speech.capture

import android.media.AudioFormat

/**
 * Standardized audio capture configuration for SpeakCraft speech pipeline (ARC-05 §4.1, CMP-26).
 *
 * Enforces:
 * - 16,000 Hz sample rate (16 kHz)
 * - 1 channel (mono)
 * - 16-bit PCM little-endian (PCM16)
 * - 20 ms frame duration (320 samples / 640 bytes per frame)
 */
data class AudioCaptureConfig(
    val sampleRateHz: Int = 16_000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val frameDurationMs: Int = 20
) {
    val channelCount: Int = 1
    val bytesPerSample: Int = 2 // 16-bit PCM

    val samplesPerFrame: Int = (sampleRateHz * frameDurationMs) / 1000 // 320
    val bytesPerFrame: Int = samplesPerFrame * bytesPerSample // 640

    val bytesPerSecond: Int = sampleRateHz * bytesPerSample * channelCount // 32,000
}
