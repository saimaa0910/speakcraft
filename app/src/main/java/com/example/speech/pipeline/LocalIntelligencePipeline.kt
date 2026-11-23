package com.example.speech.pipeline

import com.example.speech.evidence.EvidenceGate
import com.example.speech.evidence.EvidenceGateResult
import com.example.speech.feedback.FeedbackComposer
import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.RuleEngine
import com.example.speech.word.WordStageEvaluator
import com.example.speech.word.WordStageResult
import java.util.Locale

/**
 * End-to-end Local Intelligence Pipeline implementing ARC-05 §2, §5–§11 (CMP-22, CMP-37).
 * Single authoritative orchestrator replacing legacy multi-engine paths with deterministic,
 * on-device evaluation running well within the 200 ms latency budget.
 */
object LocalIntelligencePipeline {

    const val EVALUATION_VERSION = "1.0.0"

    /**
     * Executes the complete deterministic local evaluation pipeline.
     */
    fun evaluate(input: AttemptInput): AttemptEvaluationResult {
        val startTimeNs = System.nanoTime()
        val tokens = MetricEngine.tokenize(input.transcript)
        val wordCount = tokens.size

        // 1. Evidence Gate Verification (ARC-05 §5)
        val gateResult = EvidenceGate.evaluate(
            durationMs = input.durationMs,
            voicedDurationMs = input.voicedDurationMs,
            meanActiveRms = if (input.activeFrameRmsList.isNotEmpty()) input.activeFrameRmsList.average() else 0.05,
            noiseFloorRms = input.noiseFloorRms,
            wordCount = wordCount,
            templateId = input.templateId,
            hasAudio = input.hasAudio,
            hasTranscript = input.hasTranscript,
            clippingRatio = input.clippingRatio
        )

        if (gateResult is EvidenceGateResult.Inconclusive) {
            val advice = FeedbackComposer.inconclusiveAdvice(gateResult.reason)
            return AttemptEvaluationResult.Inconclusive(
                attemptId = input.attemptId,
                templateId = input.templateId,
                reason = gateResult.reason,
                advice = advice,
                durationMs = input.durationMs
            )
        }

        val hasClipping = (gateResult as? EvidenceGateResult.Valid)?.hasClippingWarning ?: false

        // 2. High-precision Grammar Rule Evaluation (ARC-05 §8.1)
        val grammarIssues = RuleEngine.evaluateGrammar(input.transcript)
        val grammarHitCount = grammarIssues.size

        // 3. Metric Engine Calculation (ARC-05 §6)
        val metrics = MetricEngine.computeMetrics(
            durationMs = input.durationMs,
            voicedDurationMs = input.voicedDurationMs,
            transcript = input.transcript,
            pauseCount = input.pauseCount,
            activeFrameRmsList = input.activeFrameRmsList,
            referenceText = input.referenceText,
            targetWord = input.targetWord,
            targetWordInflections = input.targetWordInflections,
            targetMinWords = input.targetMinWords,
            targetMaxWords = input.targetMaxWords,
            framework = input.framework,
            grammarHitCount = grammarHitCount
        )

        // 4. Word-Stage Evaluation (if applicable, ARC-05 §9)
        var wordStageResult: WordStageResult? = null
        val extraIssues = mutableListOf<DetectedIssue>()

        when (input.templateId.uppercase(Locale.ROOT)) {
            "TPL-WORD-SAY" -> {
                if (!input.targetWord.isNullOrBlank()) {
                    val result = WordStageEvaluator.evaluateSayIt(
                        targetWord = input.targetWord,
                        acceptedInflections = input.targetWordInflections,
                        transcript = input.transcript
                    )
                    wordStageResult = result
                    extraIssues.addAll(result.issues)
                }
            }
            "TPL-WORD-USE" -> {
                if (!input.referenceText.isNullOrBlank()) {
                    val result = WordStageEvaluator.evaluateUseIt(
                        referenceSentences = listOf(input.referenceText),
                        transcripts = listOf(input.transcript)
                    )
                    wordStageResult = result
                    extraIssues.addAll(result.issues)
                }
            }
            "TPL-WORD-OWN" -> {
                if (!input.targetWord.isNullOrBlank()) {
                    val result = WordStageEvaluator.evaluateMakeYourOwn(
                        targetWord = input.targetWord,
                        acceptedInflections = input.targetWordInflections,
                        transcript = input.transcript,
                        grammarHitCount = grammarHitCount
                    )
                    wordStageResult = result
                    extraIssues.addAll(result.issues)
                }
            }
        }

        // 5. Issue Detection & Synthesis (ARC-05 §8.4)
        val metricIssues = RuleEngine.detectIssues(
            metrics = metrics,
            transcript = input.transcript,
            templateId = input.templateId,
            targetWord = input.targetWord
        )
        val allIssues = (metricIssues + extraIssues).distinctBy { it.code to it.evidenceText }.sorted()

        // 6. Feedback & Tip Composition (ARC-05 §8.2, §8.4)
        val feedback = FeedbackComposer.compose(
            attemptId = input.attemptId,
            templateId = input.templateId,
            metrics = metrics,
            issues = allIssues,
            techniqueIds = input.techniqueIds,
            previousMetrics = input.previousMetrics
        )

        // 7. Headline Metrics Selection (ARC-05 §7)
        val headlineKeys = selectHeadlineKeys(input.templateId, input.hasTranscript)
        val headlineMetrics = headlineKeys.mapNotNull { metrics[it] }

        // 8. Pass/Fail & Score (ARC-05 §7, §9)
        val (passed, score) = calculatePassFailAndScore(
            templateId = input.templateId,
            metrics = metrics,
            wordStageResult = wordStageResult
        )

        val durationNs = System.nanoTime() - startTimeNs
        val latencyMs = durationNs / 1_000_000L

        return AttemptEvaluationResult.Valid(
            attemptId = input.attemptId,
            templateId = input.templateId,
            headlineMetrics = headlineMetrics,
            allMetrics = metrics,
            feedback = feedback,
            issues = allIssues,
            passed = passed,
            score = score,
            hasClippingWarning = hasClipping,
            wordStageResult = wordStageResult,
            evaluationLatencyMs = latencyMs
        )
    }

    /**
     * Determines the 3 headline metrics displayed on the attempt summary card (ARC-05 §7).
     */
    fun selectHeadlineKeys(templateId: String, hasTranscript: Boolean): List<SpeechMetricKey> {
        if (!hasTranscript) {
            return listOf(SpeechMetricKey.M_SPK, SpeechMetricKey.M_PAUSE, SpeechMetricKey.M_RATIO)
        }

        return when (templateId.uppercase(Locale.ROOT)) {
            "TPL-READ", "READ" -> listOf(SpeechMetricKey.M_MATCH, SpeechMetricKey.M_WPM, SpeechMetricKey.M_PAUSE)
            "TPL-REPEAT", "REPEAT" -> listOf(SpeechMetricKey.M_MATCH, SpeechMetricKey.M_SPK, SpeechMetricKey.M_WORDS)
            "TPL-ANSWER", "ANSWER" -> listOf(SpeechMetricKey.M_WPM, SpeechMetricKey.M_FILL, SpeechMetricKey.M_LEN)
            "TPL-STRUCT", "STRUCT" -> listOf(SpeechMetricKey.M_STRUCT, SpeechMetricKey.M_WPM, SpeechMetricKey.M_FILL)
            "TPL-STORY", "STORY" -> listOf(SpeechMetricKey.M_LEN, SpeechMetricKey.M_PAUSE, SpeechMetricKey.M_LEX)
            "TPL-WORD-SAY" -> listOf(SpeechMetricKey.M_TARGET, SpeechMetricKey.M_WORDS, SpeechMetricKey.M_WPM)
            "TPL-WORD-USE" -> listOf(SpeechMetricKey.M_MATCH, SpeechMetricKey.M_WORDS, SpeechMetricKey.M_WPM)
            "TPL-WORD-OWN" -> listOf(SpeechMetricKey.M_TARGET, SpeechMetricKey.M_WORDS, SpeechMetricKey.M_GRAM)
            else -> listOf(SpeechMetricKey.M_WPM, SpeechMetricKey.M_FILL, SpeechMetricKey.M_PAUSE)
        }
    }

    private fun calculatePassFailAndScore(
        templateId: String,
        metrics: Map<SpeechMetricKey, SpeechMetricValue>,
        wordStageResult: WordStageResult?
    ): Pair<Boolean?, Int?> {
        if (wordStageResult != null) {
            return wordStageResult.passed to wordStageResult.score
        }

        return when (templateId.uppercase(Locale.ROOT)) {
            "TPL-READ", "READ", "TPL-REPEAT", "REPEAT" -> {
                val matchScore = metrics[SpeechMetricKey.M_MATCH]?.rawValue?.toInt() ?: 0
                val passed = matchScore >= 75
                passed to matchScore
            }
            // Free speaking exercises do not have artificial pass/fail marks (SRS-BR-006, D-06)
            else -> null to null
        }
    }
}
