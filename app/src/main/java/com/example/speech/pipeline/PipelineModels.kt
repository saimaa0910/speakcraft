package com.example.speech.pipeline

import com.example.speech.analysis.AcousticFeatures
import com.example.speech.evidence.InconclusiveReason
import com.example.speech.feedback.Feedback
import com.example.speech.feedback.FocusFeedback
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import com.example.speech.word.WordStageResult
import com.example.speech.video.VideoEvaluation

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
    val previousMetrics: Map<SpeechMetricKey, SpeechMetricValue>? = null,
    val issueOverrides: Map<IssueCode, IssueOverride> = emptyMap(),
    /** Result of the on-device video analyser when the camera was on (ARC-07; D-42); null for audio-only attempts. */
    val video: VideoEvaluation? = null
) {
    companion object {
        /**
         * Builds an input from measured acoustic evidence ([com.example.speech.analysis.StreamingFeatureExtractor]
         * or [com.example.speech.analysis.RmsSeriesFeatureExtractor]) so no acoustic field is ever filled in by
         * hand. [transcript] is `null` when no recognition output exists (tier T3b).
         */
        fun fromFeatures(
            attemptId: String,
            templateId: String,
            features: AcousticFeatures,
            transcript: String?,
            referenceText: String? = null,
            targetWord: String? = null,
            targetWordInflections: List<String> = emptyList(),
            targetMinWords: Int? = null,
            targetMaxWords: Int? = null,
            framework: String? = null,
            techniqueIds: List<String> = emptyList(),
            previousMetrics: Map<SpeechMetricKey, SpeechMetricValue>? = null,
            issueOverrides: Map<IssueCode, IssueOverride> = emptyMap(),
            video: VideoEvaluation? = null
        ): AttemptInput = AttemptInput(
            attemptId = attemptId,
            templateId = templateId,
            durationMs = features.durationMs,
            voicedDurationMs = features.voicedDurationMs,
            transcript = transcript.orEmpty(),
            pauseCount = features.pauseCount,
            activeFrameRmsList = features.activeFrameRms,
            noiseFloorRms = features.noiseFloorRms,
            clippingRatio = features.clippingRatio ?: 0.0,
            hasAudio = true,
            hasTranscript = transcript != null,
            referenceText = referenceText,
            targetWord = targetWord,
            targetWordInflections = targetWordInflections,
            targetMinWords = targetMinWords,
            targetMaxWords = targetMaxWords,
            framework = framework,
            techniqueIds = techniqueIds,
            previousMetrics = previousMetrics,
            issueOverrides = issueOverrides,
            video = video
        )
    }
}

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
        val video: VideoEvaluation? = null,
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
