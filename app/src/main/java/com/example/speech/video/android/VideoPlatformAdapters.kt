package com.example.speech.video.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.speech.session.FrameCollector
import com.example.speech.video.FaceObservation
import com.example.speech.video.FrameObservation
import com.example.speech.video.HeadPose
import com.example.speech.video.NormPoint
import com.example.speech.video.PoseObservation
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX analyser that turns camera frames into [FrameObservation]s with MediaPipe Face Landmarker
 * and Pose Landmarker running on the device (ARC-07 §3–§4; D-42). Frames are analysed at most every
 * [minIntervalMs] (≈ 10 fps †); busy frames are dropped, never queued. Nothing leaves the device and
 * no frame or landmark is stored — only the per-frame summary goes to the [FrameCollector].
 */
class MediaPipeFrameAnalyzer(
    context: Context,
    private val collector: FrameCollector,
    private val minIntervalMs: Long = 100
) : ImageAnalysis.Analyzer, Closeable {

    private val face: FaceLandmarker = FaceLandmarker.createFromOptions(
        context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("models/face_landmarker.task").build())
            .setRunningMode(RunningMode.VIDEO)
            .setNumFaces(1)
            .setOutputFaceBlendshapes(true)
            .setOutputFacialTransformationMatrixes(true)
            .build()
    )
    private val pose: PoseLandmarker = PoseLandmarker.createFromOptions(
        context,
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("models/pose_landmarker_lite.task").build())
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(1)
            .build()
    )
    private var firstTimestampNs = -1L
    private var lastAnalysedMs = Long.MIN_VALUE / 2

    override fun analyze(image: ImageProxy) {
        try {
            val ts = image.imageInfo.timestamp
            if (firstTimestampNs < 0) firstTimestampNs = ts
            val ms = (ts - firstTimestampNs) / 1_000_000
            if (ms - lastAnalysedMs < minIntervalMs) return
            lastAnalysedMs = ms
            val luma = meanLuma(image)
            val mpImage = BitmapImageBuilder(image.toBitmap()).build()
            val options = ImageProcessingOptions.builder().setRotationDegrees(image.imageInfo.rotationDegrees).build()
            val faceResult = face.detectForVideo(mpImage, options, ms)
            val poseResult = pose.detectForVideo(mpImage, options, ms)
            collector.add(FrameObservation(ms, luma, toFace(faceResult), toPose(poseResult)))
        } catch (e: Exception) {
            // A failed frame is dropped; the video evidence gate handles too few frames honestly.
        } finally {
            image.close()
        }
    }

    override fun close() {
        runCatching { face.close() }
        runCatching { pose.close() }
    }

    private fun meanLuma(image: ImageProxy): Float? {
        if (image.format != ImageFormat.YUV_420_888) return null
        val plane = image.planes[0]
        val buf = plane.buffer
        val rowStride = plane.rowStride
        var sum = 0L; var count = 0
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                sum += buf.get(y * rowStride + x).toInt() and 0xFF; count++
                x += 16
            }
            y += 16
        }
        return if (count > 0) sum.toFloat() / count else null
    }

    private fun toFace(r: FaceLandmarkerResult): FaceObservation? {
        val lms = r.faceLandmarks().firstOrNull() ?: return null
        val minX = lms.minOf { it.x() }; val maxX = lms.maxOf { it.x() }
        val minY = lms.minOf { it.y() }; val maxY = lms.maxOf { it.y() }
        val angles = r.facialTransformationMatrixes().orElse(null)?.firstOrNull()?.let { HeadPose.fromMatrix(it) }
        val smile = r.faceBlendshapes().orElse(null)?.firstOrNull()?.let { cats ->
            val s = cats.filter { it.categoryName() == "mouthSmileLeft" || it.categoryName() == "mouthSmileRight" }.map { it.score() }
            if (s.isEmpty()) null else s.average().toFloat()
        }
        return FaceObservation(
            centerX = (minX + maxX) / 2f, centerY = (minY + maxY) / 2f,
            width = maxX - minX, height = maxY - minY,
            yawDeg = angles?.yawDeg, pitchDeg = angles?.pitchDeg, rollDeg = angles?.rollDeg,
            smile = smile
        )
    }

    private fun toPose(r: PoseLandmarkerResult): PoseObservation? {
        val lms = r.landmarks().firstOrNull() ?: return null
        fun pt(i: Int): NormPoint? = lms.getOrNull(i)?.let { l: NormalizedLandmark -> NormPoint(l.x(), l.y(), l.visibility().orElse(0f)) }
        // MediaPipe pose indices: 11/12 shoulders, 15/16 wrists.
        return PoseObservation(pt(11), pt(12), pt(15), pt(16))
    }
}

/**
 * CameraX front-camera controller for an attempt (ARC-07 §3): Preview + ImageAnalysis (MediaPipe) +
 * VideoCapture **without audio** to an app-private file, so the microphone stays with the speech
 * pipeline. If the device cannot bind all three use cases, it falls back to Preview + ImageAnalysis
 * (analysis still works; replay is reported unavailable).
 */
class VideoCaptureController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val analyzer: ImageAnalysis.Analyzer
) {
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    /** True when local replay recording is available on this device. */
    val canRecord: Boolean get() = videoCapture != null

    fun bind(surfaceProvider: Preview.SurfaceProvider?, onReady: () -> Unit, onError: (Throwable) -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { pv -> surfaceProvider?.let { pv.setSurfaceProvider(it) } }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { it.setAnalyzer(analysisExecutor, analyzer) }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                    .build()
                val vc = VideoCapture.withOutput(recorder)
                p.unbindAll()
                try {
                    p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis, vc)
                    videoCapture = vc
                } catch (e: Exception) {
                    p.unbindAll()
                    p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                    videoCapture = null
                }
                onReady()
            } catch (e: Exception) {
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Starts the video-only recording; [onFinalized] reports success or the error. */
    @SuppressLint("MissingPermission") // video-only: no RECORD_AUDIO needed; CAMERA is checked before binding
    fun startRecording(file: File, onFinalized: (Boolean, Throwable?) -> Unit) {
        val vc = videoCapture ?: run { onFinalized(false, IllegalStateException("replay recording unavailable")); return }
        recording = vc.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) onFinalized(!event.hasError(), event.cause)
            }
    }

    fun stopRecording() { recording?.stop(); recording = null }

    fun unbind() {
        runCatching { provider?.unbindAll() }
        analysisExecutor.shutdown()
    }
}

/**
 * WAV → AAC/M4A transcoder (CMP-40; ADR-015 step 2, mandatory): keeps recordings small enough for
 * the storage target and provides the audio track for replay. Uses only platform `MediaCodec`.
 */
object AudioTranscoder {
    private const val MIME = MediaFormat.MIMETYPE_AUDIO_AAC
    private const val TIMEOUT_US = 10_000L

    /** Transcodes a 16-bit mono PCM WAV written by `WavWriter`; returns false on any failure (WAV is kept). */
    fun wavToM4a(wav: File, out: File, bitRate: Int = 32_000): Boolean = runCatching {
        RandomAccessFile(wav, "r").use { raf ->
            val header = ByteArray(44).also { raf.readFully(it) }
            val hb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val sampleRate = hb.getInt(24)
            val channels = hb.getShort(22).toInt()
            val format = MediaFormat.createAudioFormat(MIME, sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
            }
            val codec = MediaCodec.createEncoderByType(MIME)
            val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()
                val info = MediaCodec.BufferInfo()
                val chunk = ByteArray(8_192)
                var inputDone = false
                var outputDone = false
                var track = -1
                var samplesQueued = 0L
                val bytesPerSample = 2 * channels
                while (!outputDone) {
                    if (!inputDone) {
                        val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val buf = codec.getInputBuffer(inIndex)!!
                            buf.clear()
                            val n = raf.read(chunk, 0, minOf(chunk.size, buf.remaining()))
                            val ptsUs = samplesQueued * 1_000_000L / sampleRate
                            if (n <= 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                buf.put(chunk, 0, n)
                                codec.queueInputBuffer(inIndex, 0, n, ptsUs, 0)
                                samplesQueued += n / bytesPerSample
                            }
                        }
                    }
                    when (val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer.addTrack(codec.outputFormat); muxer.start() }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (outIndex >= 0) {
                            val outBuf = codec.getOutputBuffer(outIndex)!!
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                            if (info.size > 0 && track >= 0) muxer.writeSampleData(track, outBuf, info)
                            codec.releaseOutputBuffer(outIndex, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                    }
                }
            } finally {
                runCatching { codec.stop() }; codec.release()
                runCatching { muxer.stop() }; runCatching { muxer.release() }
            }
        }
        true
    }.getOrElse { out.delete(); false }
}

/**
 * Muxes the video-only clip with the attempt's AAC audio into one replay file (ARC-07 §3; D-42).
 * Both start at capture start, so sample times are copied unchanged. Returns false on failure.
 */
object ReplayMuxer {
    fun mux(videoOnly: File, audioM4a: File, out: File): Boolean = runCatching {
        val video = MediaExtractor().apply { setDataSource(videoOnly.absolutePath) }
        val audio = MediaExtractor().apply { setDataSource(audioM4a.absolutePath) }
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val vTrack = (0 until video.trackCount).first { video.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("video/") }
            val aTrack = (0 until audio.trackCount).first { audio.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/") }
            video.selectTrack(vTrack); audio.selectTrack(aTrack)
            val outV = muxer.addTrack(video.getTrackFormat(vTrack))
            val outA = muxer.addTrack(audio.getTrackFormat(aTrack))
            muxer.start()
            val buffer = ByteBuffer.allocate(1 shl 20)
            val info = MediaCodec.BufferInfo()
            fun copy(ex: MediaExtractor, track: Int) {
                while (true) {
                    info.size = ex.readSampleData(buffer, 0)
                    if (info.size < 0) break
                    info.offset = 0
                    info.presentationTimeUs = ex.sampleTime
                    info.flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    muxer.writeSampleData(track, buffer, info)
                    ex.advance()
                }
            }
            copy(video, outV)
            copy(audio, outA)
        } finally {
            runCatching { muxer.stop() }; runCatching { muxer.release() }
            video.release(); audio.release()
        }
        true
    }.getOrElse { out.delete(); false }
}
