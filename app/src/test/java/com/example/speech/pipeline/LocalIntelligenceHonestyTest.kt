package com.example.speech.pipeline

import com.example.speech.analysis.StreamingFeatureExtractor
import com.example.speech.evidence.InconclusiveReason
import com.example.speech.feedback.StrengthCode
import com.example.speech.metrics.MetricEngine
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Honesty invariants of the local intelligence engine (PP-1, SRS-BR-002, SRS-FR-EVAL-006,
 * ARC-05 §5–§8.4; TST-01 §9 honesty checklist). Evaluation version 1.1.0.
 */
class LocalIntelligenceHonestyTest {

    private val transcriptCodes = setOf(
        IssueCode.ISSUE_FILLERS_HIGH, IssueCode.ISSUE_PACE_FAST, IssueCode.ISSUE_PACE_SLOW,
        IssueCode.ISSUE_GRAMMAR_POSSIBLE, IssueCode.ISSUE_HEDGING_HIGH, IssueCode.ISSUE_LOW_VARIETY,
        IssueCode.ISSUE_TOO_SHORT, IssueCode.ISSUE_STRUCT_ELEMENT_MISSING, IssueCode.ISSUE_WORD_NOT_RECOGNISED,
        IssueCode.ISSUE_TARGET_WORD_MISSING, IssueCode.ISSUE_WRONG_FORM
    )
    private val transcriptStrengths = setOf(
        StrengthCode.STR_FEW_FILLERS, StrengthCode.STR_GOOD_VARIETY, StrengthCode.STR_PACE_IN_BAND,
        StrengthCode.STR_LENGTH_ON_TARGET, StrengthCode.STR_STRUCT_COMPLETE, StrengthCode.STR_ALL_RECOGNISED,
        StrengthCode.STR_TARGET_WORD_USED
    )

    private val answer = "so i think the main reason we moved the launch was that the testing found two " +
        "serious problems and we wanted to fix them before customers saw them which took about a week"

    private fun base(transcript: String = answer, hasTranscript: Boolean = true) = AttemptInput(
        attemptId = "att-honesty",
        templateId = "TPL-ANSWER",
        durationMs = 20_000L,
        voicedDurationMs = 15_000L,
        transcript = transcript,
        pauseCount = 1,
        activeFrameRmsList = List(50) { 0.1 + (it % 5) * 0.01 },
        noiseFloorRms = 0.005,
        hasTranscript = hasTranscript
    )

    @Test
    fun tcHon001_audioOnly_claimsNothingThatNeedsATranscript() {
        val r = LocalIntelligencePipeline.evaluate(base(transcript = "", hasTranscript = false))
        r as AttemptEvaluationResult.Valid

        MetricEngine.TRANSCRIPT_METRICS.forEach { key ->
            assertFalse("$key must be absent without a transcript", r.allMetrics.containsKey(key))
        }
        assertTrue(r.issues.none { it.code in transcriptCodes })
        assertFalse("strength ${r.feedback.strength.code}", r.feedback.strength.code in transcriptStrengths)
        assertEquals(
            listOf(SpeechMetricKey.M_SPK, SpeechMetricKey.M_PAUSE, SpeechMetricKey.M_RATIO),
            r.headlineMetrics.map { it.key }
        )
    }

    @Test
    fun tcHon002_noLevelEvidence_isNotReplacedByADefault() {
        // Voiced time is known, but no per-frame levels were supplied: the quiet check is skipped,
        // and M-LEVEL is absent rather than invented.
        val r = LocalIntelligencePipeline.evaluate(base().copy(activeFrameRmsList = emptyList()))
        r as AttemptEvaluationResult.Valid
        assertNull(r.allMetrics[SpeechMetricKey.M_LEVEL])
        assertTrue(r.issues.none { it.code == IssueCode.ISSUE_TOO_QUIET })
    }

    @Test
    fun tcHon003_grammarObservations_onlyFromQualifiedRules_phrasedAsPossible_atMostTwo() {
        val r = LocalIntelligencePipeline.evaluate(
            base(transcript = "he don't know why the team have went home and we didn't saw the email so i sent a email again to fix two problems")
        )
        r as AttemptEvaluationResult.Valid
        val grammar = r.issues.filter { it.code == IssueCode.ISSUE_GRAMMAR_POSSIBLE }
        assertTrue(grammar.isNotEmpty())
        assertTrue("SRS-FR-EVAL-006: at most 2 shown", grammar.size <= 2)
        assertTrue(grammar.all { com.example.speech.rules.RuleQualificationRegistry.isQualified(it.ruleId!!) })
        assertTrue(grammar.all { it.suggestion.startsWith("Possible issue") })
        assertEquals(com.example.speech.metrics.MetricMethodology.ESTIMATED, IssueCode.ISSUE_GRAMMAR_POSSIBLE.methodology)
        assertTrue("M-GRAM present once rules are qualified", r.allMetrics.containsKey(SpeechMetricKey.M_GRAM))
    }

    @Test
    fun tcHon004_techniqueOverride_suppressesOnlyTheNamedIssue() {
        val pausing = base().copy(pauseCount = 8) // 8 long pauses in 15 s of speech → ISSUE_PAUSES_LONG
        val plain = LocalIntelligencePipeline.evaluate(pausing) as AttemptEvaluationResult.Valid
        assertTrue(plain.issues.any { it.code == IssueCode.ISSUE_PAUSES_LONG })

        val overridden = LocalIntelligencePipeline.evaluate(
            pausing.copy(issueOverrides = mapOf(IssueCode.ISSUE_PAUSES_LONG to IssueOverride.Suppress))
        ) as AttemptEvaluationResult.Valid
        assertTrue(overridden.issues.none { it.code == IssueCode.ISSUE_PAUSES_LONG })
        assertEquals(
            plain.issues.filter { it.code != IssueCode.ISSUE_PAUSES_LONG }.map { it.code },
            overridden.issues.map { it.code }
        )
    }

    @Test
    fun tcHon005_endToEnd_fromPcm_audioOnly() {
        val features = StreamingFeatureExtractor.analyse(tone(listOf(800 to 0.0, 3000 to 0.3, 1300 to 0.0, 3000 to 0.3, 900 to 0.0)))
        assertEquals(1, features.pauseCount)

        val input = AttemptInput.fromFeatures("att-pcm", "TPL-ANSWER", features, transcript = null)
        assertFalse(input.hasTranscript)
        val r = LocalIntelligencePipeline.evaluate(input) as AttemptEvaluationResult.Valid
        assertEquals("1.6.0", r.evaluationVersion)
        assertEquals(features.voicedDurationMs.toDouble(), r.allMetrics.getValue(SpeechMetricKey.M_SPK).rawValue!!, 0.0)
        assertEquals(1.0, r.allMetrics.getValue(SpeechMetricKey.M_PAUSE).rawValue!!, 0.0)
        assertTrue(r.issues.none { it.code in transcriptCodes })
    }

    @Test
    fun tcHon006_endToEnd_fromPcm_silenceIsInconclusive() {
        val features = StreamingFeatureExtractor.analyse(tone(listOf(4000 to 0.0)))
        val r = LocalIntelligencePipeline.evaluate(AttemptInput.fromFeatures("att-silent", "TPL-ANSWER", features, transcript = null))
        r as AttemptEvaluationResult.Inconclusive
        assertEquals(InconclusiveReason.NO_SPEECH, r.reason)
    }

    @Test
    fun tcHon007_endToEnd_fromPcm_withTranscript() {
        val features = StreamingFeatureExtractor.analyse(tone(listOf(500 to 0.0, 8000 to 0.3, 500 to 0.0)))
        val r = LocalIntelligencePipeline.evaluate(AttemptInput.fromFeatures("att-full", "TPL-ANSWER", features, transcript = answer))
        r as AttemptEvaluationResult.Valid
        assertTrue(r.allMetrics.containsKey(SpeechMetricKey.M_WPM))
        assertTrue(r.allMetrics.containsKey(SpeechMetricKey.M_FILL))
        assertNull(r.score) // free speaking: no composite score (D-06)
    }

    private fun tone(segments: List<Pair<Int, Double>>): ShortArray {
        val rate = 16_000
        val out = ArrayList<Short>()
        var n = 0
        for ((ms, amp) in segments) repeat(rate * ms / 1000) {
            val noise = ((n * 7919) % 101 - 50) / 50.0 * 0.0005
            out.add(((amp * sin(2 * PI * 200.0 * n / rate) + noise) * 32767.0).toInt().toShort())
            n++
        }
        return out.toShortArray()
    }
}
