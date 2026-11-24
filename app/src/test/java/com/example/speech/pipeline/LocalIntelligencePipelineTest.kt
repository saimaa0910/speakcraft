package com.example.speech.pipeline

import com.example.speech.evidence.InconclusiveReason
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.word.WordStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalIntelligencePipelineTest {

    @Test
    fun tcPipe001_inconclusiveAttempt_shortDuration_returnsInconclusive() {
        val input = AttemptInput(
            attemptId = "att-001",
            templateId = "TPL-READ",
            durationMs = 1200L, // < 1500 ms
            voicedDurationMs = 800L,
            transcript = "hello world",
            pauseCount = 0
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Inconclusive)
        val inc = result as AttemptEvaluationResult.Inconclusive
        assertEquals(InconclusiveReason.TOO_SHORT, inc.reason)
        assertEquals("1.5.0", inc.evaluationVersion)
        assertNotNull(inc.advice)
    }

    @Test
    fun tcPipe002_tplRead_passing_evaluatesHeadlinesAndPassStatus() {
        val reference = "explain the core architectural concepts clearly to the team"
        val input = AttemptInput(
            attemptId = "att-002",
            templateId = "TPL-READ",
            durationMs = 6000L,
            voicedDurationMs = 5000L,
            transcript = "explain the core architectural concepts clearly to the team",
            pauseCount = 0,
            referenceText = reference
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Valid)
        val valid = result as AttemptEvaluationResult.Valid

        assertEquals("1.5.0", valid.evaluationVersion)
        assertEquals(true, valid.passed)
        assertEquals(100, valid.score)

        // Headlines for READ: M-MATCH, M-WPM, M-PAUSE
        assertEquals(3, valid.headlineMetrics.size)
        assertEquals(SpeechMetricKey.M_MATCH, valid.headlineMetrics[0].key)
        assertEquals(SpeechMetricKey.M_WPM, valid.headlineMetrics[1].key)
        assertEquals(SpeechMetricKey.M_PAUSE, valid.headlineMetrics[2].key)

        assertTrue("Evaluation latency must be within 200 ms budget", valid.evaluationLatencyMs <= 200)
    }

    @Test
    fun tcPipe003_tplAnswer_freeSpeaking_hasNoPassFail() {
        val input = AttemptInput(
            attemptId = "att-003",
            templateId = "TPL-ANSWER",
            durationMs = 15000L,
            voicedDurationMs = 12000L,
            transcript = "In my previous experience we handled system resilience by designing for graceful degradation",
            pauseCount = 1,
            targetMinWords = 15
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Valid)
        val valid = result as AttemptEvaluationResult.Valid

        // Free speaking has no artificial pass/fail (SRS-BR-006, D-06)
        assertNull(valid.passed)
        assertNull(valid.score)

        // Headlines for ANSWER: M-WPM, M-FILL, M-LEN
        assertEquals(3, valid.headlineMetrics.size)
        assertEquals(SpeechMetricKey.M_WPM, valid.headlineMetrics[0].key)
        assertEquals(SpeechMetricKey.M_FILL, valid.headlineMetrics[1].key)
        assertEquals(SpeechMetricKey.M_LEN, valid.headlineMetrics[2].key)
    }

    @Test
    fun tcPipe004_tplStruct_starFramework_evaluatesElements() {
        val transcript = "When I was lead, my task was to migrate the database. " +
                "I decided to run shadow writes. As a result, we had zero downtime."
        val input = AttemptInput(
            attemptId = "att-004",
            templateId = "TPL-STRUCT",
            durationMs = 20000L,
            voicedDurationMs = 16000L,
            transcript = transcript,
            pauseCount = 1,
            framework = "STAR"
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Valid)
        val valid = result as AttemptEvaluationResult.Valid

        val structMetric = valid.allMetrics[SpeechMetricKey.M_STRUCT]
        assertNotNull(structMetric)
        assertEquals(4.0, structMetric?.rawValue ?: 0.0, 0.01)
        assertFalse(structMetric?.isIssue ?: true)

        // Headlines for STRUCT: M-STRUCT, M-WPM, M-FILL
        assertEquals(SpeechMetricKey.M_STRUCT, valid.headlineMetrics[0].key)
    }

    @Test
    fun tcPipe005_audioOnlyT3b_evaluatesAcousticsWithoutTranscript() {
        val input = AttemptInput(
            attemptId = "att-005",
            templateId = "T3B",
            durationMs = 10000L,
            voicedDurationMs = 7000L,
            transcript = "",
            pauseCount = 1,
            hasAudio = true,
            hasTranscript = false
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Valid)
        val valid = result as AttemptEvaluationResult.Valid

        // Headlines for audio-only: M-SPK, M-PAUSE, M-RATIO
        assertEquals(3, valid.headlineMetrics.size)
        assertEquals(SpeechMetricKey.M_SPK, valid.headlineMetrics[0].key)
        assertEquals(SpeechMetricKey.M_PAUSE, valid.headlineMetrics[1].key)
        assertEquals(SpeechMetricKey.M_RATIO, valid.headlineMetrics[2].key)
    }

    @Test
    fun tcPipe006_wordStageSayIt_evaluatesWordAndIssues() {
        val input = AttemptInput(
            attemptId = "att-006",
            templateId = "TPL-WORD-SAY",
            durationMs = 2500L,
            voicedDurationMs = 1800L,
            transcript = "collaborate",
            pauseCount = 0,
            targetWord = "collaborate",
            targetWordInflections = listOf("collaborates", "collaborating")
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Valid)
        val valid = result as AttemptEvaluationResult.Valid

        assertEquals(true, valid.passed)
        assertEquals(100, valid.score)
        assertNotNull(valid.wordStageResult)
        assertEquals(WordStage.SAY_IT, valid.wordStageResult?.stage)
    }

    @Test
    fun tcPipe007_wordStageMakeYourOwn_passingAttempt() {
        val input = AttemptInput(
            attemptId = "att-007",
            templateId = "TPL-WORD-OWN",
            durationMs = 8000L,
            voicedDurationMs = 6000L,
            transcript = "We must collaborate closely to ensure the architecture succeeds",
            pauseCount = 0,
            targetWord = "collaborate",
            targetWordInflections = listOf("collaborates", "collaborating")
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Valid)
        val valid = result as AttemptEvaluationResult.Valid

        assertEquals(true, valid.passed)
        assertEquals(100, valid.score)
        assertNotNull(valid.wordStageResult)
        assertEquals(WordStage.MAKE_YOUR_OWN, valid.wordStageResult?.stage)
    }

    @Test
    fun tcPipe012_v150_transcriptInsightsAndVideoTranscriptPreserved() {
        val transcript = "um he don't understand the plan and i think maybe we should pause"
        val input = AttemptInput(
            attemptId = "att-012",
            templateId = "TPL-ANSWER",
            durationMs = 15000L,
            voicedDurationMs = 10000L,
            transcript = transcript,
            pauseCount = 1
        )

        val result = LocalIntelligencePipeline.evaluate(input)
        assertTrue(result is AttemptEvaluationResult.Valid)
        val valid = result as AttemptEvaluationResult.Valid

        assertEquals("1.5.0", valid.evaluationVersion)
        assertEquals(transcript, valid.transcriptText)
        assertTrue(valid.transcriptInsights.isNotEmpty())
        assertTrue(valid.transcriptInsights.any { it.severity == InsightSeverity.ERROR }) // he don't
        assertTrue(valid.transcriptInsights.any { it.severity == InsightSeverity.WARNING }) // um, i think maybe
        assertNotNull(valid.feedback.transcriptInsightSummary)
        assertTrue(valid.feedback.transcriptInsights.isNotEmpty())
    }
}
