package com.example.speech.pipeline

import com.example.speech.evidence.EvidenceGate
import com.example.speech.evidence.EvidenceGateResult
import com.example.speech.feedback.FeedbackComposer
import com.example.speech.feedback.TipDefinition
import com.example.speech.feedback.TipSelector
import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import com.example.speech.rules.RuleEngine
import com.example.speech.video.VideoAnalyzer
import com.example.speech.video.VideoEvaluation
import com.example.speech.video.VideoMetricKey
import com.example.speech.word.WordStageEvaluator
import com.example.speech.word.WordStageResult
import java.util.Locale

/**
 * End-to-end Local Intelligence Pipeline implementing ARC-05 §2, §5–§11 (CMP-22, CMP-37).
 * Single authoritative orchestrator replacing legacy multi-engine paths with deterministic,
 * on-device evaluation running well within the 200 ms latency budget.
 */
object LocalIntelligencePipeline {

    /**
     * Semantic version of rule set + metric definitions, stored with every attempt (SRS-FR-EVAL-011).
     *
     * Changelog:
     * - 1.4.0 (2026-11-23): technique/activity `REBAND` overrides move one side of a metric's target band
     *   (ARC-03 §6.8; CL-60) and the applied band is recorded on the metric; pace advice quotes the band applied;
     *   coach tips can come from a validated content-pack `tips.json` (ARC-03 §6.9) instead of the built-in list.
     *   With no overrides and the built-in tips, issues and tips are unchanged from 1.3.0.
     * - 1.3.0 (2026-11-23): on-device video metrics V-FRAME, V-CAMERA, V-GESTURE, V-POSTURE, V-STEADY, V-SMILE and
     *   video issues (ARC-07; D-42); self-checks drop items the video measured; placeholder source links removed;
     *   acoustic noise floor also uses the quietest 200 ms stretch (short lead-ins).
     * - 1.2.0 (2026-11-23): six grammar rules qualified by SPK-02 corpus SPK02-v1 (precision 0.939–1.000)
     *   and active; R-AGREE-02 also excludes causatives and pronoun-coordinated subjects; metrics limited to
     *   the templates they apply to (ARC-05 §6); one issue per code (grammar spans excepted); no pace headline on
     *   word stages.
     * - 1.1.0 (2026-11-23): grammar observations only from SPK-02-qualified rules (none yet, so M-GRAM
     *   is omitted); transcript-based metrics omitted without a transcript (T3b); no default level when
     *   no voiced frames were measured; technique issue overrides (SUPPRESS); R-AGREE-02 excludes
     *   auxiliary/modal contexts.
     * - 1.0.0: initial pipeline.
     */
    const val EVALUATION_VERSION = "1.4.0"

    /**
     * Executes the complete deterministic local evaluation pipeline.
     *
     * @param tipCatalog coach tips to choose from: the content pack's tips when a valid pack is loaded
     *   ([com.example.speech.feedback.TipPackLoader]), otherwise the built-in list.
     */
    fun evaluate(
        input: AttemptInput,
        tipCatalog: List<TipDefinition> = TipSelector.CATALOG
    ): AttemptEvaluationResult {
        val startTimeNs = System.nanoTime()
        val tokens = MetricEngine.tokenize(input.transcript)
        val wordCount = tokens.size

        // 1. Evidence Gate Verification (ARC-05 §5)
        val gateResult = EvidenceGate.evaluate(
            durationMs = input.durationMs,
            voicedDurationMs = input.voicedDurationMs,
            // No level evidence → null; never an invented default (SRS-BR-002).
            meanActiveRms = input.activeFrameRmsList.takeIf { it.isNotEmpty() }?.average(),
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

        // 2. Grammar rules — only SPK-02-qualified rules may run (ARC-05 §8.1, SRS-FR-EVAL-006)
        val grammarRulesAvailable = input.hasTranscript && RuleEngine.activeRules.isNotEmpty()
        val grammarIssues = if (grammarRulesAvailable) RuleEngine.evaluateGrammar(input.transcript) else emptyList()
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
            grammarHitCount = grammarHitCount,
            hasTranscript = input.hasTranscript,
            grammarRulesAvailable = grammarRulesAvailable,
            bands = IssueOverrides.targetBands(input.issueOverrides) // REBAND overrides (ARC-03 §6.8)
        ).filterKeys { MetricEngine.isApplicable(it, input.templateId, input.hasTranscript) } // ARC-05 §6 templates

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
        val suppressed = IssueOverrides.suppressed(input.issueOverrides)
        // Word-stage evidence is more specific than the generic metric check, so it goes first; one entry
        // per issue code, except grammar observations which carry distinct spans (max 2, SRS-FR-EVAL-006).
        // Video issues (ARC-07 §6) rank after audio issues by priority, so a voice problem stays the focus.
        val videoIssues = VideoAnalyzer.detectIssues(input.video)
        val allIssues = (extraIssues + metricIssues + videoIssues)
            .distinctBy { if (it.code == IssueCode.ISSUE_GRAMMAR_POSSIBLE) it.code to it.evidenceText else it.code to "" }
            .filterNot { it.code in suppressed } // technique overrides (CL-60)
            .sorted()

        // 6. Feedback & Tip Composition (ARC-05 §8.2, §8.4)
        val composed = FeedbackComposer.compose(
            attemptId = input.attemptId,
            templateId = input.templateId,
            metrics = metrics,
            issues = allIssues,
            techniqueIds = input.techniqueIds,
            previousMetrics = input.previousMetrics,
            tipCatalog = tipCatalog
        )
        // Self-checks cover only what was not measured (ARC-05 §8.4): drop items the video analyser measured.
        val measuredByVideo = selfChecksMeasuredBy(input.video)
        val feedback = if (measuredByVideo.isEmpty()) composed
        else composed.copy(selfCheckItems = composed.selfCheckItems.filterNot { it.id in measuredByVideo })

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
            video = input.video,
            evaluationLatencyMs = latencyMs
        )
    }

    /** Self-check items made redundant by a measured video metric (eye line, posture, gestures). */
    private fun selfChecksMeasuredBy(video: VideoEvaluation?): Set<String> {
        if (video !is VideoEvaluation.Valid) return emptySet()
        val ids = HashSet<String>()
        if (VideoMetricKey.V_CAMERA in video.metrics) ids += "SC-EYE-01"
        if (VideoMetricKey.V_POSTURE in video.metrics) ids += "SC-POSTURE-01"
        if (VideoMetricKey.V_GESTURE in video.metrics) ids += "SC-GESTURE-01"
        return ids
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
            "TPL-WORD-SAY" -> listOf(SpeechMetricKey.M_TARGET, SpeechMetricKey.M_WORDS)
            "TPL-WORD-USE" -> listOf(SpeechMetricKey.M_MATCH, SpeechMetricKey.M_WORDS)
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
