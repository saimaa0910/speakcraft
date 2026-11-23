package com.example.speech.pipeline

import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.TargetBands
import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Technique / activity issue overrides (ARC-03 §6.8, consistency item CL-60; evaluationVersion 1.4.0):
 * `REBAND` moves one side of a metric's target band, `SUPPRESS` removes an issue, precedence and
 * validation (V-22) are deterministic, and metric values never change.
 */
class IssueOverridesTest {

    private val vocab = ("our team planned the spring launch carefully because customers asked for faster " +
        "reports clearer screens better search simple exports reliable alerts friendly onboarding useful " +
        "dashboards secure sharing quick setup offline notes calm reminders weekly summaries smart filters " +
        "bright themes gentle sounds shared calendars tidy folders private groups instant replies").split(" ")

    private fun words(n: Int): String = List(n) { vocab[it % vocab.size] }.joinToString(" ")

    /** 15 s of speech, so words × 4 = words per minute and pauses × 4 = pauses per minute. */
    private fun input(transcript: String, pauseCount: Int = 0, overrides: Map<IssueCode, IssueOverride> = emptyMap()) =
        AttemptInput(
            attemptId = "att-override",
            templateId = "TPL-ANSWER",
            durationMs = 20_000L,
            voicedDurationMs = 15_000L,
            transcript = transcript,
            pauseCount = pauseCount,
            activeFrameRmsList = List(50) { 0.1 + (it % 5) * 0.01 },
            noiseFloorRms = 0.005,
            issueOverrides = overrides
        )

    private fun evaluate(input: AttemptInput) = LocalIntelligencePipeline.evaluate(input) as AttemptEvaluationResult.Valid
    private fun codes(r: AttemptEvaluationResult.Valid) = r.issues.map { it.code }

    @Test
    fun tcOvr001_rebandPaceSlow_acceptsADeliberatelySlowPace() {
        val slow = input(words(21)) // 84 wpm: below 100 − 10
        assertTrue(IssueCode.ISSUE_PACE_SLOW in codes(evaluate(slow)))

        val rebanded = evaluate(slow.copy(issueOverrides = mapOf(IssueCode.ISSUE_PACE_SLOW to IssueOverride.Reband(70.0, 140.0))))
        assertFalse(IssueCode.ISSUE_PACE_SLOW in codes(rebanded))

        val wpm = rebanded.allMetrics.getValue(SpeechMetricKey.M_WPM)
        assertEquals("value is unchanged", 84.0, wpm.rawValue!!, 0.001)
        assertEquals("band applied is recorded", 70.0, wpm.targetMin!!, 0.0)
        assertEquals("REBAND on the slow side leaves the fast side", TargetBands.DEFAULT.wpmMax, wpm.targetMax!!, 0.0)
        assertEquals("Comfortable", wpm.band)
    }

    @Test
    fun tcOvr002_rebandPaceFast_movesOnlyTheUpperSide() {
        val fast = input(words(45)) // 180 wpm
        assertTrue(IssueCode.ISSUE_PACE_FAST in codes(evaluate(fast)))
        val rebanded = evaluate(fast.copy(issueOverrides = mapOf(IssueCode.ISSUE_PACE_FAST to IssueOverride.Reband(100.0, 190.0))))
        assertFalse(IssueCode.ISSUE_PACE_FAST in codes(rebanded))
        assertEquals(TargetBands.DEFAULT.wpmMin, rebanded.allMetrics.getValue(SpeechMetricKey.M_WPM).targetMin!!, 0.0)
    }

    @Test
    fun tcOvr003_tighterBand_forASlowDownTechnique_flagsAndQuotesTheAppliedBand() {
        val steady = input(words(36)) // 144 wpm: comfortable by default
        assertFalse(IssueCode.ISSUE_PACE_FAST in codes(evaluate(steady)))
        val r = evaluate(steady.copy(issueOverrides = mapOf(IssueCode.ISSUE_PACE_FAST to IssueOverride.Reband(90.0, 120.0))))
        val fast = r.issues.single { it.code == IssueCode.ISSUE_PACE_FAST }
        assertTrue(fast.suggestion, fast.suggestion.contains("100–120"))
    }

    @Test
    fun tcOvr004_rebandPausesAndFillers() {
        val pausing = input(words(30), pauseCount = 6) // 24 long pauses per minute
        assertTrue(IssueCode.ISSUE_PAUSES_LONG in codes(evaluate(pausing)))
        val r = evaluate(pausing.copy(issueOverrides = mapOf(IssueCode.ISSUE_PAUSES_LONG to IssueOverride.Reband(0.0, 25.0))))
        assertFalse(IssueCode.ISSUE_PAUSES_LONG in codes(r))
        assertEquals(25.0, r.allMetrics.getValue(SpeechMetricKey.M_PAUSE).targetMax!!, 0.0)
        assertEquals(6.0, r.allMetrics.getValue(SpeechMetricKey.M_PAUSE).rawValue!!, 0.0)

        val filler = input("um " + words(28) + " uh") // 2 fillers in 15 s = 8 per minute
        assertTrue(IssueCode.ISSUE_FILLERS_HIGH in codes(evaluate(filler)))
        val f = evaluate(filler.copy(issueOverrides = mapOf(IssueCode.ISSUE_FILLERS_HIGH to IssueOverride.Reband(0.0, 8.0))))
        assertFalse(IssueCode.ISSUE_FILLERS_HIGH in codes(f))
    }

    @Test
    fun tcOvr005_suppressStillWorks_andNoOverrideMeansDefaultBands() {
        val slow = input(words(21))
        val r = evaluate(slow.copy(issueOverrides = mapOf(IssueCode.ISSUE_PACE_SLOW to IssueOverride.Suppress)))
        assertFalse(IssueCode.ISSUE_PACE_SLOW in codes(r))
        assertEquals(TargetBands.DEFAULT, IssueOverrides.targetBands(emptyMap()))
        assertEquals(TargetBands.DEFAULT, IssueOverrides.targetBands(mapOf(IssueCode.ISSUE_PACE_SLOW to IssueOverride.Suppress)))
    }

    @Test
    fun tcOvr006_precedence_activityBeatsTechnique_suppressBeatsReband_widestWins() {
        val resolved = IssueOverrides.resolve(
            activity = listOf(IssueOverrideEntry(IssueCode.ISSUE_PACE_SLOW, IssueOverride.Reband(70.0, 150.0))),
            techniques = listOf(
                listOf(
                    IssueOverrideEntry(IssueCode.ISSUE_PACE_SLOW, IssueOverride.Suppress),
                    IssueOverrideEntry(IssueCode.ISSUE_PAUSES_LONG, IssueOverride.Reband(0.0, 4.0))
                ),
                listOf(
                    IssueOverrideEntry(IssueCode.ISSUE_PAUSES_LONG, IssueOverride.Reband(0.0, 6.0)),
                    IssueOverrideEntry(IssueCode.ISSUE_FILLERS_HIGH, IssueOverride.Reband(0.0, 10.0))
                ),
                listOf(IssueOverrideEntry(IssueCode.ISSUE_FILLERS_HIGH, IssueOverride.Suppress))
            )
        )
        assertEquals(IssueOverride.Reband(70.0, 150.0), resolved[IssueCode.ISSUE_PACE_SLOW])
        assertEquals(IssueOverride.Reband(0.0, 6.0), resolved[IssueCode.ISSUE_PAUSES_LONG])
        assertEquals(IssueOverride.Suppress, resolved[IssueCode.ISSUE_FILLERS_HIGH])
        assertEquals(3, resolved.size)
    }

    @Test
    fun tcOvr007_validationV22() {
        val ok = listOf(
            IssueOverrideEntry(IssueCode.ISSUE_PAUSES_LONG, IssueOverride.Suppress),
            IssueOverrideEntry(IssueCode.ISSUE_PACE_SLOW, IssueOverride.Reband(80.0, 140.0))
        )
        assertTrue(IssueOverrides.validate("pause-power", ok).isEmpty())

        val bad = listOf(
            IssueOverrideEntry(IssueCode.ISSUE_TOO_QUIET, IssueOverride.Reband(0.0, 1.0)), // no numeric band
            IssueOverrideEntry(IssueCode.ISSUE_PACE_FAST, IssueOverride.Reband(100.0, 500.0)), // implausible
            IssueOverrideEntry(IssueCode.ISSUE_LOW_VARIETY, IssueOverride.Suppress),
            IssueOverrideEntry(IssueCode.ISSUE_LOW_VARIETY, IssueOverride.Reband(0.4, 1.0)) // duplicate code
        )
        val problems = IssueOverrides.validate("bad-technique", bad)
        assertEquals(problems.toString(), 3, problems.size)
        assertTrue(problems.any { "ISSUE_TOO_QUIET" in it })
        assertTrue(problems.any { "ISSUE_PACE_FAST" in it })
        assertTrue(problems.any { "duplicate" in it })
    }

    @Test
    fun tcOvr008_conflictingPaceRebands_fallBackToTheDefaultPaceBand() {
        val bands = IssueOverrides.targetBands(
            mapOf(
                IssueCode.ISSUE_PACE_SLOW to IssueOverride.Reband(170.0, 200.0),
                IssueCode.ISSUE_PACE_FAST to IssueOverride.Reband(60.0, 120.0)
            )
        )
        assertEquals(TargetBands.DEFAULT.wpmMin, bands.wpmMin, 0.0)
        assertEquals(TargetBands.DEFAULT.wpmMax, bands.wpmMax, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun tcOvr009_rebandNeedsMinBelowMax() {
        IssueOverride.Reband(140.0, 80.0)
    }
}
