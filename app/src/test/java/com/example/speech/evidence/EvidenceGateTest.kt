package com.example.speech.evidence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceGateTest {

    @Test
    fun tcGate001_durationUnderMinimum_returnsTooShort() {
        val result = EvidenceGate.evaluate(
            durationMs = 1400L,
            voicedDurationMs = 800L,
            meanActiveRms = 0.05,
            noiseFloorRms = 0.01,
            wordCount = 5,
            templateId = "TPL-READ"
        )

        assertTrue(result is EvidenceGateResult.Inconclusive)
        assertEquals(InconclusiveReason.TOO_SHORT, (result as EvidenceGateResult.Inconclusive).reason)
    }

    @Test
    fun tcGate002_voicedSpeechUnderMinimum_returnsNoSpeech() {
        val result = EvidenceGate.evaluate(
            durationMs = 3000L,
            voicedDurationMs = 400L, // < 500 ms
            meanActiveRms = 0.05,
            noiseFloorRms = 0.01,
            wordCount = 5,
            templateId = "TPL-READ"
        )

        assertTrue(result is EvidenceGateResult.Inconclusive)
        assertEquals(InconclusiveReason.NO_SPEECH, (result as EvidenceGateResult.Inconclusive).reason)
    }

    @Test
    fun tcGate003_voicedRatioUnder25Percent_returnsNoSpeech() {
        val result = EvidenceGate.evaluate(
            durationMs = 5000L,
            voicedDurationMs = 1000L, // 20% < 25%
            meanActiveRms = 0.05,
            noiseFloorRms = 0.01,
            wordCount = 5,
            templateId = "TPL-READ"
        )

        assertTrue(result is EvidenceGateResult.Inconclusive)
        assertEquals(InconclusiveReason.NO_SPEECH, (result as EvidenceGateResult.Inconclusive).reason)
    }

    @Test
    fun tcGate004_audioLevelCloseToNoiseFloor_returnsTooQuiet() {
        val result = EvidenceGate.evaluate(
            durationMs = 3000L,
            voicedDurationMs = 2000L,
            meanActiveRms = 0.011, // <= 0.01 * 1.15
            noiseFloorRms = 0.01,
            wordCount = 10,
            templateId = "TPL-ANSWER"
        )

        assertTrue(result is EvidenceGateResult.Inconclusive)
        assertEquals(InconclusiveReason.TOO_QUIET, (result as EvidenceGateResult.Inconclusive).reason)
    }

    @Test
    fun tcGate005_emptyTranscript_returnsNoTranscript() {
        val result = EvidenceGate.evaluate(
            durationMs = 3000L,
            voicedDurationMs = 2000L,
            meanActiveRms = 0.05,
            noiseFloorRms = 0.01,
            wordCount = 0,
            templateId = "TPL-READ"
        )

        assertTrue(result is EvidenceGateResult.Inconclusive)
        assertEquals(InconclusiveReason.NO_TRANSCRIPT, (result as EvidenceGateResult.Inconclusive).reason)
    }

    @Test
    fun tcGate006_freeSpeakingTooFewWords_returnsTooFewWords() {
        val result = EvidenceGate.evaluate(
            durationMs = 4000L,
            voicedDurationMs = 2500L,
            meanActiveRms = 0.05,
            noiseFloorRms = 0.01,
            wordCount = 7, // < 8 words required for ANSWER
            templateId = "TPL-ANSWER"
        )

        assertTrue(result is EvidenceGateResult.Inconclusive)
        assertEquals(InconclusiveReason.TOO_FEW_WORDS, (result as EvidenceGateResult.Inconclusive).reason)
    }

    @Test
    fun tcGate007_shortTemplateSingleWord_isValid() {
        val result = EvidenceGate.evaluate(
            durationMs = 2000L,
            voicedDurationMs = 1200L,
            meanActiveRms = 0.05,
            noiseFloorRms = 0.01,
            wordCount = 1,
            templateId = "TPL-WORD-SAY"
        )

        assertTrue(result is EvidenceGateResult.Valid)
        assertFalse((result as EvidenceGateResult.Valid).hasClippingWarning)
    }

    @Test
    fun tcGate008_clippingWarningReported_whenClippingExceedsOnePercent() {
        val result = EvidenceGate.evaluate(
            durationMs = 3000L,
            voicedDurationMs = 2000L,
            meanActiveRms = 0.08,
            noiseFloorRms = 0.01,
            wordCount = 12,
            templateId = "TPL-ANSWER",
            clippingRatio = 0.02
        )

        assertTrue(result is EvidenceGateResult.Valid)
        assertTrue((result as EvidenceGateResult.Valid).hasClippingWarning)
    }

    @Test
    fun tcGate009_audioOnlyMode_doesNotRequireTranscript() {
        val result = EvidenceGate.evaluate(
            durationMs = 3000L,
            voicedDurationMs = 2000L,
            meanActiveRms = 0.05,
            noiseFloorRms = 0.01,
            wordCount = 0,
            templateId = "T3B",
            hasAudio = true,
            hasTranscript = false
        )

        assertTrue(result is EvidenceGateResult.Valid)
    }
}
