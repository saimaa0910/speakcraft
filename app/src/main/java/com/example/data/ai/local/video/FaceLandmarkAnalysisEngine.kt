package com.example.data.ai.local.video

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.io.File
import kotlin.math.abs

private const val TAG = "FaceLandmarkAnalysis"
private const val MODEL_ASSET_PATH = "models/face_landmarker.task"

data class FaceAnalysisResult(
    val framesAnalyzed: Int = 0,
    val faceDetectedFrames: Int = 0,
    val faceDetectionRate: Float = 0f,
    val averageHeadYaw: Float = 0f,
    val averageHeadPitch: Float = 0f,
    val faceVisibilityNote: String = "",
    val cameraPresenceGuidance: String = "",
    val facingCameraEstimate: String = "",
    val limitations: List<String> = emptyList()
)

class FaceLandmarkAnalysisEngine(private val context: Context) {

    private var faceLandmarker: FaceLandmarker? = null

    /**
     * Loads face_landmarker.task from assets/models/ or local storage.
     * If [customModelFile] is supplied and exists, it takes precedence.
     * Returns false if the model file is absent or initialization fails.
     */
    fun initialize(customModelFile: File? = null): Boolean {
        return try {
            val baseOptionsBuilder = BaseOptions.builder()

            val downloadedFile = File(context.filesDir, "ai_models/face_landmarker.task")
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
                    Log.w(TAG, "FaceLandmarker model file not found in assets or storage")
                    return false
                }
            }

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(1)
                .setOutputFaceBlendshapes(false)
                .setOutputFacialTransformationMatrixes(false)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.i(TAG, "FaceLandmarker initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "FaceLandmarker initialization failed", e)
            false
        }
    }

    /**
     * Analyses a list of [ExtractedFrame]s.
     * Note: does NOT recycle the bitmaps — the caller remains the owner.
     */
    fun analyzeFrames(frames: List<ExtractedFrame>): FaceAnalysisResult {
        val landmarker = faceLandmarker
        if (landmarker == null || frames.isEmpty()) {
            return emptyResult(frames.size)
        }

        var faceDetectedFrames = 0
        var yawSum = 0f
        var pitchSum = 0f
        var yawCount = 0
        var facingCameraFrames = 0

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
                Log.w(TAG, "FaceLandmarker detection failed for frame @${frame.timestampMs}ms", e)
                continue
            }

            val faceLandmarks = result.faceLandmarks()
            if (faceLandmarks.isNullOrEmpty()) continue

            faceDetectedFrames++
            val landmarks = faceLandmarks[0]  // first (and only) face

            // MediaPipe FaceLandmarker canonical indices:
            //   1  = nose tip
            //   234= left ear tragion (speaker's left, camera's right)
            //   454= right ear tragion
            //   10 = forehead top
            //   152= chin
            if (landmarks.size > 454) {
                val noseTip = landmarks[1]
                val leftEar = landmarks[234]
                val rightEar = landmarks[454]
                val forehead = landmarks[10]
                val chin = landmarks[152]

                // Yaw approximation: compare horizontal distance from nose to each ear
                val dxLeft = noseTip.x() - leftEar.x()
                val dxRight = rightEar.x() - noseTip.x()
                val yawRaw = if (dxLeft + dxRight > 0f) {
                    val ratio = (dxRight - dxLeft) / (dxRight + dxLeft)
                    ratio * 45f
                } else 0f

                // Pitch approximation: nose tip vertical position relative to midpoint of forehead-chin
                val midY = (forehead.y() + chin.y()) / 2f
                val pitchRaw = (noseTip.y() - midY) / ((chin.y() - forehead.y()).coerceAtLeast(0.001f)) * 30f

                yawSum += yawRaw
                pitchSum += pitchRaw
                yawCount++

                if (abs(yawRaw) < 15f) {
                    facingCameraFrames++
                }
            }
        }

        val framesAnalyzed = frames.size
        val detectionRate = if (framesAnalyzed > 0) faceDetectedFrames.toFloat() / framesAnalyzed else 0f
        val avgYaw = if (yawCount > 0) yawSum / yawCount else 0f
        val avgPitch = if (yawCount > 0) pitchSum / yawCount else 0f

        val faceVisibilityNote = when {
            detectionRate >= 0.85f -> "Face was consistently visible throughout the recording."
            detectionRate >= 0.5f -> "Face was visible in approximately ${(detectionRate * 100).toInt()}% of frames — consider positioning yourself more centrally."
            detectionRate > 0f -> "Face was detected in only ${(detectionRate * 100).toInt()}% of frames. Ensure your face is well-lit and in frame."
            else -> "No face was detected. Check camera positioning and lighting."
        }

        val cameraPresenceGuidance = when {
            abs(avgYaw) < 15f -> "Looking towards camera"
            abs(avgYaw) < 30f -> "Slightly away"
            else -> "Turned significantly from camera"
        }

        val facingPct = if (framesAnalyzed > 0) {
            (facingCameraFrames.toFloat() / framesAnalyzed * 100).toInt()
        } else 0
        val facingCameraEstimate = "$facingPct% of frames with face roughly facing camera"

        return FaceAnalysisResult(
            framesAnalyzed = framesAnalyzed,
            faceDetectedFrames = faceDetectedFrames,
            faceDetectionRate = detectionRate,
            averageHeadYaw = avgYaw,
            averageHeadPitch = avgPitch,
            faceVisibilityNote = faceVisibilityNote,
            cameraPresenceGuidance = cameraPresenceGuidance,
            facingCameraEstimate = facingCameraEstimate,
            limitations = listOf(
                "Front-camera head-orientation estimate is approximate and not a validated eye-contact measurement."
            )
        )
    }

    /** Closes the FaceLandmarker and releases its resources. */
    fun release() {
        try {
            faceLandmarker?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing FaceLandmarker", e)
        } finally {
            faceLandmarker = null
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

    private fun emptyResult(framesAnalyzed: Int) = FaceAnalysisResult(
        framesAnalyzed = framesAnalyzed,
        faceDetectedFrames = 0,
        faceDetectionRate = 0f,
        averageHeadYaw = 0f,
        averageHeadPitch = 0f,
        faceVisibilityNote = "Analysis could not be performed.",
        cameraPresenceGuidance = "Unknown",
        facingCameraEstimate = "0% of frames with face roughly facing camera",
        limitations = listOf(
            "Front-camera head-orientation estimate is approximate and not a validated eye-contact measurement."
        )
    )
}
