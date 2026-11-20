package com.example.data.evaluation

import com.example.data.model.SpeakingSession

/**
 * Explicit 12-state lifecycle for SpeakCraft Coach and Activity evaluations.
 * Never transitions to [COMPLETED] unless [EvaluationEvidenceValidator] passes.
 */
enum class CoachEvaluationState {
    NOT_STARTED,
    WAITING_FOR_AUDIO,
    WAITING_FOR_VIDEO,
    RECORDING,
    PROCESSING,
    VALIDATING_EVIDENCE,
    EVALUATING,
    COMPLETED,
    INSUFFICIENT_EVIDENCE,
    RECORDING_FAILED,
    ANALYSIS_FAILED,
    CANCELLED
}

enum class DimensionAvailability(val label: String) {
    EVALUATED("Evaluated"),
    UNAVAILABLE_MISSING_EVIDENCE("Unavailable (Missing Evidence)"),
    UNAVAILABLE_ANALYSIS_FAILED("Unavailable (Analysis Failed)"),
    NOT_REQUIRED("Not Required")
}

data class EvaluationDimensionDetail(
    val key: String,
    val label: String,
    val score: Int?,
    val evidenceSource: String,
    val confidence: Float,
    val availabilityStatus: DimensionAvailability,
    val evaluationTimestampMs: Long,
    val note: String = ""
)

enum class EvidenceIssueCode {
    MISSING_AUDIO,
    EMPTY_AUDIO_FILE,
    DURATION_TOO_SHORT,
    SILENT_AUDIO,
    UNSUPPORTED_OR_CORRUPT_AUDIO,
    AUDIO_ANALYSIS_FAILED,
    MISSING_VIDEO,
    UNREADABLE_VIDEO_FRAMES,
    VIDEO_ANALYSIS_FAILED,
    MISSING_TRANSCRIPT,
    MEANINGLESS_TRANSCRIPT,
    STALE_TRANSCRIPT,
    MODEL_INFERENCE_FAILED,
    RECORDING_FAILED
}

data class EvidenceValidationResult(
    val isValidForEvaluation: Boolean,
    val terminalFailureState: CoachEvaluationState?,
    val primaryIssueCode: EvidenceIssueCode?,
    val userTitle: String,
    val userMessage: String,
    val audioValid: Boolean,
    val videoValid: Boolean,
    val videoRequired: Boolean,
    val transcriptValid: Boolean,
    val transcriptRequired: Boolean,
    val measuredDurationSeconds: Double = 0.0,
    val measuredRmsEnergy: Float = 0f,
    val voicedSeconds: Double = 0.0,
    val audioDecodeError: String? = null,
    val issues: List<String> = emptyList()
)

data class CoachEvaluationReport(
    val sessionId: String,
    val evaluationState: CoachEvaluationState,
    val overallScore: Int?,
    val statusHeadline: String,
    val statusMessage: String,
    val isPartialEvaluation: Boolean,
    val partialReason: String?,
    val dimensions: Map<String, EvaluationDimensionDetail>,
    val strengths: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val correctedExamples: List<String> = emptyList(),
    val explanation: String = "",
    val improvementSuggestions: List<String> = emptyList(),
    val nextPracticeTask: String = "",
    val honestyNotice: String = "",
    val session: SpeakingSession? = null,
    val validation: EvidenceValidationResult,
    val evaluatedAtMs: Long = System.currentTimeMillis()
)

/**
 * Thrown by compatibility APIs (e.g. `Result<SpeakingSession>`) when speech/video/transcript
 * evidence is missing, silent, too short, corrupt, or failed analysis — guaranteeing no
 * fabricated baseline score is ever returned.
 */
class InsufficientSpeechEvidenceException(
    val report: CoachEvaluationReport
) : IllegalStateException(
    "${report.statusHeadline} ${report.statusMessage}".trim()
)
