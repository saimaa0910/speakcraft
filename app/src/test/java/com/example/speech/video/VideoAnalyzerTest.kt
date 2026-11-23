package com.example.speech.video

import com.example.speech.rules.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** On-device video analyser (ARC-07 §5–§6; D-42). Synthetic landmark sequences at 10 fps. */
class VideoAnalyzerTest {

    private fun face(yaw: Float = 0f, pitch: Float = 0f, cx: Float = 0.5f, cy: Float = 0.4f, smile: Float? = null) =
        FaceObservation(cx, cy, 0.3f, 0.4f, yaw, pitch, 0f, smile)

    private fun pose(tiltDeg: Double = 0.0, wristX: Float = 0.3f) : PoseObservation {
        val half = 0.15
        val dy = (half * kotlin.math.tan(Math.toRadians(tiltDeg))).toFloat()
        return PoseObservation(
            leftShoulder = NormPoint(0.35f, 0.7f - dy), rightShoulder = NormPoint(0.65f, 0.7f + dy),
            leftWrist = NormPoint(wristX, 0.85f), rightWrist = NormPoint(0.7f, 0.9f)
        )
    }

    private fun frames(count: Int, step: Long = 100L, luma: Float = 120f, make: (Int) -> Pair<FaceObservation?, PoseObservation?>) =
        (0 until count).map { i -> val (f, p) = make(i); FrameObservation(i * step, luma, f, p) }

    @Test
    fun tcVid001_frontalSteadySpeaker_hasNoIssues() {
        val v = VideoAnalyzer.analyse(frames(60) { face() to pose() }) as VideoEvaluation.Valid
        assertEquals(100.0, v.metrics.getValue(VideoMetricKey.V_FRAME).rawValue, 1e-9)
        assertEquals(100.0, v.metrics.getValue(VideoMetricKey.V_CAMERA).rawValue, 1e-9)
        assertTrue(VideoAnalyzer.detectIssues(v).isEmpty())
        assertEquals("Steady", v.metrics.getValue(VideoMetricKey.V_STEADY).band)
    }

    @Test
    fun tcVid002_tooFewFramesOrTooShort_isInconclusive() {
        val few = VideoAnalyzer.analyse(frames(10) { face() to null })
        assertEquals(VideoInconclusiveReason.TOO_FEW_FRAMES, (few as VideoEvaluation.Inconclusive).reason)
        val short = VideoAnalyzer.analyse(frames(20, step = 50L) { face() to null }) // 0.95 s
        assertEquals(VideoInconclusiveReason.TOO_FEW_FRAMES, (short as VideoEvaluation.Inconclusive).reason)
    }

    @Test
    fun tcVid003_darkVideo_isInconclusive() {
        val r = VideoAnalyzer.analyse(frames(40, luma = 20f) { face() to null })
        assertEquals(VideoInconclusiveReason.TOO_DARK, (r as VideoEvaluation.Inconclusive).reason)
    }

    @Test
    fun tcVid004_faceRarelyVisible_isInconclusive_notAMistake() {
        val r = VideoAnalyzer.analyse(frames(40) { i -> (if (i % 5 == 0) face() else null) to null })
        assertEquals(VideoInconclusiveReason.NO_FACE, (r as VideoEvaluation.Inconclusive).reason)
        assertTrue(VideoAnalyzer.detectIssues(r).isEmpty())
    }

    @Test
    fun tcVid005_faceOftenOutOfFrame_raisesIssue() {
        val v = VideoAnalyzer.analyse(frames(50) { i -> (if (i % 2 == 0) face() else null) to null }) as VideoEvaluation.Valid
        assertEquals(50.0, v.metrics.getValue(VideoMetricKey.V_FRAME).rawValue, 1e-9)
        assertTrue(VideoAnalyzer.detectIssues(v).any { it.code == IssueCode.ISSUE_FACE_OUT_OF_FRAME })
    }

    @Test
    fun tcVid006_turnedAway_raisesLookingAway_withHonestWording() {
        val v = VideoAnalyzer.analyse(frames(50) { i -> face(yaw = if (i < 35) 35f else 0f) to null }) as VideoEvaluation.Valid
        assertEquals(30.0, v.metrics.getValue(VideoMetricKey.V_CAMERA).rawValue, 1e-9)
        val issue = VideoAnalyzer.detectIssues(v).single { it.code == IssueCode.ISSUE_LOOKING_AWAY }
        assertTrue("must say it is estimated from head position, not eye tracking", issue.evidenceText.contains("head position"))
    }

    @Test
    fun tcVid007_noHeadAngles_omitsFacingMetric() {
        val v = VideoAnalyzer.analyse(frames(40) { FaceObservation(0.5f, 0.4f, 0.3f, 0.4f, null, null, null) to null }) as VideoEvaluation.Valid
        assertFalse(v.metrics.containsKey(VideoMetricKey.V_CAMERA))
    }

    @Test
    fun tcVid008_tiltedShoulders_raiseIssue_levelDoNot() {
        val tilted = VideoAnalyzer.analyse(frames(40) { face() to pose(tiltDeg = 18.0) }) as VideoEvaluation.Valid
        assertEquals(18.0, tilted.metrics.getValue(VideoMetricKey.V_POSTURE).rawValue, 0.5)
        assertTrue(VideoAnalyzer.detectIssues(tilted).any { it.code == IssueCode.ISSUE_POSTURE_TILTED })

        val level = VideoAnalyzer.analyse(frames(40) { face() to pose(tiltDeg = 3.0) }) as VideoEvaluation.Valid
        assertTrue(VideoAnalyzer.detectIssues(level).none { it.code == IssueCode.ISSUE_POSTURE_TILTED })
    }

    @Test
    fun tcVid009_gestures_measured_butNeverAMistake() {
        // Left wrist oscillates 0.1 frame widths every 100 ms → 1.0 widths/s → moving.
        val moving = VideoAnalyzer.analyse(frames(50) { i -> face() to pose(wristX = if (i % 2 == 0) 0.25f else 0.35f) }) as VideoEvaluation.Valid
        assertTrue(moving.metrics.getValue(VideoMetricKey.V_GESTURE).rawValue > 80.0)
        val still = VideoAnalyzer.analyse(frames(50) { face() to pose() }) as VideoEvaluation.Valid
        assertEquals(0.0, still.metrics.getValue(VideoMetricKey.V_GESTURE).rawValue, 1e-9)
        assertTrue("little gesturing is a style, not a mistake", VideoAnalyzer.detectIssues(still).isEmpty())
    }

    @Test
    fun tcVid010_noPoseDetected_omitsGestureAndPosture() {
        val v = VideoAnalyzer.analyse(frames(40) { face() to null }) as VideoEvaluation.Valid
        assertNull(v.metrics[VideoMetricKey.V_GESTURE])
        assertNull(v.metrics[VideoMetricKey.V_POSTURE])
    }

    @Test
    fun tcVid011_smile_onlyWithBlendshapes() {
        val with = VideoAnalyzer.analyse(frames(40) { i -> face(smile = if (i < 20) 0.8f else 0.1f) to null }) as VideoEvaluation.Valid
        assertEquals(50.0, with.metrics.getValue(VideoMetricKey.V_SMILE).rawValue, 1e-9)
        val without = VideoAnalyzer.analyse(frames(40) { face() to null }) as VideoEvaluation.Valid
        assertNull(without.metrics[VideoMetricKey.V_SMILE])
    }

    @Test
    fun tcVid012_headPose_fromTransformationMatrix() {
        assertAngles(HeadPose.fromMatrix(identity()), 0f, 0f, 0f)
        // Pure yaw of 30°: rotation about Y.
        val y = Math.toRadians(30.0)
        val yawM = identity().also { m ->
            m[0] = cos(y).toFloat(); m[8] = sin(y).toFloat()     // r00, r02
            m[2] = (-sin(y)).toFloat(); m[10] = cos(y).toFloat() // r20, r22
        }
        assertAngles(HeadPose.fromMatrix(yawM), 30f, 0f, 0f)
        // Pure pitch of 20°: rotation about X (r11, r12, r21, r22).
        val p = Math.toRadians(20.0)
        val pitchM = identity().also { m ->
            m[5] = cos(p).toFloat(); m[9] = (-sin(p)).toFloat()  // r11, r12
            m[6] = sin(p).toFloat(); m[10] = cos(p).toFloat()    // r21, r22
        }
        assertAngles(HeadPose.fromMatrix(pitchM), 0f, 20f, 0f)
    }

    @Test
    fun tcVid013_shoulderTilt_isMirrorIndependent() {
        val l = NormPoint(0.3f, 0.70f); val r = NormPoint(0.7f, 0.80f)
        assertEquals(VideoAnalyzer.shoulderTiltDeg(l, r), VideoAnalyzer.shoulderTiltDeg(r, l), 1e-9)
    }

    private fun identity() = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

    private fun assertAngles(a: HeadAngles, yaw: Float, pitch: Float, roll: Float) {
        assertEquals("yaw", yaw.toDouble(), a.yawDeg.toDouble(), 0.5)
        assertEquals("pitch", pitch.toDouble(), a.pitchDeg.toDouble(), 0.5)
        assertEquals("roll", roll.toDouble(), a.rollDeg.toDouble(), 0.5)
    }
}
