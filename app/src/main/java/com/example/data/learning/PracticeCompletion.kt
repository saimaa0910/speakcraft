package com.example.data.learning

import com.example.speech.content.PracticeActivity
import com.example.speech.pipeline.AttemptEvaluationResult

/**
 * One finished Coach Practice attempt (on-device engine practice, EP-08), as handed to learning progress.
 *
 * Only values the local intelligence engine actually produced are carried: [score] and [passed] stay `null`
 * for free-speaking templates that have no pass mark (SRS-BR-006), and an attempt without enough evidence is
 * [valid] = `false` with its [inconclusiveReason] — never scored (SRS-BR-002).
 */
data class PracticeCompletion(
    val attemptId: String,
    val activityId: String,
    val title: String,
    val techniqueIds: List<String>,
    val templateId: String,
    val timestampMs: Long,
    val evaluationVersion: String,
    /** `true` = the engine evaluated the attempt; `false` = inconclusive (not enough evidence). */
    val valid: Boolean,
    /** Issue codes raised for a valid attempt (empty for inconclusive attempts). */
    val issueCodes: List<String> = emptyList(),
    /** The single strength selected for a valid attempt (e.g. `STR_PACE_IN_BAND`); null when inconclusive. */
    val strengthCode: String? = null,
    /** Score as reported by the engine, only for templates that score (read-aloud, word stages). */
    val score: Int? = null,
    /** Pass/fail as reported by the engine, only for templates that have a pass mark. */
    val passed: Boolean? = null,
    /** Why the attempt was inconclusive (e.g. `TOO_SHORT`); null for valid attempts. */
    val inconclusiveReason: String? = null,
    /** Local recorded audio/video replay path on this device, if captured. */
    val localMediaPath: String = "",
    val hasVideo: Boolean = false,
    val durationSeconds: Int = 0,
    val summary: String = "",
    val transcript: String = ""
) {
    companion object {
        /** Builds the completion for a finished attempt from the engine result, copying values as they are. */
        fun fromAttempt(
            activity: PracticeActivity,
            evaluation: AttemptEvaluationResult,
            timestampMs: Long,
            localMediaPath: String = "",
            hasVideo: Boolean = false,
            durationSeconds: Int = 0
        ): PracticeCompletion = when (evaluation) {
            is AttemptEvaluationResult.Valid -> PracticeCompletion(
                attemptId = evaluation.attemptId,
                activityId = activity.id,
                title = activity.title,
                techniqueIds = activity.techniqueIds,
                templateId = evaluation.templateId,
                timestampMs = timestampMs,
                evaluationVersion = evaluation.evaluationVersion,
                valid = true,
                issueCodes = evaluation.issues.map { it.code.name }.distinct(),
                strengthCode = evaluation.feedback.strength.code.name,
                score = evaluation.score,
                passed = evaluation.passed,
                localMediaPath = localMediaPath,
                hasVideo = hasVideo,
                durationSeconds = durationSeconds,
                summary = evaluation.feedback.strength.message.ifBlank { evaluation.feedback.strength.title },
                transcript = evaluation.transcriptText
            )
            is AttemptEvaluationResult.Inconclusive -> PracticeCompletion(
                attemptId = evaluation.attemptId,
                activityId = activity.id,
                title = activity.title,
                techniqueIds = activity.techniqueIds,
                templateId = evaluation.templateId,
                timestampMs = timestampMs,
                evaluationVersion = evaluation.evaluationVersion,
                valid = false,
                inconclusiveReason = evaluation.reason.name,
                localMediaPath = localMediaPath,
                hasVideo = hasVideo,
                durationSeconds = durationSeconds,
                summary = evaluation.reason.userMessage
            )
        }
    }
}
