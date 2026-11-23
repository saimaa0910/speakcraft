package com.example.speech.video

import com.example.speech.rules.DetectedIssue
import com.example.speech.rules.IssueCode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * On-device video analyser (ARC-07 §5–§6; decision D-42). Turns per-frame face and pose observations
 * from MediaPipe into Estimated video metrics and issues.
 *
 * Honesty rules (PP-1, SRS-BR-002):
 * - a metric is computed only from frames where its inputs were detected; with too little evidence
 *   it is omitted, never defaulted;
 * - "Facing the camera" comes from head orientation, not eye tracking, and is named accordingly;
 * - only clearly actionable findings are issues (face out of frame, not facing the camera, tilted
 *   shoulders). Hand movement, head movement and smiling are reported but never called mistakes —
 *   people speak well with very different styles.
 *
 * Thresholds are proposed values (†) to be tuned on consented recordings (SPK-06, ARC-07 §9).
 */
object VideoAnalyzer {

    const val MIN_FRAMES = 15                 // †
    const val MIN_DURATION_MS = 3_000L        // †
    const val MIN_MEDIAN_LUMA = 40f           // 0..255 †
    const val MIN_FACE_SHARE = 0.30           // below this the video is Inconclusive (NO_FACE) †

    const val FACE_IN_FRAME_ISSUE_BELOW = 75.0   // % (target ≥ 80 %, noise band 5) †
    const val FACING_MAX_ANGLE_DEG = 20f         // |yaw| and |pitch| within this count as facing †
    const val FACING_ISSUE_BELOW = 55.0          // % (target ≥ 60 %, noise band 5) †
    const val TILT_ISSUE_ABOVE_DEG = 12.0        // mean |shoulder tilt| (target ≤ 8°, noise band 4°) †
    const val MIN_VISIBILITY = 0.5f
    const val MIN_POSE_FRAMES = 10
    const val MAX_STEP_MS = 250L                 // gesture motion only between close frames
    const val MOVING_SPEED = 0.15                // frame widths per second counts as moving †
    const val SMILE_THRESHOLD = 0.5f

    fun analyse(frames: List<FrameObservation>): VideoEvaluation {
        val sorted = frames.sortedBy { it.timestampMs }
        val n = sorted.size
        val duration = if (n >= 2) sorted.last().timestampMs - sorted.first().timestampMs else 0L
        if (n < MIN_FRAMES || duration < MIN_DURATION_MS) {
            return VideoEvaluation.Inconclusive(VideoInconclusiveReason.TOO_FEW_FRAMES, n)
        }
        val lumas = sorted.mapNotNull { it.luma }
        if (lumas.isNotEmpty() && median(lumas) < MIN_MEDIAN_LUMA) {
            return VideoEvaluation.Inconclusive(VideoInconclusiveReason.TOO_DARK, n)
        }
        val faces = sorted.mapNotNull { it.face }
        val faceShare = faces.size.toDouble() / n
        if (faceShare < MIN_FACE_SHARE) {
            return VideoEvaluation.Inconclusive(VideoInconclusiveReason.NO_FACE, n)
        }

        val metrics = LinkedHashMap<VideoMetricKey, VideoMetricValue>()

        // V-FRAME
        val framePct = faceShare * 100.0
        metrics[VideoMetricKey.V_FRAME] = VideoMetricValue(
            VideoMetricKey.V_FRAME, framePct, pct(framePct),
            band = if (framePct >= 80.0) "In frame" else "Often out of frame",
            isIssue = framePct < FACE_IN_FRAME_ISSUE_BELOW
        )

        // V-CAMERA (head orientation; only when the transformation matrix was produced)
        val angled = faces.filter { it.yawDeg != null && it.pitchDeg != null }
        if (angled.size >= MIN_FRAMES / 2) {
            val facing = angled.count { abs(it.yawDeg!!) <= FACING_MAX_ANGLE_DEG && abs(it.pitchDeg!!) <= FACING_MAX_ANGLE_DEG }
            val v = facing * 100.0 / angled.size
            metrics[VideoMetricKey.V_CAMERA] = VideoMetricValue(
                VideoMetricKey.V_CAMERA, v, pct(v),
                band = if (v >= 60.0) "Mostly facing the camera" else "Often turned away",
                isIssue = v < FACING_ISSUE_BELOW
            )
        }

        // V-GESTURE (only when pose was detected at all)
        val poseFrames = sorted.filter { it.pose != null }
        if (poseFrames.size >= MIN_POSE_FRAMES) {
            var movingMs = 0L
            for (i in 1 until poseFrames.size) {
                val a = poseFrames[i - 1]; val b = poseFrames[i]
                val dt = b.timestampMs - a.timestampMs
                if (dt <= 0 || dt > MAX_STEP_MS) continue
                val speed = maxOf(wristSpeed(a.pose!!.leftWrist, b.pose!!.leftWrist, dt), wristSpeed(a.pose.rightWrist, b.pose.rightWrist, dt))
                if (speed >= MOVING_SPEED) movingMs += dt
            }
            val v = (movingMs * 100.0 / duration).coerceIn(0.0, 100.0)
            val handsSeen = poseFrames.any { visible(it.pose!!.leftWrist) || visible(it.pose.rightWrist) }
            metrics[VideoMetricKey.V_GESTURE] = VideoMetricValue(
                VideoMetricKey.V_GESTURE, v, pct(v),
                band = when { !handsSeen -> "Hands not in view"; v >= 15.0 -> "Active"; v >= 5.0 -> "Some"; else -> "Little" },
                isIssue = false
            )

            // V-POSTURE (both shoulders visible)
            val tilts = poseFrames.mapNotNull { f ->
                val l = f.pose!!.leftShoulder; val r = f.pose.rightShoulder
                if (visible(l) && visible(r)) shoulderTiltDeg(l!!, r!!) else null
            }
            if (tilts.size >= MIN_POSE_FRAMES) {
                val mean = tilts.average()
                metrics[VideoMetricKey.V_POSTURE] = VideoMetricValue(
                    VideoMetricKey.V_POSTURE, mean, String.format(Locale.ROOT, "%.0f°", mean),
                    band = if (mean <= 8.0) "Level" else "Tilted",
                    isIssue = mean > TILT_ISSUE_ABOVE_DEG
                )
            }
        }

        // V-STEADY: head-centre movement relative to face size
        if (faces.size >= MIN_FRAMES / 2) {
            val meanW = faces.map { it.width.toDouble() }.average().takeIf { it > 1e-6 }
            if (meanW != null) {
                val sd = sqrt(variance(faces.map { it.centerX.toDouble() }) + variance(faces.map { it.centerY.toDouble() }))
                val v = sd / meanW
                metrics[VideoMetricKey.V_STEADY] = VideoMetricValue(
                    VideoMetricKey.V_STEADY, v, String.format(Locale.ROOT, "%.2f", v),
                    band = when { v <= 0.25 -> "Steady"; v <= 0.5 -> "Some movement"; else -> "A lot of movement" },
                    isIssue = false
                )
            }
        }

        // V-SMILE: only when blendshapes were produced for most face frames
        val smiles = faces.mapNotNull { it.smile }
        if (smiles.size >= faces.size / 2 && smiles.isNotEmpty()) {
            val v = smiles.count { it >= SMILE_THRESHOLD } * 100.0 / smiles.size
            metrics[VideoMetricKey.V_SMILE] = VideoMetricValue(
                VideoMetricKey.V_SMILE, v, pct(v),
                band = if (v >= 20.0) "Warm" else "Neutral",
                isIssue = false
            )
        }

        return VideoEvaluation.Valid(metrics, n, duration)
    }

    /** Issues from a valid video evaluation (ARC-07 §6). Inconclusive video produces none. */
    fun detectIssues(video: VideoEvaluation?): List<DetectedIssue> {
        if (video !is VideoEvaluation.Valid) return emptyList()
        val out = ArrayList<DetectedIssue>()
        video.metrics[VideoMetricKey.V_FRAME]?.takeIf { it.isIssue }?.let {
            out += DetectedIssue(IssueCode.ISSUE_FACE_OUT_OF_FRAME, "Face in frame ${it.displayValue} of the time",
                "Keep your head and shoulders in view for the whole answer.")
        }
        video.metrics[VideoMetricKey.V_CAMERA]?.takeIf { it.isIssue }?.let {
            out += DetectedIssue(IssueCode.ISSUE_LOOKING_AWAY, "Facing the camera ${it.displayValue} of the time (estimated from head position)",
                "Bring your face back to the lens for each key point.")
        }
        video.metrics[VideoMetricKey.V_POSTURE]?.takeIf { it.isIssue }?.let {
            out += DetectedIssue(IssueCode.ISSUE_POSTURE_TILTED, "Average shoulder tilt ${it.displayValue}",
                "Keep your shoulders level and relaxed.")
        }
        return out
    }

    private fun visible(p: NormPoint?) = p != null && p.visibility >= MIN_VISIBILITY

    private fun wristSpeed(a: NormPoint?, b: NormPoint?, dtMs: Long): Double =
        if (visible(a) && visible(b)) hypot((b!!.x - a!!.x).toDouble(), (b.y - a.y).toDouble()) / (dtMs / 1000.0) else 0.0

    /** Absolute angle of the shoulder line from horizontal, 0..90°. */
    internal fun shoulderTiltDeg(l: NormPoint, r: NormPoint): Double {
        val deg = Math.toDegrees(atan2((r.y - l.y).toDouble(), (r.x - l.x).toDouble()))
        val a = abs(deg) % 180.0
        return if (a > 90.0) 180.0 - a else a
    }

    private fun median(v: List<Float>): Float = v.sorted()[v.size / 2]

    private fun variance(v: List<Double>): Double {
        val m = v.average()
        return v.sumOf { (it - m) * (it - m) } / v.size
    }

    private fun pct(v: Double) = String.format(Locale.ROOT, "%.0f %%", v)
}
