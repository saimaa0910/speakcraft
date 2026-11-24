package com.example.speech.pipeline

import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Transcript highlights must agree with the evaluation (evaluationVersion 1.6.0; ARC-05 §8.4, CL-60,
 * SRS-FR-EVAL-006): warnings only for raised issues, nothing for technique-suppressed codes, grammar only from
 * qualified rules and at most two, and offsets that always point at the highlighted words.
 */
class TranscriptHighlightConsistencyTest {

    private fun attempt(transcript: String, overrides: Map<IssueCode, IssueOverride> = emptyMap()) = AttemptInput(
        attemptId = "att-highlights",
        templateId = "TPL-ANSWER",
        durationMs = 20_000L,
        voicedDurationMs = 15_000L,
        transcript = transcript,
        pauseCount = 0,
        activeFrameRmsList = List(50) { 0.1 + (it % 5) * 0.01 },
        noiseFloorRms = 0.005,
        issueOverrides = overrides
    )

    private val fluentWithOneFiller = "our team planned the spring launch carefully because customers asked for " +
        "faster reports um clearer screens better search simple exports and reliable alerts for everyone"

    @Test
    fun tcHigh001_aSingleFillerInAFluentAnswer_isATipNotAWarning() {
        val r = LocalIntelligencePipeline.evaluate(attempt(fluentWithOneFiller)) as AttemptEvaluationResult.Valid
        assertTrue("no filler issue expected", r.issues.none { it.code == IssueCode.ISSUE_FILLERS_HIGH })
        val filler = r.transcriptInsights.single { it.issueCode == IssueCode.ISSUE_FILLERS_HIGH }
        assertEquals(InsightSeverity.TIP, filler.severity)
        assertEquals("um", filler.highlightedText)
    }

    @Test
    fun tcHigh002_raisedFillerIssue_highlightsWarnings() {
        val messy = "um so we uh moved the launch um because the uh testing found um problems you know"
        val r = LocalIntelligencePipeline.evaluate(attempt(messy)) as AttemptEvaluationResult.Valid
        assertTrue(r.issues.any { it.code == IssueCode.ISSUE_FILLERS_HIGH })
        val fillers = r.transcriptInsights.filter { it.issueCode == IssueCode.ISSUE_FILLERS_HIGH }
        assertTrue(fillers.size >= 5)
        assertTrue(fillers.all { it.severity == InsightSeverity.WARNING })
    }

    @Test
    fun tcHigh003_techniqueSuppressedCodes_areNotHighlighted() {
        val messy = "um so we uh moved the launch um because the uh testing found um problems you know"
        val r = LocalIntelligencePipeline.evaluate(
            attempt(messy, overrides = mapOf(IssueCode.ISSUE_FILLERS_HIGH to IssueOverride.Suppress))
        ) as AttemptEvaluationResult.Valid
        assertTrue(r.issues.none { it.code == IssueCode.ISSUE_FILLERS_HIGH })
        assertTrue(r.transcriptInsights.none { it.issueCode == IssueCode.ISSUE_FILLERS_HIGH })
    }

    @Test
    fun tcHigh004_grammarHighlights_matchTheTwoReportedGrammarIssues() {
        val three = "he don't like it and she don't either so it don't matter what the team decided last week"
        val r = LocalIntelligencePipeline.evaluate(attempt(three)) as AttemptEvaluationResult.Valid
        val issueSpans = r.issues.filter { it.code == IssueCode.ISSUE_GRAMMAR_POSSIBLE }.map { it.evidenceText }
        val highlightSpans = r.transcriptInsights.filter { it.issueCode == IssueCode.ISSUE_GRAMMAR_POSSIBLE }.map { it.highlightedText }
        assertEquals(TranscriptAnalyzer.MAX_GRAMMAR_INSIGHTS, highlightSpans.size)
        assertEquals(issueSpans, highlightSpans)
    }

    @Test
    fun tcHigh005_offsetsStayExact_whenLowercasingWouldChangeTheTextLength() {
        // 'İ' lowercases to two characters; offsets must still point at the original words.
        val transcript = "İstanbul was um busy and İzmir was uh calm, sort of"
        val insights = TranscriptAnalyzer.analyze(transcript)
        assertTrue(insights.isNotEmpty())
        insights.forEach { assertEquals(it.highlightedText, transcript.substring(it.spanStart, it.spanEnd)) }
        assertTrue(insights.any { it.highlightedText == "um" } && insights.any { it.highlightedText == "sort of" })
    }

    @Test
    fun tcHigh006_withoutATranscript_thereAreNoHighlights() {
        val r = LocalIntelligencePipeline.evaluate(attempt("").copy(hasTranscript = false)) as AttemptEvaluationResult.Valid
        assertTrue(r.transcriptInsights.isEmpty())
    }
}
