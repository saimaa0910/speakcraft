package com.example.data.learningpaths

import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.FeatureSource
import com.example.speech.pipeline.InsightSeverity
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.rules.IssueCode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Learning-path lesson submissions with a recording get their speaking feedback from the local intelligence
 * engine (same as Coach Practice and the labs); written submissions keep the text analysis.
 */
class LearningPathSpeakingFeedbackTest {

    private val lesson = LearningPathCurriculumRegistry.getLesson("30_day_challenge", 6)!!
    private val recording: File = File.createTempFile("lesson_rec_", ".m4a").apply { writeBytes(ByteArray(64) { 1 }) }

    @After
    fun cleanUp() {
        recording.delete()
    }

    private fun features(durationMs: Long, voicedMs: Long) = AcousticFeatures(
        source = FeatureSource.PCM,
        durationMs = durationMs,
        voicedDurationMs = voicedMs,
        pauses = emptyList(),
        activeFrameRms = List((voicedMs / 20).toInt()) { 0.1 },
        noiseFloorRms = 0.005,
        clippingRatio = 0.0,
        firstVoicedMs = if (voicedMs > 0) 300L else null,
        lastVoicedMs = if (voicedMs > 0) durationMs - 300L else null
    )

    private fun request(text: String) = LessonSubmissionRequest(
        programId = "30_day_challenge",
        dayNumber = 6,
        spokenOrWrittenText = text,
        audioFilePath = recording.absolutePath,
        audioDurationSeconds = 20
    )

    private val spoken =
        "Yesterday my manager asked about the release and he don't think we are ready yet so I explained our " +
            "testing plan step by step and showed the results from last week which made the whole team feel " +
            "calmer about the launch date"

    @Test
    fun recordedSubmissionWithTranscript_usesTheEnginesIssuesHighlightsAndCorrections() {
        val fb = LearningPathEvaluationEngine.evaluateLesson(lesson, request(spoken)) { features(20_000, 16_000) }

        assertTrue(fb.evidenceNote, fb.evidenceNote.contains("speech engine ${LocalIntelligencePipeline.EVALUATION_VERSION}"))
        assertTrue(fb.evidenceNote.contains("audio + your transcript"))
        assertTrue(fb.mistakesFound.any { it.startsWith(IssueCode.ISSUE_GRAMMAR_POSSIBLE.title) && it.contains("he don't") })
        assertTrue(fb.transcriptInsights.any { it.severity == InsightSeverity.ERROR && it.highlightedText == "he don't" })
        assertTrue(fb.correctedExamples.any { it.original == "he don't" && it.improved == "he doesn't" })
        assertTrue(fb.whatWentWell.isNotEmpty())
    }

    @Test
    fun recordedSubmissionWithoutTranscript_isAudioOnly_andTheScoreIsLabelledAsNotASpeakingMeasurement() {
        val fb = LearningPathEvaluationEngine.evaluateLesson(lesson, request("")) { features(20_000, 15_000) }

        assertTrue(fb.evidenceNote, fb.evidenceNote.contains("audio only"))
        assertTrue(fb.transcriptInsights.isEmpty())
        assertTrue(fb.explanation.contains("not a measurement of your speaking"))
        assertTrue(fb.mistakesFound.none { it.contains("filler", ignoreCase = true) })
    }

    @Test
    fun inconclusiveRecording_withoutText_failsWithTheEnginesAdvice() {
        val fb = LearningPathEvaluationEngine.evaluateLesson(lesson, request("")) { features(1_000, 800) }

        assertFalse(fb.passed)
        assertTrue(fb.mistakesFound.first().startsWith("Your recording could not be evaluated"))
        assertTrue(fb.coachTips.first().contains("2–3 seconds"))
        assertTrue(fb.evidenceNote.startsWith("Recording not evaluated"))
    }

    @Test
    fun unreadableRecording_fallsBackToTheTextAndSaysSo() {
        val text = "I adjusted our morning sync yesterday because pausing cleanly before key points gives listeners far better clarity."
        val withUnreadable = LearningPathEvaluationEngine.evaluateLesson(lesson, request(text)) { null }
        val written = LearningPathEvaluationEngine.evaluateLesson(
            lesson, LessonSubmissionRequest(programId = "30_day_challenge", dayNumber = 6, spokenOrWrittenText = text)
        )

        assertTrue(withUnreadable.evidenceNote.contains("could not be read"))
        assertEquals(written.overallScore, withUnreadable.overallScore)
        assertEquals(written.transcriptInsights, withUnreadable.transcriptInsights)
    }
}
