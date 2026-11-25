package com.example.data.evaluation

import com.example.data.model.SpeakingSession
import com.example.speech.analysis.AcousticFeatures
import com.example.speech.evidence.InconclusiveReason
import com.example.speech.feedback.TipDefinition
import com.example.speech.feedback.TipSelector
import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.MetricMethodology
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.AttemptInput
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * What a practice lab asks the speaker to do, which decides the engine template (ARC-05 §7):
 * read-aloud and shadowing with a script → `TPL-READ` checked against [referenceText]; PREP/STAR prompts →
 * `TPL-STRUCT` with [framework]; any other free speaking → `TPL-ANSWER` (with a word target only when the
 * activity defines one — never an assumed one).
 */
data class LabPracticeTarget(
    val templateId: String,
    val referenceText: String? = null,
    val framework: String? = null,
    val targetMinWords: Int? = null,
    val targetMaxWords: Int? = null
) {
    companion object {
        const val TPL_READ = "TPL-READ"
        const val TPL_ANSWER = "TPL-ANSWER"
        const val TPL_STRUCT = "TPL-STRUCT"

        /** Frameworks the engine can check (M-STRUCT cue lexicons, ARC-05 §6). Matched as upper-case words. */
        private val FRAMEWORKS = listOf("STAR", "PREP")

        /**
         * Reading a given script aloud (pronunciation drills, shadowing, voice training). Delivery markers such
         * as "[PAUSE 1s]" are not words to say and are removed. A blank script is free speaking.
         */
        fun readAloud(script: String?): LabPracticeTarget {
            val text = script.orEmpty().replace(MARKER, " ").replace(Regex("""\s+"""), " ").trim()
            return if (text.isEmpty()) answer() else LabPracticeTarget(TPL_READ, referenceText = text)
        }

        private val MARKER = Regex("""\[[^\]]*]""")

        fun answer(targetMinWords: Int? = null, targetMaxWords: Int? = null): LabPracticeTarget =
            LabPracticeTarget(TPL_ANSWER, targetMinWords = targetMinWords, targetMaxWords = targetMaxWords)

        fun structured(framework: String): LabPracticeTarget {
            val f = framework.trim().uppercase(Locale.ROOT)
            require(f in FRAMEWORKS) { "unsupported framework: $framework" }
            return LabPracticeTarget(TPL_STRUCT, framework = f)
        }

        /**
         * Free speaking about a prompt: `TPL-STRUCT` when one of [texts] names PREP or STAR as an upper-case
         * word (e.g. "PREP Opinion Sprint", "… using STAR"), otherwise `TPL-ANSWER`.
         */
        fun fromPrompt(vararg texts: String?): LabPracticeTarget {
            val joined = texts.filterNotNull().joinToString(" ")
            val framework = FRAMEWORKS.firstOrNull { Regex("""\b$it\b""").containsMatchIn(joined) }
            return if (framework != null) structured(framework) else answer()
        }
    }
}

/** The attempt being evaluated: which lab, which prompt, which recording. */
data class LabAttemptRequest(
    val sessionId: String,
    val moduleType: String,
    val promptTitle: String,
    val target: LabPracticeTarget,
    val mimeType: String = "audio/mp4",
    val localMediaPath: String = ""
)

/** How the screen's transcript was used for the attempt. */
enum class TranscriptUse {
    /** Recogniser words were evaluated with the audio. */
    USED,

    /** No transcript: audio-only feedback (tier T3b behaviour). */
    NONE,

    /** The words cannot belong to the whole recording (far too few or too many for the measured speaking time). */
    INCOMPLETE
}

/**
 * Evaluates a practice-lab recording with the local intelligence engine ([LocalIntelligencePipeline], ARC-05)
 * and maps the result into the lab screens' report and session models, so Speaking Lab, Challenges, the
 * pronunciation, shadowing, grammar and presentation labs, voice training and lesson practice get exactly the
 * feedback Coach Practice gives for the same evidence.
 *
 * Honesty (SRS-BR-002, PP-1): a score appears only when the engine gives one (read-aloud "recognised as
 * intended"); free speaking shows measured metrics instead. Inconclusive attempts carry the engine's advice and
 * no score, and are never saved. The skill scores of the legacy session model (fluency, pronunciation, …) are
 * not measured by the engine and stay 0 with no evaluated dimension. Pure Kotlin; the caller measures the audio.
 */
object LabAttemptEvaluator {

    /** † Proposed: fewer recognised words than this per minute of measured speaking time cannot cover the recording. */
    const val MIN_TRANSCRIPT_COVERAGE_WPM = 50.0

    /** † Proposed: more words than this per minute of measured speaking time cannot come from the recording. */
    const val MAX_TRANSCRIPT_COVERAGE_WPM = 350.0

    /** † The coverage check needs at least this much measured speech to be meaningful. */
    const val COVERAGE_CHECK_MIN_SPEAKING_MS = 5_000L

    /** Separator after which `SpeakingSession.audioQualityNote` carries machine-readable data (hidden in the UI). */
    const val NOTE_DATA_SEPARATOR = "|"
    private const val ENGINE_MARKER = "engine="

    /** The legacy skill dimensions the engine does not measure. */
    val LEGACY_DIMENSIONS: List<String> = listOf("fluency", "pronunciation", "vocabulary", "grammar", "delivery")

    /**
     * Evaluates one attempt. [features] is the acoustic evidence measured from the recording (`null` when the
     * recording could not be read); [transcript] is the screen's transcript — blank means none, so the engine
     * evaluates audio only.
     */
    fun evaluate(
        request: LabAttemptRequest,
        features: AcousticFeatures?,
        transcript: String?,
        tipCatalog: List<TipDefinition> = TipSelector.CATALOG,
        nowMs: Long = System.currentTimeMillis()
    ): CoachEvaluationReport {
        if (features == null) return unreadableRecordingReport(request, nowMs)
        val attempt = runEngine(request, features, transcript, tipCatalog)
        return toReport(attempt.result, request, features, attempt.transcriptUse, nowMs)
    }

    /** An engine result together with how the transcript was used for it. */
    data class EngineAttempt(val result: AttemptEvaluationResult, val transcriptUse: TranscriptUse)

    /**
     * Runs [LocalIntelligencePipeline] on measured [features] and the screen's [transcript] (dropped when blank or
     * when it cannot belong to the whole recording, see [transcriptUse]) for the request's template.
     */
    fun runEngine(
        request: LabAttemptRequest,
        features: AcousticFeatures,
        transcript: String?,
        tipCatalog: List<TipDefinition> = TipSelector.CATALOG
    ): EngineAttempt {
        val use = transcriptUse(features, transcript)
        val input = AttemptInput.fromFeatures(
            attemptId = request.sessionId,
            templateId = request.target.templateId,
            features = features,
            transcript = if (use == TranscriptUse.USED) transcript!!.trim() else null,
            referenceText = request.target.referenceText,
            targetMinWords = request.target.targetMinWords,
            targetMaxWords = request.target.targetMaxWords,
            framework = request.target.framework
        )
        return EngineAttempt(LocalIntelligencePipeline.evaluate(input, tipCatalog), use)
    }

    /** One line per issue the engine raised, Estimated ones marked (ARC-05 §8.4). */
    fun issueLines(result: AttemptEvaluationResult.Valid): List<String> = result.issues.map { issue ->
        val estimated = if (issue.code.methodology == MetricMethodology.ESTIMATED) " (estimated)" else ""
        "${issue.code.title}$estimated: ${issue.evidenceText}"
    }

    /** The focus advice (when an issue was found) followed by the selected coach tips. */
    fun tipLines(result: AttemptEvaluationResult.Valid): List<String> = buildList {
        if (result.feedback.focus.issueCode != null) add(result.feedback.focus.advice)
        result.feedback.tips.forEach { add("${it.title}: ${it.text}") }
    }.distinct()

    /** Decides whether the transcript can be evaluated together with the measured audio. */
    fun transcriptUse(features: AcousticFeatures, transcript: String?): TranscriptUse {
        val text = transcript?.trim().orEmpty()
        if (text.isEmpty()) return TranscriptUse.NONE
        val words = MetricEngine.tokenize(text).size
        if (words == 0) return TranscriptUse.NONE
        if (features.voicedDurationMs < COVERAGE_CHECK_MIN_SPEAKING_MS) return TranscriptUse.USED
        val wordsPerSpeakingMinute = words / (features.voicedDurationMs / 60_000.0)
        return if (wordsPerSpeakingMinute < MIN_TRANSCRIPT_COVERAGE_WPM || wordsPerSpeakingMinute > MAX_TRANSCRIPT_COVERAGE_WPM) {
            TranscriptUse.INCOMPLETE
        } else {
            TranscriptUse.USED
        }
    }

    /** Maps an engine result into the lab report (and, for a valid attempt, the session to save). */
    fun toReport(
        result: AttemptEvaluationResult,
        request: LabAttemptRequest,
        features: AcousticFeatures,
        transcriptUse: TranscriptUse,
        nowMs: Long = System.currentTimeMillis()
    ): CoachEvaluationReport = when (result) {
        is AttemptEvaluationResult.Inconclusive -> inconclusiveReport(result, request, features, transcriptUse, nowMs)
        is AttemptEvaluationResult.Valid -> validReport(result, request, features, transcriptUse, nowMs)
    }

    /** The engine version stored with a session evaluated by the engine, or `null` for a legacy session. */
    fun evaluationVersionOf(session: SpeakingSession): String? =
        session.audioQualityNote.substringAfter(NOTE_DATA_SEPARATOR, "")
            .split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith(ENGINE_MARKER) }
            ?.removePrefix(ENGINE_MARKER)
            ?.takeIf { it.isNotBlank() }

    fun isEngineEvaluated(session: SpeakingSession): Boolean = evaluationVersionOf(session) != null

    /** Label for the only score the engine gives on these labs (ARC-05 §7: read-aloud pass mark). */
    fun scoreLabel(result: AttemptEvaluationResult.Valid): String? =
        if (SpeechMetricKey.M_MATCH in result.allMetrics) "Recognised as intended" else null

    /** "Measured" or "Estimated" for a metric (ARC-05 §6). */
    fun methodologyLabel(key: SpeechMetricKey): String =
        if (key.methodology == MetricMethodology.MEASURED) "Measured" else "Estimated"

    // ─────────────────────────────────────────────────────────────────────────

    private fun validReport(
        result: AttemptEvaluationResult.Valid,
        request: LabAttemptRequest,
        features: AcousticFeatures,
        transcriptUse: TranscriptUse,
        nowMs: Long
    ): CoachEvaluationReport {
        val feedback = result.feedback
        val audioOnly = transcriptUse != TranscriptUse.USED
        // The engine scores only read-aloud ("recognised as intended", M-MATCH). Without that metric (no transcript)
        // there is nothing to score, whatever the template.
        val score = result.score?.takeIf { result.wordStageResult != null || SpeechMetricKey.M_MATCH in result.allMetrics }

        val strengths = listOf(feedback.strength.message)
        val mistakes = issueLines(result)
        val tips = tipLines(result)
        val rewrites = result.issues
            .mapNotNull { grammarRewrite(it) }
            .map { (before, after, _) -> "Instead of: \"$before\" → Say: \"$after\"" }
        val limitation = limitationNote(transcriptUse, request.target)
        val nextTask = if (feedback.focus.issueCode != null) feedback.focus.advice else tips.firstOrNull().orEmpty()

        val assessment = buildString {
            append(feedback.strength.message)
            append(' ')
            append(
                if (feedback.focus.issueCode != null) "Focus next: ${feedback.focus.title.lowercase(Locale.ROOT)} — ${feedback.focus.advice}"
                else feedback.focus.advice
            )
            if (limitation != null) append("\n\n").append(limitation)
        }
        val structure = result.allMetrics[SpeechMetricKey.M_STRUCT]?.let { m ->
            val found = m.detectedElements.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "none"
            "${request.target.framework ?: "Framework"} elements (estimated): ${m.displayValue} — found: $found"
        }.orEmpty()
        val wpm = result.allMetrics[SpeechMetricKey.M_WPM]?.rawValue?.takeIf { !audioOnly }?.roundToLong() ?: 0L

        val session = SpeakingSession(
            moduleType = request.moduleType,
            promptTitle = request.promptTitle.ifBlank { "Speaking Practice" },
            durationSeconds = (features.durationMs / 1000.0).roundToLong().coerceAtLeast(1L),
            localMediaPath = request.localMediaPath,
            mediaType = request.mimeType,
            transcript = if (audioOnly) "" else result.transcriptText,
            overallAssessment = assessment,
            strengths = strengths,
            mistakes = mistakes,
            betterAlternatives = rewrites,
            improvementSuggestions = tips,
            recommendedExercises = feedback.tips.map { it.title },
            pronunciationAssessable = false,
            thoughtOrganization = structure,
            nextRecommendedActivity = nextTask,
            audioQualityNote = audioNote(result, features),
            wordsPerMinute = wpm,
            evaluationStatus = CoachEvaluationState.COMPLETED.name,
            isPartialEvaluation = audioOnly,
            evaluatedDimensionKeys = emptyList(),
            unavailableDimensionKeys = LEGACY_DIMENSIONS,
            evidenceSummary = evidenceSummary(result, features, transcriptUse)
        )

        return CoachEvaluationReport(
            sessionId = request.sessionId,
            evaluationState = CoachEvaluationState.COMPLETED,
            overallScore = score,
            statusHeadline = if (audioOnly) "Audio-only feedback ready" else "Feedback ready",
            statusMessage = feedback.focus.title,
            isPartialEvaluation = audioOnly,
            partialReason = limitation,
            dimensions = emptyMap(),
            strengths = strengths,
            mistakes = mistakes,
            correctedExamples = rewrites,
            explanation = assessment,
            improvementSuggestions = tips,
            nextPracticeTask = nextTask,
            honestyNotice = HONESTY_NOTICE,
            session = session,
            validation = validation(features, result.transcriptText, transcriptUse, null),
            evaluatedAtMs = nowMs,
            engineResult = result
        )
    }

    private fun inconclusiveReport(
        result: AttemptEvaluationResult.Inconclusive,
        request: LabAttemptRequest,
        features: AcousticFeatures,
        transcriptUse: TranscriptUse,
        nowMs: Long
    ): CoachEvaluationReport {
        val message = buildString {
            append(result.reason.userMessage)
            append(' ')
            append(result.advice.advice)
            limitationNote(transcriptUse, request.target)?.let { append("\n\n").append(it) }
        }
        val v = validation(features, null, transcriptUse, result.reason)
        return CoachEvaluationReport(
            sessionId = request.sessionId,
            evaluationState = CoachEvaluationState.INSUFFICIENT_EVIDENCE,
            overallScore = null,
            statusHeadline = "Not enough to evaluate",
            statusMessage = message,
            isPartialEvaluation = false,
            partialReason = null,
            dimensions = emptyMap(),
            nextPracticeTask = result.advice.advice,
            honestyNotice = "No score was given or saved: the attempt did not have enough evidence to evaluate.",
            session = null,
            validation = v,
            evaluatedAtMs = nowMs,
            engineResult = result
        )
    }

    /** The recording could not be decoded on this device: nothing was measured, so nothing is evaluated. */
    fun unreadableRecordingReport(request: LabAttemptRequest, nowMs: Long = System.currentTimeMillis()): CoachEvaluationReport {
        val title = "Recording could not be read"
        val message = "This device could not read the audio in your recording, so it was not evaluated. Record again in the app and try once more."
        return CoachEvaluationReport(
            sessionId = request.sessionId,
            evaluationState = CoachEvaluationState.ANALYSIS_FAILED,
            overallScore = null,
            statusHeadline = title,
            statusMessage = message,
            isPartialEvaluation = false,
            partialReason = null,
            dimensions = emptyMap(),
            honestyNotice = "No score was given or saved because the audio could not be measured.",
            session = null,
            validation = EvidenceValidationResult(
                isValidForEvaluation = false,
                terminalFailureState = CoachEvaluationState.ANALYSIS_FAILED,
                primaryIssueCode = EvidenceIssueCode.UNSUPPORTED_OR_CORRUPT_AUDIO,
                userTitle = title,
                userMessage = message,
                audioValid = false,
                videoValid = false,
                videoRequired = false,
                transcriptValid = false,
                transcriptRequired = false
            ),
            evaluatedAtMs = nowMs
        )
    }

    private fun validation(
        features: AcousticFeatures,
        transcript: String?,
        transcriptUse: TranscriptUse,
        reason: InconclusiveReason?
    ): EvidenceValidationResult {
        val issue = when (reason) {
            null -> null
            InconclusiveReason.TOO_SHORT -> EvidenceIssueCode.DURATION_TOO_SHORT
            InconclusiveReason.TOO_QUIET, InconclusiveReason.NO_SPEECH -> EvidenceIssueCode.SILENT_AUDIO
            InconclusiveReason.NO_TRANSCRIPT -> EvidenceIssueCode.MISSING_TRANSCRIPT
            InconclusiveReason.TOO_FEW_WORDS -> EvidenceIssueCode.MEANINGLESS_TRANSCRIPT
        }
        return EvidenceValidationResult(
            isValidForEvaluation = reason == null,
            terminalFailureState = if (reason == null) null else CoachEvaluationState.INSUFFICIENT_EVIDENCE,
            primaryIssueCode = issue,
            userTitle = if (reason == null) "" else "Not enough to evaluate",
            userMessage = reason?.userMessage.orEmpty(),
            audioValid = reason == null || (reason != InconclusiveReason.TOO_SHORT && reason != InconclusiveReason.TOO_QUIET && reason != InconclusiveReason.NO_SPEECH),
            videoValid = false,
            videoRequired = false,
            transcriptValid = transcriptUse == TranscriptUse.USED && reason == null && !transcript.isNullOrBlank(),
            transcriptRequired = false,
            measuredDurationSeconds = features.durationMs / 1000.0,
            measuredRmsEnergy = (features.meanActiveRms ?: 0.0).toFloat(),
            voicedSeconds = features.voicedDurationMs / 1000.0,
            issues = listOfNotNull(reason?.name)
        )
    }

    private fun limitationNote(transcriptUse: TranscriptUse, target: LabPracticeTarget): String? {
        val skipped = when (target.templateId) {
            LabPracticeTarget.TPL_READ -> "checking your words against the script"
            LabPracticeTarget.TPL_STRUCT -> "pace, filler words and structure"
            else -> "pace, filler words and length"
        }
        return when (transcriptUse) {
            TranscriptUse.USED -> null
            TranscriptUse.NONE ->
                "Audio-only feedback: there was no transcript, so word-based checks ($skipped) were not made. Turn on live transcription in Settings to get them."
            TranscriptUse.INCOMPLETE ->
                "Audio-only feedback: the transcript covered only part of your recording (or did not match its length), so word-based checks ($skipped) were skipped rather than guessed."
        }
    }

    private fun audioNote(result: AttemptEvaluationResult.Valid, features: AcousticFeatures): String = buildString {
        append(String.format(Locale.ROOT, "%.1f s recorded • %.1f s speaking", features.durationMs / 1000.0, features.voicedDurationMs / 1000.0))
        if (result.hasClippingWarning) append(" • Some audio was clipped (too loud)")
        append(" $NOTE_DATA_SEPARATOR ")
        append(ENGINE_MARKER).append(result.evaluationVersion)
        append(';').append("template=").append(result.templateId)
    }

    private fun evidenceSummary(result: AttemptEvaluationResult.Valid, features: AcousticFeatures, use: TranscriptUse): String = buildString {
        append("On-device engine ${result.evaluationVersion} • ${result.templateId} • ")
        append(String.format(Locale.ROOT, "audio (%.1f s speaking)", features.voicedDurationMs / 1000.0))
        if (use == TranscriptUse.USED) {
            val words = result.allMetrics[SpeechMetricKey.M_WORDS]?.rawValue?.roundToInt()
            append(" + transcript").append(if (words != null) " ($words words)" else "")
        }
    }

    /**
     * A grammar observation as (said, suggested, why): from "Possible issue: '{span}' → '{replacement}'. {why}"
     * (RuleEngine). The span and replacement may contain apostrophes ("he don't" → "he doesn't"), so the span is
     * taken from the issue's evidence and the replacement ends at the closing "'.".
     */
    fun grammarRewrite(issue: DetectedIssue): Triple<String, String, String>? {
        if (issue.code != IssueCode.ISSUE_GRAMMAR_POSSIBLE || issue.evidenceText.isBlank()) return null
        val text = issue.suggestion
        val prefix = "'${issue.evidenceText}' → '"
        val start = text.indexOf(prefix)
        if (start < 0) return null
        val from = start + prefix.length
        val end = text.indexOf("'.", from).takeIf { it >= 0 } ?: text.lastIndexOf('\'').takeIf { it > from } ?: return null
        val after = text.substring(from, end).trim()
        if (after.isEmpty()) return null
        val why = text.substring(end + 1).removePrefix(".").trim()
        return Triple(issue.evidenceText, after, why)
    }

    private const val HONESTY_NOTICE =
        "Evaluated on this device from your recording. Metrics marked Estimated are approximate; nothing that was not measured is shown."

    /** Metrics in display order for a detail list: headline first, then the rest (ARC-05 §7). */
    fun detailMetrics(result: AttemptEvaluationResult.Valid): List<SpeechMetricValue> {
        val headline = result.headlineMetrics
        val rest = result.allMetrics.values.filter { m -> headline.none { it.key == m.key } }.sortedBy { it.key.ordinal }
        return headline + rest
    }
}
