package com.example.speech.feedback

import com.example.speech.evidence.InconclusiveReason
import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackComposerTest {

    @Test
    fun tcFeedback001_strength_recognisesImprovementVsPrevious() {
        val prevMetrics = mapOf(
            SpeechMetricKey.M_FILL to SpeechMetricValue(
                key = SpeechMetricKey.M_FILL,
                rawValue = 6.0,
                displayValue = "6.0 / min",
                band = "Frequent",
                isIssue = true
            )
        )
        val currentMetrics = mapOf(
            SpeechMetricKey.M_FILL to SpeechMetricValue(
                key = SpeechMetricKey.M_FILL,
                rawValue = 2.0, // Decreased by 4.0 >= 2.0
                displayValue = "2.0 / min",
                band = "Low",
                isIssue = false
            ),
            SpeechMetricKey.M_WPM to SpeechMetricValue(
                key = SpeechMetricKey.M_WPM,
                rawValue = 130.0,
                displayValue = "130 wpm",
                band = "Comfortable",
                isIssue = false
            )
        )

        val strength = FeedbackComposer.selectStrength(
            templateId = "TPL-ANSWER",
            metrics = currentMetrics,
            previousMetrics = prevMetrics
        )

        assertEquals(StrengthCode.STR_IMPROVED, strength.code)
        assertTrue(strength.message.contains("Fewer filler words"))
    }

    @Test
    fun tcFeedback002_strength_readAllRecognised() {
        val currentMetrics = mapOf(
            SpeechMetricKey.M_MATCH to SpeechMetricValue(
                key = SpeechMetricKey.M_MATCH,
                rawValue = 100.0,
                displayValue = "100 %",
                band = "Pass",
                isIssue = false
            )
        )

        val strength = FeedbackComposer.selectStrength(
            templateId = "TPL-READ",
            metrics = currentMetrics,
            previousMetrics = null
        )

        assertEquals(StrengthCode.STR_ALL_RECOGNISED, strength.code)
    }

    @Test
    fun tcFeedback003_focus_matchesHighestPriorityIssue() {
        val issues = listOf(
            DetectedIssue(
                code = IssueCode.ISSUE_TOO_QUIET,
                evidenceText = "Loudness variability",
                suggestion = "Speak closer to the microphone"
            ),
            DetectedIssue(
                code = IssueCode.ISSUE_FILLERS_HIGH,
                evidenceText = "5.0 / min",
                suggestion = "Pause silently instead"
            )
        )

        val focus = FeedbackComposer.selectFocus(issues)
        assertEquals(1, focus.priority)
        assertEquals(IssueCode.ISSUE_TOO_QUIET, focus.issueCode)
    }

    @Test
    fun tcFeedback004_focus_noIssuesDetected_returnsPositiveFocus() {
        val focus = FeedbackComposer.selectFocus(emptyList())
        assertEquals(0, focus.priority)
        assertEquals("No Mistakes Detected", focus.title)
    }

    @Test
    fun tcFeedback005_inconclusiveAdvice_coversAllReasons() {
        for (reason in InconclusiveReason.entries) {
            val advice = FeedbackComposer.inconclusiveAdvice(reason)
            assertNotNull(advice.advice)
            assertTrue(advice.advice.isNotBlank())
            assertEquals(1, advice.priority)
        }
    }

    @Test
    fun tcFeedback006_tipSelector_focusTipForTopIssueAndDeterministicHash() {
        val issues = listOf(
            DetectedIssue(
                code = IssueCode.ISSUE_FILLERS_HIGH,
                evidenceText = "4 fillers",
                suggestion = "Pause silently instead"
            )
        )

        val tips1 = TipSelector.select(
            attemptId = "attempt-001",
            issues = issues,
            metrics = emptyMap(),
            techniqueIds = listOf("TECH-SILENT-PAUSE")
        )

        assertFalse(tips1.isEmpty())
        val focusTip = tips1.first()
        assertTrue(focusTip.isFocusTip)
        assertEquals(IssueCode.ISSUE_FILLERS_HIGH, focusTip.forIssue)
        assertEquals("The Power of the Silent Pause", focusTip.title)
    }

    @Test
    fun tcFeedback007_tipSelector_noIssue_returnsGeneralTechniqueTip() {
        val tips = TipSelector.select(
            attemptId = "attempt-002",
            issues = emptyList(),
            metrics = emptyMap()
        )

        assertEquals(1, tips.size)
        assertTrue(tips[0].isFocusTip)
        assertEquals("Vocal Resonance & Posture", tips[0].title)
    }
}
