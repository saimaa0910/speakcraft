package com.example.speech.pipeline

import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptAnalyzerTest {

    @Test
    fun tcTrans001_emptyAndBlankTranscript_returnsEmptyList() {
        val emptyInsights = TranscriptAnalyzer.analyze("")
        assertTrue(emptyInsights.isEmpty())
        assertEquals("No issues detected in your speech", TranscriptAnalyzer.composeSummary(emptyInsights))

        val blankInsights = TranscriptAnalyzer.analyze("   \n\t  ")
        assertTrue(blankInsights.isEmpty())
        assertEquals("No issues detected in your speech", TranscriptAnalyzer.composeSummary(blankInsights))
    }

    @Test
    fun tcTrans002_grammarAgreementError_detectsErrorWithOffsetsAndSuggestion() {
        val transcript = "he don't understand the system"
        val insights = TranscriptAnalyzer.analyze(transcript)

        val grammar = insights.filter { it.severity == InsightSeverity.ERROR }
        assertEquals(1, grammar.size)

        val issue = grammar.first()
        assertEquals(IssueCode.ISSUE_GRAMMAR_POSSIBLE, issue.issueCode)
        assertEquals(0, issue.spanStart)
        assertEquals(8, issue.spanEnd)
        assertEquals("he don't", issue.highlightedText)
        assertTrue(issue.suggestion.contains("doesn't"))
    }

    @Test
    fun tcTrans003_fillerWords_detectsWarningSeverity() {
        val transcript = "um so basically we you know completed the task uh yesterday"
        val insights = TranscriptAnalyzer.analyze(transcript)

        val fillers = insights.filter { it.issueCode == IssueCode.ISSUE_FILLERS_HIGH }
        assertTrue("Expected at least 3 fillers, got ${fillers.size}", fillers.size >= 3)
        assertTrue(fillers.all { it.severity == InsightSeverity.WARNING })
        assertTrue(fillers.any { it.highlightedText.equals("um", ignoreCase = true) })
        assertTrue(fillers.any { it.highlightedText.equals("you know", ignoreCase = true) })
        assertTrue(fillers.any { it.highlightedText.equals("uh", ignoreCase = true) })
    }

    @Test
    fun tcTrans004_hedgingPhrases_detectsWarningSeverity() {
        val transcript = "this is sort of an interesting problem and perhaps we can solve it"
        val insights = TranscriptAnalyzer.analyze(transcript)

        val hedges = insights.filter { it.issueCode == IssueCode.ISSUE_HEDGING_HIGH }
        assertTrue("Expected at least 2 hedges, got ${hedges.size}", hedges.size >= 2)
        assertTrue(hedges.all { it.severity == InsightSeverity.WARNING })
        assertTrue(hedges.any { it.highlightedText.equals("sort of", ignoreCase = true) })
        assertTrue(hedges.any { it.highlightedText.equals("perhaps", ignoreCase = true) })
    }

    @Test
    fun tcTrans005_overlappingSpanDeduplication_keepsLongerOrHigherSeverity() {
        val transcript = "i think maybe we should consider other alternatives"
        val insights = TranscriptAnalyzer.analyze(transcript)

        // "i think maybe" should subsume "maybe"
        val hedgeInsights = insights.filter { it.issueCode == IssueCode.ISSUE_HEDGING_HIGH }
        assertEquals(1, hedgeInsights.size)
        assertEquals("i think maybe", hedgeInsights.first().highlightedText.lowercase())
    }

    @Test
    fun tcTrans006_composeSummary_formatsPluralsCorrectly() {
        // Single filler
        val oneFiller = listOf(
            TranscriptInsight(
                issueCode = IssueCode.ISSUE_FILLERS_HIGH,
                spanStart = 0,
                spanEnd = 2,
                highlightedText = "um",
                issueTitle = IssueCode.ISSUE_FILLERS_HIGH.title,
                suggestion = "Pause silently",
                severity = InsightSeverity.WARNING
            )
        )
        assertEquals("1 filler word detected", TranscriptAnalyzer.composeSummary(oneFiller))

        // Multiple mixed
        val mixed = listOf(
            TranscriptInsight(
                issueCode = IssueCode.ISSUE_GRAMMAR_POSSIBLE,
                spanStart = 0,
                spanEnd = 8,
                highlightedText = "he don't",
                issueTitle = IssueCode.ISSUE_GRAMMAR_POSSIBLE.title,
                suggestion = "Use doesn't",
                severity = InsightSeverity.ERROR
            ),
            TranscriptInsight(
                issueCode = IssueCode.ISSUE_FILLERS_HIGH,
                spanStart = 10,
                spanEnd = 12,
                highlightedText = "um",
                issueTitle = IssueCode.ISSUE_FILLERS_HIGH.title,
                suggestion = "Pause silently",
                severity = InsightSeverity.WARNING
            ),
            TranscriptInsight(
                issueCode = IssueCode.ISSUE_FILLERS_HIGH,
                spanStart = 20,
                spanEnd = 22,
                highlightedText = "uh",
                issueTitle = IssueCode.ISSUE_FILLERS_HIGH.title,
                suggestion = "Pause silently",
                severity = InsightSeverity.WARNING
            )
        )
        assertEquals("1 possible grammar issue and 2 filler words detected", TranscriptAnalyzer.composeSummary(mixed))
    }

    @Test
    fun tcTrans007_characterOffsets_strictlyWithinTranscriptBounds() {
        val transcript = "well um he don't know and i think maybe we made a apple mistake"
        val insights = TranscriptAnalyzer.analyze(transcript)

        assertTrue(insights.isNotEmpty())
        for (insight in insights) {
            assertTrue("spanStart >= 0", insight.spanStart >= 0)
            assertTrue("spanEnd <= length", insight.spanEnd <= transcript.length)
            assertTrue("spanStart < spanEnd", insight.spanStart < insight.spanEnd)

            val substring = transcript.substring(insight.spanStart, insight.spanEnd)
            assertEquals("Span substring must match highlighted text", insight.highlightedText, substring)
        }
    }
}
