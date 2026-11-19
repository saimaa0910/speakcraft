package com.example.data.ai.local.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "VideoFrameProcessing"

data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val fileSizeBytes: Long,
    val hasAudio: Boolean,
    val corruptionDetected: Boolean
)

data class ExtractedFrame(
    val timestampMs: Long,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int
) {
    /** Recycles the underlying bitmap if not already recycled. */
    fun recycle() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

class VideoFrameProcessingEngine {

    /**
     * Extracts metadata from the given MP4 video file using MediaMetadataRetriever.
     * Returns null if the file doesn't exist. Catches exceptions and returns corruptionDetected=true on error.
     */
    fun extractMetadata(videoFile: File): VideoMetadata? {
        if (!videoFile.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0

            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            val frameRate = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                ?.toFloatOrNull() ?: 0f

            val hasAudioStr = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val hasAudio = hasAudioStr == "yes"

            VideoMetadata(
                durationMs = durationMs,
                width = width,
                height = height,
                frameRate = frameRate,
                fileSizeBytes = videoFile.length(),
                hasAudio = hasAudio,
                corruptionDetected = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata — possible corruption", e)
            VideoMetadata(
                durationMs = 0L,
                width = 0,
                height = 0,
                frameRate = 0f,
                fileSizeBytes = videoFile.length(),
                hasAudio = false,
                corruptionDetected = true
            )
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Extracts frames from the video at [samplingIntervalMs] intervals, up to [maxFrames].
     * Each bitmap is scaled to at most 640px wide to conserve memory.
     * The caller is responsible for recycling bitmaps via [ExtractedFrame.recycle] after processing.
     *
     * @param onProgress Called with (framesExtractedSoFar, totalFramesToExtract).
     */
    suspend fun extractFrames(
        videoFile: File,
        samplingIntervalMs: Long = 1000L,
        maxFrames: Int = 60,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<ExtractedFrame> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<ExtractedFrame>()
        if (!videoFile.exists()) return@withContext frames

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            if (durationMs <= 0L) return@withContext frames

            // Build timestamp list
            val timestamps = mutableListOf<Long>()
            var t = 0L
            while (t <= durationMs && timestamps.size < maxFrames) {
                timestamps.add(t)
                t += samplingIntervalMs
            }
            val total = timestamps.size

            for ((index, timestampMs) in timestamps.withIndex()) {
                onProgress(index, total)
                val rawBitmap: Bitmap? = try {
                    retriever.getFrameAtTime(
                        timestampMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get frame at ${timestampMs}ms", e)
                    null
                }

                if (rawBitmap != null) {
                    val scaled = scaleBitmapToMaxWidth(rawBitmap, maxWidth = 640)
                    if (scaled !== rawBitmap) {
                        rawBitmap.recycle()
                    }
                    frames.add(
                        ExtractedFrame(
                            timestampMs = timestampMs,
                            bitmap = scaled,
                            width = scaled.width,
                            height = scaled.height
                        )
                    )
                }
            }

            onProgress(total, total)
        } catch (e: Exception) {
            Log.e(TAG, "Frame extraction failed — video may be corrupted", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        frames
    }

    /**
     * Processes frames sequentially, invoking [onFrame] and immediately recycling
     * each bitmap to avoid holding multiple frames in memory simultaneously.
     */
    suspend fun processFrames(
        videoFile: File,
        samplingIntervalMs: Long = 1000L,
        maxFrames: Int = 60,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onFrame: suspend (ExtractedFrame) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!videoFile.exists()) return@withContext

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            if (durationMs <= 0L) return@withContext

            val timestamps = mutableListOf<Long>()
            var t = 0L
            while (t <= durationMs && timestamps.size < maxFrames) {
                timestamps.add(t)
                t += samplingIntervalMs
            }
            val total = timestamps.size

            for ((index, timestampMs) in timestamps.withIndex()) {
                onProgress(index, total)
                val rawBitmap: Bitmap? = try {
                    retriever.getFrameAtTime(
                        timestampMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get frame at ${timestampMs}ms", e)
                    null
                }

                if (rawBitmap != null) {
                    val scaled = scaleBitmapToMaxWidth(rawBitmap, maxWidth = 640)
                    if (scaled !== rawBitmap) {
                        rawBitmap.recycle()
                    }
                    val frame = ExtractedFrame(
                        timestampMs = timestampMs,
                        bitmap = scaled,
                        width = scaled.width,
                        height = scaled.height
                    )
                    try {
                        onFrame(frame)
                    } finally {
                        frame.recycle()
                    }
                }
            }
            onProgress(total, total)
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing failed — video may be corrupted", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun scaleBitmapToMaxWidth(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val scale = maxWidth.toFloat() / src.width
        val newHeight = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, maxWidth, newHeight, true)
    }
}
