package com.example.speech.tier

/**
 * Speech capture and recognition capability tiers defined in ADR-009 / ARC-05 §3.
 */
enum class SpeechTier(
    val label: String,
    val hasTranscript: Boolean,
    val hasAudioReplay: Boolean,
    val isStrictlyOnDevice: Boolean
) {
    /**
     * Tier 1 (Full on-device):
     * - API >= 33, on-device recognizer available and language pack present.
     * - App owns microphone via AudioRecord (16 kHz mono PCM16).
     * - Audio is piped via ParcelFileDescriptor to on-device SpeechRecognizer with EXTRA_SEGMENTED_SESSION.
     * - Audio recorded to WAV for replay and comprehensive acoustic analysis.
     */
    T1_FULL_ON_DEVICE(
        label = "On-device transcription",
        hasTranscript = true,
        hasAudioReplay = true,
        isStrictlyOnDevice = true
    ),

    /**
     * Tier 2 (On-device, recognizer owns microphone):
     * - API 31-32 with on-device recognizer, or API 33+ where audio-source piping failed in SPK-01 / runtime.
     * - Recognizer owns the microphone.
     * - Continuous transcription via segment restart loop and text stitching with de-duplication.
     * - Audio replay unavailable; acoustic metrics sourced from onRmsChanged.
     */
    T2_ON_DEVICE_MIC(
        label = "On-device transcription (no replay)",
        hasTranscript = true,
        hasAudioReplay = false,
        isStrictlyOnDevice = true
    ),

    /**
     * Tier 3a (Online platform transcription):
     * - API < 31, or device without offline recognizer, where the user has explicitly given informed consent.
     * - Platform network speech recognizer captures and transcribes.
     * - Segment restart loop and text stitching.
     */
    T3A_ONLINE_CONSENTED(
        label = "Online transcription",
        hasTranscript = true,
        hasAudioReplay = false,
        isStrictlyOnDevice = false
    ),

    /**
     * Tier 3b (Audio-only fallback):
     * - API < 31 or no offline recognizer, and user has not consented to online transcription.
     * - App owns microphone via AudioRecord.
     * - No transcript generated; provides delivery-only acoustic metrics (duration, speaking time, pauses, pace).
     */
    T3B_AUDIO_ONLY(
        label = "Audio-only feedback",
        hasTranscript = false,
        hasAudioReplay = true,
        isStrictlyOnDevice = true
    )
}
