package com.example.data.ai.local.video

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerOptions
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "BodyLanguageAnalysis"
private const val MODEL_ASSET_PATH = "models/pose_landmarker_lite.task"

// MediaPipe Pose landmark indices (BlazePose 33-point model)
private const val NOSE = 0
private const val LEFT_SHOULDER = 11
private const val RIGHT_SHOULDER = 12
private const val LEFT_HIP = 23
private const val RIGHT_HIP = 24

data class PostureAnalysisResult(
    val framesAnalyzed: Int,
    val bodyDetectedFrames: Int,
    val shoulderAlignmentNote: String,
    val upperBodyPresence: String,
    val posturalStabilityNote: String,
    val movementLevel: String,
    val positioningNote: String,
    val coachingSuggestions: List<String>,
    val limitations: List<String>
)

class BodyLanguageAnalysisEngine(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null

    /**
     * Loads pose_landmarker_lite.task from assets/models/ or local storage.
     * If [customModelFile] is supplied and exists, it takes precedence.
     * Returns false if the model file is absent or initialization fails.
     */
    fun initialize(customModelFile: File? = null): Boolean {
        return try {
            val baseOptionsBuilder = BaseOptions.builder()

            val downloadedFile = File(context.filesDir, "ai_models/pose_landmarker_lite.task")
            when {
                customModelFile != null && customModelFile.exists() -> {
                    baseOptionsBuilder.setModelAssetPath(customModelFile.absolutePath)
                }
                hasAsset(MODEL_ASSET_PATH) -> {
                    baseOptionsBuilder.setModelAssetPath(MODEL_ASSET_PATH)
                }
                downloadedFile.exists() -> {
                    baseOptionsBuilder.setModelAssetPath(downloadedFile.absolutePath)
                }
                else -> {
                    Log.w(TAG, "PoseLandmarker model file not found in assets or storage")
                    return false
                }
            }

            val options = PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.i(TAG, "PoseLandmarker initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "PoseLandmarker initialization failed", e)
            false
        }
    }

    /**
     * Analyses posture / body language from a list of [ExtractedFrame]s.
     * Does NOT recycle bitmaps — caller remains the owner.
     */
    fun analyzeFrames(frames: List<ExtractedFrame>): PostureAnalysisResult {
        val landmarker = poseLandmarker
        if (landmarker == null || frames.isEmpty()) {
            return emptyResult(frames.size)
        }

        var bodyDetectedFrames = 0
        var shoulderUnevenFrames = 0
        var shoulderDetectedFrames = 0

        // For movement (nose X positions)
        val noseXPositions = mutableListOf<Float>()

        // Shoulder Y deltas for alignment analysis
        val shoulderYDeltas = mutableListOf<Float>()

        // Upper-body presence: frames where both shoulders visible
        var bothShouldersFrames = 0

        for (frame in frames) {
            val mpImage = try {
                BitmapImageBuilder(frame.bitmap).build()
            } catch (e: Exception) {
                Log.w(TAG, "Could not build MPImage for frame @${frame.timestampMs}ms", e)
                continue
            }

            val result = try {
                landmarker.detect(mpImage)
            } catch (e: Exception) {
                Log.w(TAG, "PoseLandmarker detection failed for frame @${frame.timestampMs}ms", e)
                continue
            }

            val poseLandmarksList = result.landmarks()
            if (poseLandmarksList.isNullOrEmpty()) continue

            bodyDetectedFrames++
            val landmarks = poseLandmarksList[0]

            if (landmarks.size > RIGHT_HIP) {
                val nose = landmarks[NOSE]
                val leftShoulder = landmarks[LEFT_SHOULDER]
                val rightShoulder = landmarks[RIGHT_SHOULDER]

                noseXPositions.add(nose.x())

                val leftVis = try {
                    val vis = leftShoulder.visibility()
                    if (vis != null && vis.isPresent) vis.get() else 1f
                } catch (e: Exception) { 1f }

                val rightVis = try {
                    val vis = rightShoulder.visibility()
                    if (vis != null && vis.isPresent) vis.get() else 1f
                } catch (e: Exception) { 1f }

                if (leftVis > 0.5f && rightVis > 0.5f) {
                    bothShouldersFrames++
                    shoulderDetectedFrames++

                    val yDelta = abs(leftShoulder.y() - rightShoulder.y())
                    shoulderYDeltas.add(yDelta)
                    if (yDelta > 0.05f) shoulderUnevenFrames++
                }
            }
        }

        val framesAnalyzed = frames.size
        val presenceRate = if (framesAnalyzed > 0) bodyDetectedFrames.toFloat() / framesAnalyzed else 0f
        val shoulderRate = if (framesAnalyzed > 0) bothShouldersFrames.toFloat() / framesAnalyzed else 0f

        // Shoulder alignment note
        val avgYDelta = if (shoulderYDeltas.isNotEmpty()) shoulderYDeltas.average().toFloat() else 0f
        val unevenRate = if (shoulderDetectedFrames > 0) shoulderUnevenFrames.toFloat() / shoulderDetectedFrames else 0f
        val shoulderAlignmentNote = when {
            shoulderDetectedFrames == 0 -> "Shoulder alignment could not be assessed — shoulders not detected."
            unevenRate > 0.5f -> "Shoulders appear slightly uneven in many frames (avg difference: ${String.format("%.2f", avgYDelta)} normalized units)."
            else -> "Shoulders appear reasonably level throughout the recording."
        }

        // Upper body presence
        val upperBodyPresence = when {
            shoulderRate >= 0.8f -> "Upper body consistently in frame (${(shoulderRate * 100).toInt()}% of frames)."
            shoulderRate >= 0.5f -> "Upper body partially in frame (${(shoulderRate * 100).toInt()}% of frames) — consider adjusting your camera position."
            else -> "Upper body was rarely fully visible. Ensure shoulders are within the camera frame."
        }

        // Movement level (std dev of nose X)
        val movementLevel: String
        val posturalStabilityNote: String
        if (noseXPositions.size >= 2) {
            val mean = noseXPositions.average().toFloat()
            val variance = noseXPositions.map { (it - mean).pow(2) }.average().toFloat()
            val stdDev = sqrt(variance)
            movementLevel = when {
                stdDev < 0.03f -> "Low"
                stdDev < 0.08f -> "Moderate"
                else -> "High"
            }
            posturalStabilityNote = when (movementLevel) {
                "Low" -> "Your head and upper body remained mostly stable throughout the recording."
                "Moderate" -> "Some movement detected — a moderate amount of natural movement is fine."
                else -> "Notable movement detected. Try to stay centred in the frame for a more composed appearance."
            }
        } else {
            movementLevel = "Unknown"
            posturalStabilityNote = "Insufficient data to assess movement."
        }

        // Positioning note
        val positioningNote = if (bodyDetectedFrames > 0) {
            val avgNoseX = noseXPositions.average().toFloat()
            when {
                avgNoseX < 0.35f -> "You appear positioned towards the left side of the frame — centering may improve visual balance."
                avgNoseX > 0.65f -> "You appear positioned towards the right side of the frame — centering may improve visual balance."
                else -> "You appear well-centred in the frame horizontally."
            }
        } else {
            "Positioning could not be assessed."
        }

        // Coaching suggestions
        val suggestions = mutableListOf<String>()
        if (unevenRate > 0.5f) suggestions.add("Keep your shoulders level and relaxed.")
        if (shoulderRate < 0.6f) suggestions.add("Position the camera so your full upper body is visible.")
        if (movementLevel == "High") suggestions.add("Try to remain centred and reduce lateral movement during your presentation.")
        if (shoulderRate >= 0.8f && unevenRate <= 0.3f && movementLevel in listOf("Low", "Moderate")) {
            suggestions.add("Your upper body is well-positioned in the frame.")
        }

        return PostureAnalysisResult(
            framesAnalyzed = framesAnalyzed,
            bodyDetectedFrames = bodyDetectedFrames,
            shoulderAlignmentNote = shoulderAlignmentNote,
            upperBodyPresence = upperBodyPresence,
            posturalStabilityNote = posturalStabilityNote,
            movementLevel = movementLevel,
            positioningNote = positioningNote,
            coachingSuggestions = suggestions.ifEmpty {
                listOf("Continue maintaining good posture and camera positioning.")
            },
            limitations = listOf(
                "Posture observations are based on detected body landmarks and do not make claims about confidence, personality, or emotional state."
            )
        )
    }

    /** Closes the PoseLandmarker and releases its resources. */
    fun release() {
        try {
            poseLandmarker?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing PoseLandmarker", e)
        } finally {
            poseLandmarker = null
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun hasAsset(path: String): Boolean {
        return try {
            context.assets.open(path).use { }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun emptyResult(framesAnalyzed: Int) = PostureAnalysisResult(
        framesAnalyzed = framesAnalyzed,
        bodyDetectedFrames = 0,
        shoulderAlignmentNote = "Analysis could not be performed.",
        upperBodyPresence = "Unknown",
        posturalStabilityNote = "Analysis could not be performed.",
        movementLevel = "Unknown",
        positioningNote = "Analysis could not be performed.",
        coachingSuggestions = emptyList(),
        limitations = listOf(
            "Posture observations are based on detected body landmarks and do not make claims about confidence, personality, or emotional state."
        )
    )
}
