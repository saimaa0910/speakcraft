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
        val issues = RuleEngine.evaluateGrammar(transcript, RuleEngine.CANDIDATE_RULES)

        assertEquals(2, issues.size)
        assertTrue(issues.any { it.ruleId == "R-AGREE-01" && it.evidenceText.contains("he don't") })
        assertTrue(issues.any { it.ruleId == "R-AGREE-02" && it.evidenceText.contains("she have") })
    }

    @Test
    fun tcRule002_doubleComparative_detectedAccurately() {
        val transcript = "this design is more better and more faster than before"
        val issues = RuleEngine.evaluateGrammar(transcript, RuleEngine.CANDIDATE_RULES)

        assertEquals(2, issues.size)
        assertTrue(issues.all { it.ruleId == "R-COMP-01" })
    }

    @Test
    fun tcRule003_pastAuxiliaryBaseVerb_detectedAccurately() {
        val transcript = "we didn't went to the office and they did not saw the report"
        val issues = RuleEngine.evaluateGrammar(transcript, RuleEngine.CANDIDATE_RULES)

        assertEquals(2, issues.size)
        assertTrue(issues.all { it.ruleId == "R-PAST-01" })
    }

    @Test
    fun tcRule004_participleAfterHave_detectedAccurately() {
        val transcript = "I have went there three times this month"
        val issues = RuleEngine.evaluateGrammar(transcript, RuleEngine.CANDIDATE_RULES)

        assertEquals(1, issues.size)
        assertEquals("R-PART-01", issues[0].ruleId)
    }

    @Test
    fun tcRule005_articleBeforeVowelSound_detectedAccurately() {
        val transcript = "she gave me a apple and waited a hour"
        val issues = RuleEngine.evaluateGrammar(transcript, RuleEngine.CANDIDATE_RULES)

        assertEquals(2, issues.size)
        assertTrue(issues.all { it.ruleId == "R-ARTICLE-01" })
    }

    @Test
    fun tcRule006_negativeCases_noFalsePositives() {
        val cleanTranscript = "they don't mind because I have an apple and waited an hour. " +
                "This solution is better and faster. We did not go yesterday."
        val issues = RuleEngine.evaluateGrammar(cleanTranscript, RuleEngine.CANDIDATE_RULES)

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

    @Test
    fun tcRule008_onlyRegisteredRulesAreActive() {
        // SRS-FR-EVAL-006: a rule runs in the app only if SPK-02 recorded its qualification.
        assertEquals(RuleQualificationRegistry.QUALIFIED.keys, RuleEngine.activeRules.map { it.id }.toSet())
        assertTrue(RuleEngine.CANDIDATE_RULES.map { it.id }.containsAll(RuleQualificationRegistry.QUALIFIED.keys))
        val hits = RuleEngine.evaluateGrammar("he don't understand the plan and she have a car")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { RuleQualificationRegistry.isQualified(it.ruleId!!) })
    }

    @Test
    fun tcRule009_agreeHave_doesNotFireInAuxiliaryOrModalContexts() {
        val correct = listOf(
            "does she have a car",
            "would he have known the answer",
            "did it have any effect",
            "she wants to have lunch and he needs to have a break"
        )
        for (text in correct) {
            val hits = RuleEngine.evaluateGrammar(text, RuleEngine.CANDIDATE_RULES).filter { it.ruleId == "R-AGREE-02" }
            assertTrue("False positive on: $text", hits.isEmpty())
        }
        assertEquals(1, RuleEngine.evaluateGrammar("she have a car", RuleEngine.CANDIDATE_RULES).count { it.ruleId == "R-AGREE-02" })
    }

    @Test
    fun tcRule010_qualificationHarness_requiresCorpusSizeAndPrecision() {
        val rule = RuleEngine.CANDIDATE_RULES.first { it.id == "R-COMP-01" }
        val positives = (1..30).map { LabelledExample("this one is more better than number $it", true) }
        val negatives = (1..30).map { LabelledExample("this one is better than number $it", false) }

        val report = RuleQualificationHarness.evaluate(rule, positives + negatives)
        assertEquals(30, report.truePositives)
        assertEquals(0, report.falsePositives)
        assertEquals(1.0, report.precision!!, 1e-9)
        assertTrue(report.qualifies)

        // Too small a corpus never qualifies, even at perfect precision (ARC-05 §8.1).
        val small = RuleQualificationHarness.evaluate(rule, positives.take(10) + negatives.take(10))
        assertFalse(small.qualifies)

        // Precision below 0.90 does not qualify: 30 TP + 4 FP = 0.88.
        val noisyNegatives = negatives.take(26) + (1..4).map { LabelledExample("we need more better tools $it", false) }
        val noisy = RuleQualificationHarness.evaluate(rule, positives + noisyNegatives)
        assertEquals(4, noisy.falsePositives)
        assertFalse(noisy.qualifies)
    }
}
