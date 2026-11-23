package com.example.speech.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricEngineTest {

    @Test
    fun tcMetric001_durationAndSpeakingTime_computedAccurately() {
        val metrics = MetricEngine.computeMetrics(
            durationMs = 10000L,
            voicedDurationMs = 8000L,
            transcript = "this is a test sentence with eight words in it",
            pauseCount = 1
        )

        val dur = metrics[SpeechMetricKey.M_DUR]
        assertNotNull(dur)
        assertEquals(10000.0, dur?.rawValue ?: 0.0, 0.01)
        assertEquals("10.0 s", dur?.displayValue)

        val spk = metrics[SpeechMetricKey.M_SPK]
        assertNotNull(spk)
        assertEquals(8000.0, spk?.rawValue ?: 0.0, 0.01)

        val ratio = metrics[SpeechMetricKey.M_RATIO]
        assertNotNull(ratio)
        assertEquals(0.80, ratio?.rawValue ?: 0.0, 0.01)
    }

    @Test
    fun tcMetric002_wpmPace_categorizedCorrectly() {
        // 120 words in 1 minute (60000 ms) = 120 WPM (Comfortable)
        val text120 = (1..120).joinToString(" ") { "word" }
        val mNormal = MetricEngine.computeMetrics(
            durationMs = 65000L,
            voicedDurationMs = 60000L,
            transcript = text120,
            pauseCount = 1
        )
        val wpmNormal = mNormal[SpeechMetricKey.M_WPM]
        assertEquals(120.0, wpmNormal?.rawValue ?: 0.0, 1.0)
        assertEquals("Comfortable", wpmNormal?.band)
        assertFalse(wpmNormal?.isIssue ?: true)

        // 50 words in 1 minute = 50 WPM (< 90 wpm: Slow issue)
        val text50 = (1..50).joinToString(" ") { "word" }
        val mSlow = MetricEngine.computeMetrics(
            durationMs = 65000L,
            voicedDurationMs = 60000L,
            transcript = text50,
            pauseCount = 1
        )
        val wpmSlow = mSlow[SpeechMetricKey.M_WPM]
        assertEquals(50.0, wpmSlow?.rawValue ?: 0.0, 1.0)
        assertEquals("Slow", wpmSlow?.band)
        assertTrue(wpmSlow?.isIssue ?: false)

        // 200 words in 1 minute = 200 WPM (> 170 wpm: Fast issue)
        val text200 = (1..200).joinToString(" ") { "word" }
        val mFast = MetricEngine.computeMetrics(
            durationMs = 65000L,
            voicedDurationMs = 60000L,
            transcript = text200,
            pauseCount = 1
        )
        val wpmFast = mFast[SpeechMetricKey.M_WPM]
        assertEquals(200.0, wpmFast?.rawValue ?: 0.0, 1.0)
        assertEquals("Fast", wpmFast?.band)
        assertTrue(wpmFast?.isIssue ?: false)
    }

    @Test
    fun tcMetric003_referenceMatch_levenshteinAlignment() {
        // 100% exact match
        val hypExact = listOf("the", "quick", "brown", "fox")
        val refExact = listOf("the", "quick", "brown", "fox")
        val match100 = MetricEngine.computeReferenceMatch(hypExact, refExact)
        assertEquals(100.0, match100, 0.01)

        // 1 substitution in 4 words: distance 1, match = 1.0 - 1/4 = 75%
        val hypSub = listOf("the", "slow", "brown", "fox")
        val match75 = MetricEngine.computeReferenceMatch(hypSub, refExact)
        assertEquals(75.0, match75, 0.01)

        // 1 deletion in 4 words: distance 1, match = 75%
        val hypDel = listOf("the", "brown", "fox")
        val matchDel = MetricEngine.computeReferenceMatch(hypDel, refExact)
        assertEquals(75.0, matchDel, 0.01)
    }

    @Test
    fun tcMetric004_fillers_detectedAndRated() {
        // "um", "uh", "you know"
        val transcript = "I was um thinking that uh you know we should start"
        val metrics = MetricEngine.computeMetrics(
            durationMs = 30000L,
            voicedDurationMs = 30000L, // 0.5 minutes
            transcript = transcript,
            pauseCount = 0
        )

        val fill = metrics[SpeechMetricKey.M_FILL]
        assertNotNull(fill)
        // 3 fillers in 0.5 min = 6.0 per min (> 4.0 issue threshold)
        assertEquals(6.0, fill?.rawValue ?: 0.0, 0.1)
        assertTrue(fill?.isIssue ?: false)
    }

    @Test
    fun tcMetric005_mattr_movingAverageTypeTokenRatio() {
        // Under 25 tokens: MATTR is null
        val shortTokens = (1..20).map { "token$it" }
        assertNull(MetricEngine.computeMattr(shortTokens, windowSize = 25))

        // 30 unique tokens: MATTR is 1.0
        val uniqueTokens = (1..30).map { "unique$it" }
        val mattr100 = MetricEngine.computeMattr(uniqueTokens, windowSize = 25)
        assertNotNull(mattr100)
        assertEquals(1.0, mattr100 ?: 0.0, 0.01)

        // 30 repeating identical tokens: MATTR = 1 / 25 = 0.04
        val repTokens = (1..30).map { "same" }
        val mattrLow = MetricEngine.computeMattr(repTokens, windowSize = 25)
        assertNotNull(mattrLow)
        assertEquals(0.04, mattrLow ?: 0.0, 0.01)
    }

    @Test
    fun tcMetric006_starFramework_cuePhraseDetection() {
        val transcript = "When I was in my role as team lead, my task was to deliver the project. " +
                "I decided to reorganize the backlog. As a result, we achieved on-time delivery."

        val metrics = MetricEngine.computeMetrics(
            durationMs = 20000L,
            voicedDurationMs = 18000L,
            transcript = transcript,
            pauseCount = 1,
            framework = "STAR"
        )

        val struct = metrics[SpeechMetricKey.M_STRUCT]
        assertNotNull(struct)
        assertEquals(4.0, struct?.rawValue ?: 0.0, 0.01)
        assertFalse(struct?.isIssue ?: true)
        assertEquals(listOf("Situation", "Task", "Action", "Result"), struct?.detectedElements)
    }

    @Test
    fun tcMetric007_targetWord_exactAndInflections() {
        val metrics = MetricEngine.computeMetrics(
            durationMs = 5000L,
            voicedDurationMs = 4000L,
            transcript = "She presented innovative ideas to the committee",
            pauseCount = 0,
            targetWord = "innovate",
            targetWordInflections = listOf("innovates", "innovating", "innovative", "innovation")
        )

        val target = metrics[SpeechMetricKey.M_TARGET]
        assertNotNull(target)
        assertEquals(1.0, target?.rawValue ?: 0.0, 0.01)
        assertEquals("Pass", target?.band)
        assertFalse(target?.isIssue ?: true)
    }

    @Test
    fun tcMetric008_loudnessCv_qualitySignal() {
        val steadyRms = listOf(0.05, 0.051, 0.049, 0.05, 0.052)
        val mSteady = MetricEngine.computeMetrics(
            durationMs = 5000L,
            voicedDurationMs = 4000L,
            transcript = "testing loudness consistency signal",
            pauseCount = 0,
            activeFrameRmsList = steadyRms
        )
        val levelSteady = mSteady[SpeechMetricKey.M_LEVEL]
        assertNotNull(levelSteady)
        assertFalse(levelSteady?.isIssue ?: true)

        val erraticRms = listOf(0.01, 0.15, 0.005, 0.20, 0.002)
        val mErratic = MetricEngine.computeMetrics(
            durationMs = 5000L,
            voicedDurationMs = 4000L,
            transcript = "testing erratic audio volume",
            pauseCount = 0,
            activeFrameRmsList = erraticRms
        )
        val levelErratic = mErratic[SpeechMetricKey.M_LEVEL]
        assertNotNull(levelErratic)
        assertTrue(levelErratic?.isIssue ?: false)
    }
}
