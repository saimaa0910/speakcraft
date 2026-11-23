package com.example.speech.video

/** A 2-D point in normalised image coordinates (0..1, origin top-left), with a visibility score. */
data class NormPoint(val x: Float, val y: Float, val visibility: Float = 1f)

/**
 * Face observed in one frame (MediaPipe Face Landmarker, ARC-07 §4). Box is in normalised image
 * coordinates. Head angles come from the facial transformation matrix ([HeadPose]); `null` when the
 * matrix was not produced. [smile] is the mean of the mouth-smile blendshapes (0..1), `null` when
 * blendshapes are disabled.
 */
data class FaceObservation(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val yawDeg: Float?,
    val pitchDeg: Float?,
    val rollDeg: Float?,
    val smile: Float? = null
)

/** Upper-body pose observed in one frame (MediaPipe Pose Landmarker). Points are `null` when not detected. */
data class PoseObservation(
    val leftShoulder: NormPoint?,
    val rightShoulder: NormPoint?,
    val leftWrist: NormPoint?,
    val rightWrist: NormPoint?
)

/**
 * One analysed camera frame. [luma] is the mean of the Y plane (0..255), measured on the frame
 * before landmark detection; [face] / [pose] are `null` when nothing was detected.
 */
data class FrameObservation(
    val timestampMs: Long,
    val luma: Float?,
    val face: FaceObservation?,
    val pose: PoseObservation?
)

/** Video metric catalogue (ARC-07 §5). Every video metric is Estimated: it comes from landmark models. */
enum class VideoMetricKey(val code: String, val displayName: String, val unit: String) {
    V_FRAME("V-FRAME", "Face in frame", "%"),
    V_CAMERA("V-CAMERA", "Facing the camera", "%"),
    V_GESTURE("V-GESTURE", "Hand movement", "%"),
    V_POSTURE("V-POSTURE", "Shoulder tilt", "deg"),
    V_STEADY("V-STEADY", "Head movement", "ratio"),
    V_SMILE("V-SMILE", "Smiling", "%")
}

data class VideoMetricValue(
    val key: VideoMetricKey,
    val rawValue: Double,
    val displayValue: String,
    val band: String,
    val isIssue: Boolean
)

/** Why the video part of an attempt could not be evaluated (the audio part is unaffected). */
enum class VideoInconclusiveReason(val userMessage: String) {
    TOO_FEW_FRAMES("Not enough video was analysed. Keep the camera on for the whole answer."),
    TOO_DARK("The video was too dark to analyse. Face a light source or move to a brighter place."),
    NO_FACE("Your face wasn't visible enough to analyse. Place the phone at eye level with your face in the frame.")
}

/** Outcome of the video analyser for one attempt (ARC-07 §5–§6). */
sealed interface VideoEvaluation {
    data class Valid(
        val metrics: Map<VideoMetricKey, VideoMetricValue>,
        val analysedFrames: Int,
        val analysedDurationMs: Long
    ) : VideoEvaluation

    data class Inconclusive(val reason: VideoInconclusiveReason, val analysedFrames: Int) : VideoEvaluation
}
