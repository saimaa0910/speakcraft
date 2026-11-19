package com.example.data.ai.local.video

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerOptions
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "GestureAnalysis"
private const val MODEL_ASSET_PATH = "models/hand_landmarker.task"

// Wrist landmark index in MediaPipe HandLandmarker (21-point hand model)
private const val WRIST = 0

data class GestureAnalysisResult(
    val framesAnalyzed: Int,
    val handsDetectedFrames: Int,
    val gestureFrequency: Float,
    val leftHandPresent: Boolean,
    val rightHandPresent: Boolean,
    val gestureConsistency: String,
    val gestureObservations: List<String>,
    val limitations: List<String>
)

class GestureAnalysisEngine(private val context: Context) {

    private var handLandmarker: HandLandmarker? = null

    /**
     * Loads hand_landmarker.task from assets/models/ or local storage.
     * If [customModelFile] is supplied and exists, it takes precedence.
     * Returns false if the model file is absent or initialization fails.
     */
    fun initialize(customModelFile: File? = null): Boolean {
        return try {
            val baseOptionsBuilder = BaseOptions.builder()

            val downloadedFile = File(context.filesDir, "ai_models/hand_landmarker.task")
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
                    Log.w(TAG, "HandLandmarker model file not found in assets or storage")
                    return false
                }
            }

            val options = HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(2)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.i(TAG, "HandLandmarker initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "HandLandmarker initialization failed", e)
            false
        }
    }

    /**
     * Analyses hand gesture presence and movement from [frames].
     * Does NOT recycle bitmaps — caller remains the owner.
     */
    fun analyzeFrames(frames: List<ExtractedFrame>): GestureAnalysisResult {
        val landmarker = handLandmarker
        if (landmarker == null || frames.isEmpty()) {
            return emptyResult(frames.size)
        }

        var handsDetectedFrames = 0
        var leftHandSeenAtLeastOnce = false
        var rightHandSeenAtLeastOnce = false

        // Collect wrist X positions per hand across frames for variance calculation
        val wristXPositions = mutableListOf<Float>()

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
                Log.w(TAG, "HandLandmarker detection failed for frame @${frame.timestampMs}ms", e)
                continue
            }

            val handLandmarksList = result.landmarks()
            val handednessList = result.handedness()

            if (handLandmarksList.isNullOrEmpty()) continue

            handsDetectedFrames++

            // Check handedness labels
            handednessList?.forEach { categories ->
                val label = categories.firstOrNull()?.categoryName()?.lowercase() ?: ""
                if (label == "left") leftHandSeenAtLeastOnce = true
                if (label == "right") rightHandSeenAtLeastOnce = true
            }

            // Record wrist X of the first detected hand for variance analysis
            handLandmarksList.firstOrNull()?.let { landmarks ->
                if (landmarks.size > WRIST) {
                    wristXPositions.add(landmarks[WRIST].x())
                }
            }
        }

        val framesAnalyzed = frames.size
        val gestureFrequency = if (framesAnalyzed > 0) handsDetectedFrames.toFloat() / framesAnalyzed else 0f
        val handPct = (gestureFrequency * 100).toInt()

        // Gesture consistency via std dev of wrist X
        val gestureConsistency: String = if (wristXPositions.size >= 2) {
            val mean = wristXPositions.average().toFloat()
            val variance = wristXPositions.map { (it - mean).pow(2) }.average().toFloat()
            val stdDev = sqrt(variance)
            when {
                stdDev < 0.05f -> "Consistent — hands remained in a stable area of the frame."
                stdDev < 0.15f -> "Moderate variation — hands moved around the frame with some consistency."
                else -> "High variation — hands moved widely across the frame."
            }
        } else if (handsDetectedFrames == 0) {
            "No hands detected — gesture consistency could not be measured."
        } else {
            "Insufficient data for consistency analysis."
        }

        // Factual observations (no value judgements based on frequency alone)
        val observations = mutableListOf<String>()
        observations.add("Hands were visible in $handPct% of the recording.")

        when {
            gestureFrequency < 0.1f -> observations.add(
                "Hand gestures were limited — consider using them to emphasize key points if it feels natural."
            )
            gestureFrequency < 0.4f -> observations.add(
                "Some hand gestures were present, contributing to visual engagement."
            )
            else -> observations.add(
                "Hand gestures were frequently visible throughout the recording."
            )
        }

        if (leftHandSeenAtLeastOnce && rightHandSeenAtLeastOnce) {
            observations.add("Both left and right hands were detected at various points.")
        } else if (leftHandSeenAtLeastOnce) {
            observations.add("Only the left hand was detected.")
        } else if (rightHandSeenAtLeastOnce) {
            observations.add("Only the right hand was detected.")
        }

        return GestureAnalysisResult(
            framesAnalyzed = framesAnalyzed,
            handsDetectedFrames = handsDetectedFrames,
            gestureFrequency = gestureFrequency,
            leftHandPresent = leftHandSeenAtLeastOnce,
            rightHandPresent = rightHandSeenAtLeastOnce,
            gestureConsistency = gestureConsistency,
            gestureObservations = observations,
            limitations = listOf(
                "Gesture analysis identifies hand presence and approximate movement. It does not interpret gesture meaning or evaluate communication effectiveness from gestures alone."
            )
        )
    }

    /** Closes the HandLandmarker and releases its resources. */
    fun release() {
        try {
            handLandmarker?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing HandLandmarker", e)
        } finally {
            handLandmarker = null
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

    private fun emptyResult(framesAnalyzed: Int) = GestureAnalysisResult(
        framesAnalyzed = framesAnalyzed,
        handsDetectedFrames = 0,
        gestureFrequency = 0f,
        leftHandPresent = false,
        rightHandPresent = false,
        gestureConsistency = "Analysis could not be performed.",
        gestureObservations = listOf("No analysis was performed."),
        limitations = listOf(
            "Gesture analysis identifies hand presence and approximate movement. It does not interpret gesture meaning or evaluate communication effectiveness from gestures alone."
        )
    )
}
