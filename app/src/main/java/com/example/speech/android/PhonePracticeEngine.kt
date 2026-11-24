package com.example.speech.android

import android.content.Context
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.example.core.logging.AppLogger
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.session.AttemptOutcome
import com.example.speech.session.AttemptRequest
import com.example.speech.session.CaptureListener
import com.example.speech.session.CaptureResult
import com.example.speech.session.FrameCollector
import com.example.speech.session.PracticeEngine
import com.example.speech.session.PracticeHandle
import com.example.speech.session.PracticeProgressListener
import com.example.speech.session.PracticeResult
import com.example.speech.session.PracticeStartOptions
import com.example.speech.session.SpeechAttemptRunner
import com.example.speech.session.StopReason
import com.example.speech.tier.SpeechCapabilityService
import com.example.speech.tier.SpeechTier
import com.example.speech.video.android.AudioTranscoder
import com.example.speech.video.android.MediaPipeFrameAnalyzer
import com.example.speech.video.android.ReplayMuxer
import com.example.speech.video.android.VideoCaptureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Front-camera preview and analysis for practice; implemented by [PhonePracticeEngine]. */
interface PracticeCameraHost {
    /** Loads the on-device face and pose models and binds the front camera. Returns false when it fails. */
    suspend fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider): Boolean
    fun unbindCamera()
}

/**
 * Runs practice attempts on the phone with the local intelligence engine (CMP-29; ARC-05 §2–§5, ARC-07):
 * picks the speech tier, wires microphone, recogniser and camera to [SpeechAttemptRunner], and prepares a
 * local replay (WAV → AAC, muxed with the camera's video when it recorded; ADR-015). Everything stays in
 * app-private storage; only the three most recent attempts are kept.
 */
class PhonePracticeEngine(
    context: Context,
    private val runner: SpeechAttemptRunner
) : PracticeEngine, PracticeCameraHost {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("speech_tier", Context.MODE_PRIVATE)
    private val capability = SpeechCapabilityService(appContext)

    private val frames = FrameCollector().apply { accepting = false }
    @Volatile private var camera: VideoCaptureController? = null
    @Volatile private var analyzer: MediaPipeFrameAnalyzer? = null

    /** True once the recogniser on this phone rejected piped audio; T1 is then demoted to T2 (RSK-09). */
    private val pipeDemoted: Boolean get() = prefs.getBoolean(KEY_PIPE_DEMOTED, false)

    override fun detectTier(allowOnline: Boolean): SpeechTier =
        capability.detectTier(userConsentedToOnline = allowOnline, pipeDemoted = pipeDemoted)

    override suspend fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider): Boolean {
        unbindCamera()
        val a = withContext(Dispatchers.IO) {
            runCatching { MediaPipeFrameAnalyzer(appContext, frames) }
                .onFailure { AppLogger.w(TAG, "Face/pose models could not be loaded", it) }
                .getOrNull()
        } ?: return false
        val controller = VideoCaptureController(appContext, lifecycleOwner, a)
        val ok = suspendCancellableCoroutine { cont ->
            controller.bind(
                surfaceProvider,
                onReady = { if (cont.isActive) cont.resume(true) },
                onError = { e -> AppLogger.w(TAG, "Camera could not start", e); if (cont.isActive) cont.resume(false) }
            )
        }
        if (ok) {
            analyzer = a
            camera = controller
        } else {
            controller.unbind()
            a.close()
        }
        return ok
    }

    override fun unbindCamera() {
        frames.accepting = false
        camera?.unbind()
        analyzer?.close()
        camera = null
        analyzer = null
    }

    override fun start(request: AttemptRequest, options: PracticeStartOptions, listener: PracticeProgressListener): PracticeHandle {
        val tier = detectTier(options.allowOnline)
        val dir = attemptDir(request.attemptId)
        val needsPcm = tier == SpeechTier.T1_FULL_ON_DEVICE || tier == SpeechTier.T3B_AUDIO_ONLY
        val wav = if (needsPcm) File(dir, "audio.wav") else null
        val pipe = if (tier == SpeechTier.T1_FULL_ON_DEVICE) RecognizerPipe() else null
        val recognizer = when (tier) {
            SpeechTier.T1_FULL_ON_DEVICE -> PlatformRecognizerEngine(appContext, onDevice = true, audioPipe = pipe!!.readSide)
            SpeechTier.T2_ON_DEVICE_MIC -> PlatformRecognizerEngine(appContext, onDevice = true)
            SpeechTier.T3A_ONLINE_CONSENTED -> PlatformRecognizerEngine(appContext, onDevice = false)
            SpeechTier.T3B_AUDIO_ONLY -> null
        }

        val cam = camera.takeIf { options.withCamera }
        if (cam != null) frames.restart()
        val videoFile = if (cam?.canRecord == true) File(dir, "video.mp4") else null
        val videoFinalized = CountDownLatch(1)
        var videoOk = false
        if (videoFile != null) {
            cam!!.startRecording(videoFile) { ok, _ -> videoOk = ok; videoFinalized.countDown() }
        } else {
            videoFinalized.countDown()
        }

        val wiring = SpeechAttemptRunner.AttemptWiring(
            audioSource = if (needsPcm) AudioRecordSource(appContext) else null,
            recognizer = recognizer,
            recognizerPipe = pipe,
            extraSinks = listOfNotNull(wav?.let { WavFileSink(it) }),
            frames = if (cam != null) frames else null,
            captureListener = object : CaptureListener {
                override fun onElapsed(elapsedMs: Long) = listener.onElapsed(elapsedMs)
                override fun onLimitWarning(remainingMs: Long) = listener.onLimitWarning(remainingMs)
                override fun onStopped(result: CaptureResult) {
                    if (result.stopReason != StopReason.USER && result.stopReason != StopReason.INTERRUPTED) {
                        listener.onAutoStopped(result.stopReason)
                    }
                }
            }
        )

        val active = try {
            runner.start(request, tier, wiring)
        } catch (e: Exception) {
            pipe?.close(); pipe?.closeReadSide()
            frames.accepting = false
            cam?.stopRecording()
            dir.deleteRecursively()
            throw e
        }

        return object : PracticeHandle {
            override val tier: SpeechTier = tier
            override val cameraActive: Boolean = cam != null

            override fun stop(): PracticeResult {
                val outcome = active.stop()
                pipe?.closeReadSide()
                frames.accepting = false
                cam?.stopRecording()
                videoFinalized.await(5, TimeUnit.SECONDS)
                val (replay, hasVideo) = buildReplay(dir, wav, videoFile?.takeIf { videoOk && it.exists() && it.length() > 0 })
                val demote = tier == SpeechTier.T1_FULL_ON_DEVICE && pipeLooksBroken(outcome)
                if (demote) prefs.edit().putBoolean(KEY_PIPE_DEMOTED, true).apply()
                return PracticeResult(outcome, tier, replay, hasVideo, switchedToCompatibilityMode = demote)
            }

            override fun cancel() {
                active.cancel()
                pipe?.closeReadSide()
                frames.accepting = false
                cam?.stopRecording()
                dir.deleteRecursively()
            }
        }
    }

    /** The user spoke (≥ 2 s of voice) but the piped recogniser returned no words: piping does not work here. */
    private fun pipeLooksBroken(outcome: AttemptOutcome): Boolean {
        if (outcome.features.voicedDurationMs < 2_000) return false
        val transcript = (outcome.evaluation as? AttemptEvaluationResult.Valid)?.transcriptText.orEmpty()
        return transcript.isBlank()
    }

    private fun buildReplay(dir: File, wav: File?, video: File?): Pair<File?, Boolean> {
        val audio = wav?.takeIf { it.exists() && it.length() > 44 }?.let { w ->
            val m4a = File(dir, "audio.m4a")
            if (AudioTranscoder.wavToM4a(w, m4a)) { w.delete(); m4a } else w
        }
        if (video != null) {
            if (audio != null && audio.extension == "m4a") {
                val out = File(dir, "replay.mp4")
                if (ReplayMuxer.mux(video, audio, out)) {
                    video.delete(); audio.delete()
                    return out to true
                }
            }
            return video to true // camera replay without sound (e.g. recogniser-owned microphone)
        }
        return audio to false
    }

    private fun attemptDir(attemptId: String): File {
        val root = File(appContext.filesDir, "practice").apply { mkdirs() }
        root.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() }?.drop(KEEP_ATTEMPTS - 1)
            ?.forEach { it.deleteRecursively() }
        return File(root, attemptId.replace(Regex("[^A-Za-z0-9_-]"), "_")).apply { mkdirs() }
    }

    private companion object {
        const val TAG = "PhonePracticeEngine"
        const val KEY_PIPE_DEMOTED = "pipe_demoted"
        const val KEEP_ATTEMPTS = 3
    }
}
