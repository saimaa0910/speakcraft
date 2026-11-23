package com.example.speech.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Golden fixtures (ARC-05 §10, WBS T-07.6, TST-01 §7): fixed inputs whose complete outcome is pinned
 * for one `evaluationVersion`. Any behaviour change makes a fixture fail; the fix is a deliberate
 * version bump with a changelog entry plus re-pinned expectations — never a silent drift.
 */
class GoldenFixturesTest {

    private fun summarize(r: AttemptEvaluationResult): String = when (r) {
        is AttemptEvaluationResult.Inconclusive -> "v=${r.evaluationVersion} INCONCLUSIVE reason=${r.reason}"
        is AttemptEvaluationResult.Valid -> buildString {
            append("v=${r.evaluationVersion} VALID")
            append(" headline=${r.headlineMetrics.map { it.key.code }}")
            append(" issues=${r.issues.map { it.code.name.removePrefix("ISSUE_") }}")
            append(" strength=${r.feedback.strength.code.name}")
            append(" focus=${r.feedback.focus.issueCode?.name?.removePrefix("ISSUE_") ?: "none"}")
            append(" tips=${r.feedback.tips.map { it.tipId }}")
            append(" passed=${r.passed} score=${r.score}")
        }
    }

    private val rms = List(60) { 0.12 + (it % 6) * 0.01 }

    private val fixtures: List<Pair<String, AttemptInput>> = listOf(
        "G1 read pass" to AttemptInput(
            attemptId = "g1", templateId = "TPL-READ", durationMs = 9_000, voicedDurationMs = 7_500,
            transcript = "good morning everyone thank you for joining today's call",
            referenceText = "Good morning everyone, thank you for joining today's call.",
            pauseCount = 0, activeFrameRmsList = rms, noiseFloorRms = 0.005
        ),
        "G2 read fail" to AttemptInput(
            attemptId = "g2", templateId = "TPL-READ", durationMs = 9_000, voicedDurationMs = 7_000,
            transcript = "good morning everyone thank for join call",
            referenceText = "Good morning everyone, thank you for joining today's call.",
            pauseCount = 1, activeFrameRmsList = rms, noiseFloorRms = 0.005
        ),
        "G3 answer fluent" to AttemptInput(
            attemptId = "g3", templateId = "TPL-ANSWER", durationMs = 30_000, voicedDurationMs = 24_000,
            transcript = "in my last role i led a small team that rebuilt our onboarding process we interviewed " +
                "new hires mapped every step and removed three manual approvals as a result new staff were " +
                "productive two weeks sooner and our managers spent less time on paperwork",
            pauseCount = 0, activeFrameRmsList = rms, noiseFloorRms = 0.005, targetMinWords = 30
        ),
        "G4 answer messy" to AttemptInput(
            attemptId = "g4", templateId = "TPL-ANSWER", durationMs = 20_000, voicedDurationMs = 12_000,
            transcript = "um so basically he don't really um like the plan you know and um we didn't went " +
                "to the review um i think maybe it is sort of more better to like wait um you know",
            pauseCount = 5, activeFrameRmsList = rms, noiseFloorRms = 0.005
        ),
        "G5 star missing result" to AttemptInput(
            attemptId = "g5", templateId = "TPL-STRUCT", durationMs = 40_000, voicedDurationMs = 30_000,
            transcript = "the situation was that our biggest client was unhappy with late reports my task was " +
                "to fix the reporting process so i built a shared tracker and set up a weekly check in with " +
                "the analysts and i also trained two new people",
            pauseCount = 1, activeFrameRmsList = rms, noiseFloorRms = 0.005, framework = "STAR"
        ),
        "G6 word say wrong form" to AttemptInput(
            attemptId = "g6", templateId = "TPL-WORD-SAY", durationMs = 3_000, voicedDurationMs = 1_400,
            transcript = "convenient", targetWord = "convey", targetWordInflections = listOf("conveys", "conveyed", "conveying"),
            pauseCount = 0, activeFrameRmsList = rms, noiseFloorRms = 0.005
        ),
        "G7 audio only pauses" to AttemptInput(
            attemptId = "g7", templateId = "TPL-ANSWER", durationMs = 30_000, voicedDurationMs = 18_000,
            transcript = "", hasTranscript = false, pauseCount = 7, activeFrameRmsList = rms, noiseFloorRms = 0.005
        ),
        "G8 too short" to AttemptInput(
            attemptId = "g8", templateId = "TPL-ANSWER", durationMs = 1_000, voicedDurationMs = 600,
            transcript = "hello", pauseCount = 0, activeFrameRmsList = rms, noiseFloorRms = 0.005
        )
    )

    /** Pinned at evaluationVersion 1.3.0 (reviewed 2026-11-23 against ARC-05 §6–§9). */
    private val expected: Map<String, String> = mapOf(
        "G1 read pass" to "v=1.3.0 VALID headline=[M-MATCH, M-WPM, M-PAUSE] issues=[PACE_SLOW] strength=STR_ALL_RECOGNISED focus=PACE_SLOW tips=[TIP-PACE-SLOW-01, TIP-TECH-GENERAL-02, TIP-TECH-GENERAL-01] passed=true score=100",
        "G2 read fail" to "v=1.3.0 VALID headline=[M-MATCH, M-WPM, M-PAUSE] issues=[WORD_NOT_RECOGNISED, PACE_SLOW, PAUSES_LONG] strength=STR_COMPLETED focus=WORD_NOT_RECOGNISED tips=[TIP-WORD-RECOG-01, TIP-PAUSES-01, TIP-PACE-SLOW-01] passed=false score=66",
        "G3 answer fluent" to "v=1.3.0 VALID headline=[M-WPM, M-FILL, M-LEN] issues=[] strength=STR_PACE_IN_BAND focus=none tips=[TIP-TECH-GENERAL-02] passed=null score=null",
        "G4 answer messy" to "v=1.3.0 VALID headline=[M-WPM, M-FILL] issues=[FILLERS_HIGH, PACE_FAST, PAUSES_LONG, GRAMMAR_POSSIBLE, GRAMMAR_POSSIBLE, HEDGING_HIGH] strength=STR_GOOD_VARIETY focus=FILLERS_HIGH tips=[TIP-FILLERS-PAUSE-01, TIP-GRAMMAR-01, TIP-HEDGING-01] passed=null score=null",
        "G5 star missing result" to "v=1.3.0 VALID headline=[M-STRUCT, M-WPM, M-FILL] issues=[STRUCT_ELEMENT_MISSING, PACE_SLOW] strength=STR_FEW_PAUSES focus=STRUCT_ELEMENT_MISSING tips=[TIP-STRUCT-STAR-01, TIP-PACE-SLOW-01, TIP-TECH-GENERAL-02] passed=null score=null",
        "G6 word say wrong form" to "v=1.3.0 VALID headline=[M-TARGET, M-WORDS] issues=[TARGET_WORD_MISSING] strength=STR_COMPLETED focus=TARGET_WORD_MISSING tips=[TIP-TARGET-01, TIP-TECH-GENERAL-02, TIP-TECH-GENERAL-01] passed=false score=0",
        "G7 audio only pauses" to "v=1.3.0 VALID headline=[M-SPK, M-PAUSE, M-RATIO] issues=[PAUSES_LONG] strength=STR_COMPLETED focus=PAUSES_LONG tips=[TIP-PAUSES-01, TIP-TECH-GENERAL-02, TIP-TECH-GENERAL-01] passed=null score=null",
        "G8 too short" to "v=1.3.0 INCONCLUSIVE reason=TOO_SHORT"
    )

    @Test
    fun tcGold001_allFixturesMatchPinnedOutcome() {
        val mismatches = fixtures.mapNotNull { (name, input) ->
            val actual = summarize(LocalIntelligencePipeline.evaluate(input))
            if (actual != expected[name]) "\n\"$name\" to \"$actual\"," else null
        }
        assertEquals("Golden fixtures changed:${mismatches.joinToString("")}", 0, mismatches.size)
    }
}
