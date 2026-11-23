package com.example.speech.pipeline

import com.example.speech.feedback.TipSelector
import com.example.speech.rules.IssueCode
import com.example.speech.video.FaceObservation
import com.example.speech.video.FrameObservation
import com.example.speech.video.VideoAnalyzer
import com.example.speech.video.VideoEvaluation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Video evaluation inside the local intelligence pipeline (ARC-07 §6; D-42). */
class VideoPipelineIntegrationTest {

    private val answer = "in my last role i led a small team that rebuilt our onboarding process we interviewed " +
        "new hires mapped every step and removed three manual approvals as a result new staff were productive sooner"

    private fun input(video: VideoEvaluation?) = AttemptInput(
        attemptId = "vid-1", templateId = "TPL-ANSWER", durationMs = 30_000, voicedDurationMs = 22_000,
        transcript = answer, pauseCount = 0, activeFrameRmsList = List(40) { 0.12 }, noiseFloorRms = 0.005,
        video = video
    )

    private fun turnedAwayVideo(): VideoEvaluation = VideoAnalyzer.analyse(
        (0 until 60).map { i -> FrameObservation(i * 100L, 120f, FaceObservation(0.5f, 0.4f, 0.3f, 0.4f, if (i < 40) 40f else 0f, 0f, 0f), null) }
    )

    @Test
    fun tcVidPipe001_videoIssues_joinMistakes_afterAudioIssues_withVideoTip() {
        val r = LocalIntelligencePipeline.evaluate(input(turnedAwayVideo())) as AttemptEvaluationResult.Valid
        assertTrue(r.issues.any { it.code == IssueCode.ISSUE_LOOKING_AWAY })
        val priorities = r.issues.map { it.code.priority }
        assertEquals(priorities.sorted(), priorities)
        assertTrue(r.feedback.tips.any { it.tipId == "TIP-VIDEO-CAMERA-01" })
        assertTrue(r.video is VideoEvaluation.Valid)
    }

    @Test
    fun tcVidPipe002_measuredAspects_leaveTheSelfCheckList() {
        val withVideo = LocalIntelligencePipeline.evaluate(input(turnedAwayVideo())) as AttemptEvaluationResult.Valid
        assertTrue(withVideo.feedback.selfCheckItems.none { it.id == "SC-EYE-01" })
        val audioOnly = LocalIntelligencePipeline.evaluate(input(null)) as AttemptEvaluationResult.Valid
        assertTrue(audioOnly.feedback.selfCheckItems.any { it.id == "SC-EYE-01" })
    }

    @Test
    fun tcVidPipe003_inconclusiveVideo_doesNotBlockAudio_andAddsNoIssues() {
        val dark = VideoAnalyzer.analyse((0 until 40).map { FrameObservation(it * 100L, 10f, null, null) })
        val r = LocalIntelligencePipeline.evaluate(input(dark)) as AttemptEvaluationResult.Valid
        assertTrue(r.issues.none { it.code.priority >= 11 })
        assertTrue(r.video is VideoEvaluation.Inconclusive)
    }

    @Test
    fun tcVidPipe004_tipCatalogue_hasNoPlaceholderSourceLinks() {
        // SRS-BR-028 / ARC-03 V-12: a source link must be a specific YouTube video (11-character id) or absent.
        val valid = Regex("""^https://(www\.)?(youtube\.com/(watch\?v=|shorts/)|youtu\.be/)[A-Za-z0-9_-]{11}([&?]t=\d+s?)?$""")
        TipSelector.CATALOG.forEach { tip ->
            tip.sourceVideoUrl?.let { assertTrue("${tip.tipId} has an invalid source link: $it", valid.matches(it)) }
        }
        // Every issue code has at least one tip (ARC-03 §6 build-time rule).
        IssueCode.values().forEach { code ->
            assertTrue("no tip for $code", TipSelector.CATALOG.any { it.triggerIssueCode == code } || code == IssueCode.ISSUE_WRONG_FORM)
        }
    }
}
