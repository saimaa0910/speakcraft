package com.example.speech.pipeline

import com.example.speech.evidence.InconclusiveReason
import com.example.speech.feedback.Feedback
import com.example.speech.feedback.FocusFeedback
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.rules.DetectedIssue
import com.example.speech.word.WordStageResult

/**
 * Standard input payload for an attempt evaluation (ARC-05 §2, §5).
 */
data class AttemptInput(
    val attemptId: String,
    val templateId: String,
    val durationMs: Long,
    val voicedDurationMs: Long,
    val transcript: String,
    val pauseCount: Int,
    val activeFrameRmsList: List<Double> = emptyList(),
    val noiseFloorRms: Double = 0.01,
    val clippingRatio: Double = 0.0,
    val hasAudio: Boolean = true,
    val hasTranscript: Boolean = true,
    val referenceText: String? = null,
    val targetWord: String? = null,
    val targetWordInflections: List<String> = emptyList(),
    val targetMinWords: Int? = null,
    val targetMaxWords: Int? = null,
    val framework: String? = null,
    val techniqueIds: List<String> = emptyList(),
    val previousMetrics: Map<SpeechMetricKey, SpeechMetricValue>? = null
)

/**
 * Versioned outcome of evaluating an attempt through LocalIntelligencePipeline (ARC-05 §2, §10).
 */
sealed interface AttemptEvaluationResult {
    val attemptId: String
    val evaluationVersion: String

    data class Valid(
        override val attemptId: String,
        override val evaluationVersion: String = LocalIntelligencePipeline.EVALUATION_VERSION,
        val templateId: String,
        val headlineMetrics: List<SpeechMetricValue>,
        val allMetrics: Map<SpeechMetricKey, SpeechMetricValue>,
        val feedback: Feedback,
        val issues: List<DetectedIssue>,
        val passed: Boolean?,
        val score: Int?,
        val hasClippingWarning: Boolean = false,
        val wordStageResult: WordStageResult? = null,
        val evaluationLatencyMs: Long
    ) : AttemptEvaluationResult

    data class Inconclusive(
        override val attemptId: String,
        override val evaluationVersion: String = LocalIntelligencePipeline.EVALUATION_VERSION,
        val templateId: String,
        val reason: InconclusiveReason,
        val advice: FocusFeedback,
        val durationMs: Long
    ) : AttemptEvaluationResult
}
