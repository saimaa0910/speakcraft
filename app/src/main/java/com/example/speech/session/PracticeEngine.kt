package com.example.speech.session

import com.example.speech.tier.SpeechTier
import java.io.File

/** Options chosen on the practice screen for one attempt. */
data class PracticeStartOptions(
    /** Analyse body language with the front camera (ARC-07; D-42). The camera must already be bound. */
    val withCamera: Boolean,
    /** The user agreed to online transcription when no on-device recogniser exists (SRS-BR-023). */
    val allowOnline: Boolean
)

/** Live progress for the recording screen; may be called from any thread. */
interface PracticeProgressListener {
    fun onElapsed(elapsedMs: Long) {}
    fun onLimitWarning(remainingMs: Long) {}
    /** Recording ended by itself (time limit or a device error); the caller should call [PracticeHandle.stop]. */
    fun onAutoStopped(reason: StopReason) {}
}

/** One running practice attempt. */
interface PracticeHandle {
    val tier: SpeechTier
    val cameraActive: Boolean
    /** Stops, evaluates on the device and prepares local replay. Blocking: call off the main thread. */
    fun stop(): PracticeResult
    /** Discards the attempt; nothing is evaluated or kept. */
    fun cancel()
}

/** What the result screen shows for one attempt. */
data class PracticeResult(
    val outcome: AttemptOutcome,
    val tier: SpeechTier,
    /** App-private replay (M4A audio, or MP4 when the camera recorded); null when not available. */
    val replayFile: File?,
    val replayHasVideo: Boolean,
    /**
     * The on-device recogniser did not accept piped audio on this device (T1), so later attempts use the
     * recogniser's own microphone (T2). The user is told and asked to try again (RSK-09).
     */
    val switchedToCompatibilityMode: Boolean = false
)

/** The device side of practice: tier detection and recording (Android implementation: `PhonePracticeEngine`). */
interface PracticeEngine {
    fun detectTier(allowOnline: Boolean): SpeechTier
    fun start(request: AttemptRequest, options: PracticeStartOptions, listener: PracticeProgressListener): PracticeHandle
}
