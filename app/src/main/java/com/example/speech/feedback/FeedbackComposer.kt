package com.example.speech.feedback

import com.example.speech.evidence.InconclusiveReason
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.pipeline.TranscriptAnalyzer
import com.example.speech.pipeline.TranscriptInsight
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode

/**
 * Deterministic Feedback Composer implementing ARC-05 §8.2, §8.4, and §10 (CMP-35).
 * Guarantees pure, reproducible evaluation results without generative hallucinations.
 */
object FeedbackComposer {

    private val EXPLAINABILITY_MAP = mapOf(
        SpeechMetricKey.M_DUR to MetricExplainability(
            key = SpeechMetricKey.M_DUR,
            label = "Duration",
            methodology = "Measured",
            explanation = "Total elapsed time of your recording in seconds."
        ),
        SpeechMetricKey.M_SPK to MetricExplainability(
            key = SpeechMetricKey.M_SPK,
            label = "Speaking Time",
            methodology = "Measured",
            explanation = "Cumulative duration of voiced speech frames detected above the noise floor."
        ),
        SpeechMetricKey.M_RATIO to MetricExplainability(
            key = SpeechMetricKey.M_RATIO,
            label = "Speech Ratio",
            methodology = "Measured",
            explanation = "Percentage of your recording spent actively speaking versus pausing."
        ),
        SpeechMetricKey.M_WPM to MetricExplainability(
            key = SpeechMetricKey.M_WPM,
            label = "Pace (WPM)",
            methodology = "Measured",
            explanation = "Speaking speed calculated as recognized word count divided by voiced speaking minutes."
        ),
        SpeechMetricKey.M_PAUSE to MetricExplainability(
            key = SpeechMetricKey.M_PAUSE,
            label = "Long Pauses",
            methodology = "Measured",
            explanation = "Count of internal pauses longer than 1.0 second during your response."
        ),
        SpeechMetricKey.M_WORDS to MetricExplainability(
            key = SpeechMetricKey.M_WORDS,
            label = "Words",
            methodology = "Measured",
            explanation = "Total count of words recognized by the speech-to-text engine."
        ),
        SpeechMetricKey.M_LEN to MetricExplainability(
            key = SpeechMetricKey.M_LEN,
            label = "Length vs Target",
            methodology = "Measured",
            explanation = "Comparison of your word count against the recommended exercise target range."
        ),
        SpeechMetricKey.M_TARGET to MetricExplainability(
            key = SpeechMetricKey.M_TARGET,
            label = "Target Word Used",
            methodology = "Measured",
            explanation = "Verification of whether the target vocabulary term or its accepted inflections were spoken."
        ),
        SpeechMetricKey.M_MATCH to MetricExplainability(
            key = SpeechMetricKey.M_MATCH,
            label = "Recognised as Intended",
            methodology = "Measured",
            explanation = "Word-level Levenshtein alignment between your transcript and the reference prompt."
        ),
        SpeechMetricKey.M_FILL to MetricExplainability(
            key = SpeechMetricKey.M_FILL,
            label = "Filler Words",
            methodology = "Estimated",
            explanation = "Detected frequency of vocalized fillers ('um', 'uh', 'you know') per minute of speaking."
        ),
        SpeechMetricKey.M_LEX to MetricExplainability(
            key = SpeechMetricKey.M_LEX,
            label = "Vocabulary Variety",
            methodology = "Estimated",
            explanation = "Moving-Average Type-Token Ratio (MATTR) over a sliding window of 25 tokens."
        ),
        SpeechMetricKey.M_GRAM to MetricExplainability(
            key = SpeechMetricKey.M_GRAM,
            label = "Possible Grammar Checks",
            methodology = "Estimated",
            explanation = "Hits against verified high-precision grammar rules (precision >= 0.90)."
        ),
        SpeechMetricKey.M_STRUCT to MetricExplainability(
            key = SpeechMetricKey.M_STRUCT,
            label = "Structure Elements",
            methodology = "Estimated",
            explanation = "Detection of framework cue phrases (e.g., Situation, Task, Action, Result for STAR)."
        ),
        SpeechMetricKey.M_HEDGE to MetricExplainability(
            key = SpeechMetricKey.M_HEDGE,
            label = "Hedging",
            methodology = "Estimated",
            explanation = "Frequency of softening and qualifying phrases ('I think maybe', 'sort of') per 100 words."
        ),
        SpeechMetricKey.M_LEVEL to MetricExplainability(
            key = SpeechMetricKey.M_LEVEL,
            label = "Loudness Consistency",
            methodology = "Measured (Quality Signal)",
            explanation = "Coefficient of variation of audio frame RMS levels, used only for audio quality guidance."
        )
    )

    private val GENERAL_SELF_CHECKS = listOf(
        SelfCheckItem("SC-EYE-01", "Maintained comfortable, steady eye contact (or looked at the camera)"),
        SelfCheckItem("SC-POSTURE-01", "Kept an open, upright posture without slouching"),
        SelfCheckItem("SC-GESTURE-01", "Used natural hand gestures to emphasize key ideas"),
        SelfCheckItem("SC-FACIAL-01", "Facial expression matched the emotional tone of the message")
    )

    /**
     * Composes strength, focus, tips, explainability, and self-checks for an attempt.
     */
    fun compose(
        attemptId: String,
        templateId: String,
        metrics: Map<SpeechMetricKey, SpeechMetricValue>,
        issues: List<DetectedIssue>,
        techniqueIds: List<String> = emptyList(),
        previousMetrics: Map<SpeechMetricKey, SpeechMetricValue>? = null,
        tipCatalog: List<TipDefinition> = TipSelector.CATALOG,
        transcriptInsights: List<TranscriptInsight> = emptyList()
    ): Feedback {
        val strength = selectStrength(templateId, metrics, previousMetrics)
        val focus = selectFocus(issues)
        val tips = TipSelector.select(
            attemptId = attemptId,
            issues = issues,
            metrics = metrics,
            techniqueIds = techniqueIds,
            templateId = templateId,
            maxExtra = 2,
            catalog = tipCatalog
        )
        val explainability = metrics.keys.mapNotNull { EXPLAINABILITY_MAP[it] }
        val transcriptInsightSummary = if (transcriptInsights.isNotEmpty()) {
            TranscriptAnalyzer.composeSummary(transcriptInsights)
        } else {
            null
        }

        return Feedback(
            strength = strength,
            focus = focus,
            issues = issues,
            tips = tips,
            selfCheckItems = GENERAL_SELF_CHECKS,
            explainability = explainability,
            transcriptInsightSummary = transcriptInsightSummary,
            transcriptInsights = transcriptInsights
        )
    }

    /**
     * Deterministic Strength selection per ARC-05 §8.2.
     * Evaluates improvements vs previous attempt first, then highest-performing metric in band.
     */
    fun selectStrength(
        templateId: String,
        metrics: Map<SpeechMetricKey, SpeechMetricValue>,
        previousMetrics: Map<SpeechMetricKey, SpeechMetricValue>?
    ): StrengthFeedback {
        // 1. Check for genuine improvement vs previous attempt (beyond noise band)
        if (previousMetrics != null) {
            val currFill = metrics[SpeechMetricKey.M_FILL]?.rawValue
            val prevFill = previousMetrics[SpeechMetricKey.M_FILL]?.rawValue
            if (currFill != null && prevFill != null && prevFill - currFill >= 2.0) {
                return StrengthFeedback(
                    code = StrengthCode.STR_IMPROVED,
                    title = "Noticeable Improvement",
                    message = String.format("Fewer filler words than last time: %.1f → %.1f per minute.", prevFill, currFill),
                    metricKey = SpeechMetricKey.M_FILL
                )
            }

            val currPauses = metrics[SpeechMetricKey.M_PAUSE]?.rawValue?.toInt()
            val prevPauses = previousMetrics[SpeechMetricKey.M_PAUSE]?.rawValue?.toInt()
            if (currPauses != null && prevPauses != null && prevPauses - currPauses >= 2) {
                return StrengthFeedback(
                    code = StrengthCode.STR_IMPROVED,
                    title = "Smoother Flow",
                    message = "Fewer hesitation pauses than your previous attempt: $prevPauses → $currPauses.",
                    metricKey = SpeechMetricKey.M_PAUSE
                )
            }
        }

        // 2. READ / REPEAT all words recognized as intended
        val matchMetric = metrics[SpeechMetricKey.M_MATCH]
        if (matchMetric != null && (matchMetric.rawValue ?: 0.0) >= 95.0) {
            return StrengthFeedback(
                code = StrengthCode.STR_ALL_RECOGNISED,
                title = "Excellent Articulation",
                message = "Every word was recognized as intended with ${matchMetric.displayValue} alignment.",
                metricKey = SpeechMetricKey.M_MATCH
            )
        }

        // 3. Target word used successfully
        val targetMetric = metrics[SpeechMetricKey.M_TARGET]
        if (targetMetric != null && targetMetric.rawValue == 1.0) {
            return StrengthFeedback(
                code = StrengthCode.STR_TARGET_WORD_USED,
                title = "Target Vocabulary Used",
                message = "You incorporated the target vocabulary term accurately.",
                metricKey = SpeechMetricKey.M_TARGET
            )
        }

        // 4. Framework structure complete
        val structMetric = metrics[SpeechMetricKey.M_STRUCT]
        if (structMetric != null && !structMetric.isIssue) {
            return StrengthFeedback(
                code = StrengthCode.STR_STRUCT_COMPLETE,
                title = "Complete Structure",
                message = "All stages of the framework were clearly expressed.",
                metricKey = SpeechMetricKey.M_STRUCT
            )
        }

        // 5. Pace in natural band
        val wpmMetric = metrics[SpeechMetricKey.M_WPM]
        if (wpmMetric != null && !wpmMetric.isIssue && (wpmMetric.rawValue ?: 0.0) in 100.0..160.0) {
            return StrengthFeedback(
                code = StrengthCode.STR_PACE_IN_BAND,
                title = "Comfortable Pace",
                message = "Your speaking speed (${wpmMetric.displayValue}) was right in the natural conversational band.",
                metricKey = SpeechMetricKey.M_WPM
            )
        }

        // 6. Low pauses
        val pauseMetric = metrics[SpeechMetricKey.M_PAUSE]
        if (pauseMetric != null && !pauseMetric.isIssue) {
            return StrengthFeedback(
                code = StrengthCode.STR_FEW_PAUSES,
                title = "Continuous Flow",
                message = "Steady cadence with minimal hesitation pauses.",
                metricKey = SpeechMetricKey.M_PAUSE
            )
        }

        // 7. Low fillers
        val fillMetric = metrics[SpeechMetricKey.M_FILL]
        if (fillMetric != null && !fillMetric.isIssue) {
            return StrengthFeedback(
                code = StrengthCode.STR_FEW_FILLERS,
                title = "Clean Delivery",
                message = "Very low filler word usage (${fillMetric.displayValue}).",
                metricKey = SpeechMetricKey.M_FILL
            )
        }

        // 8. Length on target
        val lenMetric = metrics[SpeechMetricKey.M_LEN]
        if (lenMetric != null && !lenMetric.isIssue) {
            return StrengthFeedback(
                code = StrengthCode.STR_LENGTH_ON_TARGET,
                title = "Target Length Reached",
                message = "Your response length hit the exercise target.",
                metricKey = SpeechMetricKey.M_LEN
            )
        }

        // 9. Vocabulary variety
        val lexMetric = metrics[SpeechMetricKey.M_LEX]
        if (lexMetric != null && !lexMetric.isIssue && (lexMetric.rawValue ?: 0.0) >= 0.55) {
            return StrengthFeedback(
                code = StrengthCode.STR_GOOD_VARIETY,
                title = "Diverse Phrasing",
                message = "Strong vocabulary variety throughout your response.",
                metricKey = SpeechMetricKey.M_LEX
            )
        }

        // Fallback: Attempt completed
        return StrengthFeedback(
            code = StrengthCode.STR_COMPLETED,
            title = "Practice Completed",
            message = "You finished a full attempt — consistent practice builds speaking confidence.",
            metricKey = null
        )
    }

    /**
     * Focus selection derived directly from highest-priority issue (ARC-05 §8.2 priorities 1–8).
     */
    fun selectFocus(issues: List<DetectedIssue>): FocusFeedback {
        val topIssue = issues.firstOrNull() ?: return FocusFeedback(
            priority = 0,
            issueCode = null,
            title = "No Mistakes Detected",
            advice = "Our checks didn't find issues in this attempt. Keep maintaining this level of consistency!",
            evidenceText = null,
            suggestionText = null
        )

        return FocusFeedback(
            priority = topIssue.code.priority,
            issueCode = topIssue.code,
            title = topIssue.code.title,
            advice = topIssue.suggestion,
            evidenceText = topIssue.evidenceText,
            suggestionText = topIssue.suggestion
        )
    }

    /**
     * Standardized advice for inconclusive outcomes (ARC-05 §5, §8.2 priority 1).
     */
    fun inconclusiveAdvice(reason: InconclusiveReason): FocusFeedback {
        val adviceText = when (reason) {
            InconclusiveReason.TOO_SHORT -> "Speak a complete sentence of at least 2–3 seconds."
            InconclusiveReason.TOO_QUIET -> "Audio was quiet. Speak closer to the microphone and at a steady volume."
            InconclusiveReason.NO_SPEECH -> "No voiced speech was detected. Check your mic permissions and try again."
            InconclusiveReason.NO_TRANSCRIPT -> "No words were recognized. Speak distinctly and ensure language pack is ready."
            InconclusiveReason.TOO_FEW_WORDS -> "Add a few more words to allow an honest, complete evaluation."
        }
        return FocusFeedback(
            priority = 1,
            issueCode = null,
            title = "Inconclusive Attempt",
            advice = adviceText,
            evidenceText = reason.name,
            suggestionText = adviceText
        )
    }
}
