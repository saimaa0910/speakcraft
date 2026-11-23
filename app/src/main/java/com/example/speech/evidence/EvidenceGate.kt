package com.example.speech.evidence

/**
 * Standardized inconclusive reasons defined in ARC-05 §5 and SRS-BR-001.
 */
enum class InconclusiveReason(val userMessage: String) {
    TOO_SHORT("Recording was too short to evaluate (under 1.5 seconds). Try speaking a full sentence."),
    TOO_QUIET("Audio was too quiet or the microphone was muffled. Please speak closer to the mic."),
    NO_SPEECH("No voiced speech detected in the recording."),
    NO_TRANSCRIPT("No words were recognized. Please ensure microphone permissions are granted and speak clearly."),
    TOO_FEW_WORDS("Too few words recognized for this exercise. Try elaborating on your response.")
}

/**
 * Outcome of evaluating an attempt against the Evidence Gate (ARC-05 §5, SRS-BR-001).
 */
sealed class EvidenceGateResult {
    data class Valid(val hasClippingWarning: Boolean = false) : EvidenceGateResult()
    data class Inconclusive(val reason: InconclusiveReason) : EvidenceGateResult()
}

/**
 * Deterministic Evidence Gate enforcing honesty invariants before metric calculation
 * (ARC-05 §5, FND-AI-17, SRS-BR-001).
 */
object EvidenceGate {

    /**
     * Minimum overall duration required for any attempt (1.5 seconds).
     */
    const val MIN_DURATION_MS = 1500L

    /**
     * Minimum voiced speech required (0.5 seconds).
     */
    const val MIN_VOICED_SPEECH_MS = 500L

    /**
     * Minimum voiced speech ratio (25% of total recording).
     */
    const val MIN_VOICED_RATIO = 0.25

    /**
     * Evaluates attempt evidence against strict quality and honesty thresholds.
     */
    fun evaluate(
        durationMs: Long,
        voicedDurationMs: Long,
        meanActiveRms: Double,
        noiseFloorRms: Double,
        wordCount: Int,
        templateId: String,
        hasAudio: Boolean = true,
        hasTranscript: Boolean = true,
        clippingRatio: Double = 0.0
    ): EvidenceGateResult {
        // 1. Duration check (applies to all templates)
        if (durationMs < MIN_DURATION_MS) {
            return EvidenceGateResult.Inconclusive(InconclusiveReason.TOO_SHORT)
        }

        // 2. Audio voiced & level checks (when audio is available)
        if (hasAudio) {
            if (voicedDurationMs < MIN_VOICED_SPEECH_MS) {
                return EvidenceGateResult.Inconclusive(InconclusiveReason.NO_SPEECH)
            }

            val voicedRatio = voicedDurationMs.toDouble() / durationMs.toDouble()
            if (voicedRatio < MIN_VOICED_RATIO) {
                return EvidenceGateResult.Inconclusive(InconclusiveReason.NO_SPEECH)
            }

            // Audio level must be distinguishably above noise floor (delta > 3 dB or 1.4x factor)
            if (meanActiveRms <= noiseFloorRms * 1.15) {
                return EvidenceGateResult.Inconclusive(InconclusiveReason.TOO_QUIET)
            }
        }

        // 3. Transcript presence checks
        if (hasTranscript) {
            if (wordCount <= 0) {
                return EvidenceGateResult.Inconclusive(InconclusiveReason.NO_TRANSCRIPT)
            }

            val minWordsRequired = when (templateId.uppercase()) {
                "TPL-ANSWER", "TPL-STRUCT", "TPL-STORY", "ANSWER", "STRUCT", "STORY" -> 8
                else -> 1 // READ, REPEAT, TPL-WORD-* require >= 1 word
            }

            if (wordCount < minWordsRequired) {
                return EvidenceGateResult.Inconclusive(InconclusiveReason.TOO_FEW_WORDS)
            }
        }

        val isClipped = clippingRatio > 0.01 // > 1% samples clipped
        return EvidenceGateResult.Valid(hasClippingWarning = isClipped)
    }
}
