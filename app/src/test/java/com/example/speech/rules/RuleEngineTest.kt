package com.example.speech.rules

import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    @Test
    fun tcRule001_thirdPersonAgreement_detectedAccurately() {
        val transcript = "he don't understand the problem and she have a new idea"
        val issues = RuleEngine.evaluateGrammar(transcript)

        assertEquals(2, issues.size)
        assertTrue(issues.any { it.ruleId == "R-AGREE-01" && it.evidenceText.contains("he don't") })
        assertTrue(issues.any { it.ruleId == "R-AGREE-02" && it.evidenceText.contains("she have") })
    }

    @Test
    fun tcRule002_doubleComparative_detectedAccurately() {
        val transcript = "this design is more better and more faster than before"
        val issues = RuleEngine.evaluateGrammar(transcript)

        assertEquals(2, issues.size)
        assertTrue(issues.all { it.ruleId == "R-COMP-01" })
    }

    @Test
    fun tcRule003_pastAuxiliaryBaseVerb_detectedAccurately() {
        val transcript = "we didn't went to the office and they did not saw the report"
        val issues = RuleEngine.evaluateGrammar(transcript)

        assertEquals(2, issues.size)
        assertTrue(issues.all { it.ruleId == "R-PAST-01" })
    }

    @Test
    fun tcRule004_participleAfterHave_detectedAccurately() {
        val transcript = "I have went there three times this month"
        val issues = RuleEngine.evaluateGrammar(transcript)

        assertEquals(1, issues.size)
        assertEquals("R-PART-01", issues[0].ruleId)
    }

    @Test
    fun tcRule005_articleBeforeVowelSound_detectedAccurately() {
        val transcript = "she gave me a apple and waited a hour"
        val issues = RuleEngine.evaluateGrammar(transcript)

        assertEquals(2, issues.size)
        assertTrue(issues.all { it.ruleId == "R-ARTICLE-01" })
    }

    @Test
    fun tcRule006_negativeCases_noFalsePositives() {
        val cleanTranscript = "they don't mind because I have an apple and waited an hour. " +
                "This solution is better and faster. We did not go yesterday."
        val issues = RuleEngine.evaluateGrammar(cleanTranscript)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun tcRule007_issueOrdering_strictlyFollowsAuthoritativePriority() {
        // Construct metrics with multiple issues:
        // Priority 1: M-LEVEL (Too quiet)
        // Priority 2: M-MATCH (Alignment < 75%)
        // Priority 4: M-FILL (Fillers high)
        // Priority 5: M-WPM (Pace fast)
        // Priority 8: Grammar rule hit
        val metrics = MetricEngine.computeMetrics(
            durationMs = 20000L,
            voicedDurationMs = 15000L,
            transcript = "he don't know the answer um like um you know",
            pauseCount = 0,
            referenceText = "he knows the answer clearly and speaks directly",
            activeFrameRmsList = listOf(0.001, 0.20, 0.002, 0.25) // CV > 0.7
        )

        val issues = RuleEngine.detectIssues(
            metrics = metrics,
            transcript = "he don't know the answer um like um you know",
            templateId = "TPL-READ"
        )

        assertFalse(issues.isEmpty())
        val priorities = issues.map { it.code.priority }
        // Verify priorities are non-decreasing (sorted ascending)
        for (i in 0 until priorities.size - 1) {
            assertTrue("Expected priority order violated: ${priorities[i]} <= ${priorities[i + 1]}",
                priorities[i] <= priorities[i + 1])
        }

        assertEquals(IssueCode.ISSUE_TOO_QUIET, issues.first().code)
    }
}
