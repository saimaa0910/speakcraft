package com.example.speech.session

import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.recognition.RecognitionError
import com.example.speech.tier.SpeechTier
import com.example.speech.video.FaceObservation
import com.example.speech.video.FrameObservation
import com.example.speech.video.VideoEvaluation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.math.PI
import kotlin.math.sin

/** EP-06 session layer: capture, recognition sessions and the attempt runner (ARC-05 §3–§5). */
class SpeechSessionTest {

    // ---------- fakes ----------

    /** Plays [segments] of (ms, amplitude) once, then returns silence (or [errorAfter] code). */
    private class FakeAudioSource(
        segments: List<Pair<Int, Double>>,
        private val loopForever: Boolean = false,
        private val errorAfterSamples: Int = -1
    ) : AudioSource {
        override val sampleRateHz = 16_000
        private val pcm: ShortArray
        private var pos = 0
        var started = false; var stopped = false
        init {
            val out = ArrayList<Short>(); var n = 0
            for ((ms, amp) in segments) repeat(16 * ms) {
                out.add(((amp * sin(2 * PI * 210.0 * n / 16_000) + ((n * 7919) % 101 - 50) / 50.0 * 0.0005) * 32767).toInt().toShort()); n++
            }
            pcm = out.toShortArray()
        }
        override fun start() { started = true }
        override fun read(buffer: ShortArray, offset: Int, length: Int): Int {
            if (errorAfterSamples in 0..pos) return -3
            // Like a real microphone, a finite source has "no data yet" once its audio is used up
            // (returning silence instantly would simulate minutes of recording in milliseconds).
            if (pos >= pcm.size) { if (loopForever) pos = 0 else return 0 }
            val n = minOf(length, pcm.size - pos)
            System.arraycopy(pcm, pos, buffer, offset, n); pos += n
            return n
        }
        override fun stop() { stopped = true }
        override fun release() {}
    }

    private class FakeClock(var now: Long = 1_000L) : () -> Long { override fun invoke() = now }

    private sealed interface Ev
    private data class Rms(val db: Float, val advanceMs: Long = 60) : Ev
    private data class Partial(val text: String) : Ev
    private data class Final(val text: String) : Ev
    private data class Err(val e: RecognitionError) : Ev

    /** Each startSegment call plays the next script entry synchronously; stopSegment plays [onStop]. */
    private class ScriptedEngine(
        private val clock: FakeClock,
        private val script: List<List<Ev>>,
        private val onStop: List<Ev> = listOf(Err(RecognitionError.NoMatch))
    ) : RecognizerEngine {
        var starts = 0; var cancelled = false
        private var cb: RecognizerCallbacks? = null
        override fun startSegment(callbacks: RecognizerCallbacks) {
            cb = callbacks
            val i = starts++
            if (i < script.size) play(script[i])
        }
        override fun stopSegment() = play(onStop)
        override fun cancel() { cancelled = true }
        override fun release() {}
        private fun play(events: List<Ev>) = events.forEach { e ->
            when (e) {
                is Rms -> { clock.now += e.advanceMs; cb!!.onRms(e.db) }
                is Partial -> cb!!.onPartialResult(e.text)
                is Final -> cb!!.onFinalResult(e.text, null)
                is Err -> cb!!.onError(e.e)
            }
        }
    }

    private fun speech(ms: Int) = List(ms / 60) { Rms(8f) }
    private fun silence(ms: Int) = List(ms / 60) { Rms(-2f) }

    // ---------- capture ----------

    @Test
    fun tcSess101_userStop_returnsMeasuredFeatures() {
        val src = FakeAudioSource(listOf(300 to 0.0, 2000 to 0.3))
        val cap = CaptureController(src, maxDurationMs = 60_000)
        cap.start()
        Thread.sleep(150)
        val r = cap.stop()
        assertEquals(StopReason.USER, r.stopReason)
        assertTrue(r.features.voicedDurationMs > 0)
        assertTrue(src.started && src.stopped)
    }

    @Test
    fun tcSess102_maxDuration_stopsAutomatically_withWarningForLongLimits() {
        var warned = -1L
        val cap = CaptureController(FakeAudioSource(listOf(1000 to 0.3), loopForever = true), maxDurationMs = 31_000,
            listener = object : CaptureListener { override fun onLimitWarning(remainingMs: Long) { warned = remainingMs } })
        cap.start()
        val r = cap.awaitStopped(10_000)
        assertNotNull(r)
        assertEquals(StopReason.MAX_DURATION, r!!.stopReason)
        assertEquals(31_000L, r.features.durationMs)
        assertTrue("warning ~10 s before the limit, was $warned", warned in 9_960L..10_000L)
    }

    @Test
    fun tcSess103_shortLimit_hasNoWarning() {
        var warned = false
        val cap = CaptureController(FakeAudioSource(listOf(1000 to 0.3), loopForever = true), maxDurationMs = 5_000,
            listener = object : CaptureListener { override fun onLimitWarning(remainingMs: Long) { warned = true } })
        cap.start()
        assertEquals(StopReason.MAX_DURATION, cap.awaitStopped(10_000)!!.stopReason)
        assertFalse("SRS-FR-SESS-003: no 10 s warning on limits under 30 s", warned)
    }

    @Test
    fun tcSess104_storageFull_stopsWithSinkError_keepingFeaturesSoFar() {
        val failing = object : PcmSink {
            var calls = 0
            override fun onPcm(samples: ShortArray, length: Int) { if (++calls > 50) throw IOException("ENOSPC") }
        }
        val cap = CaptureController(FakeAudioSource(listOf(5000 to 0.3), loopForever = true), listOf(failing), maxDurationMs = 60_000)
        cap.start()
        val r = cap.awaitStopped(10_000)!!
        assertEquals(StopReason.SINK_ERROR, r.stopReason)
        assertTrue(r.error is IOException)
        assertTrue(r.features.durationMs in 900L..1100L)
    }

    @Test
    fun tcSess105_sourceReadError_stopsWithSourceError() {
        val cap = CaptureController(FakeAudioSource(listOf(3000 to 0.3), errorAfterSamples = 16_000), maxDurationMs = 60_000)
        cap.start()
        assertEquals(StopReason.SOURCE_ERROR, cap.awaitStopped(10_000)!!.stopReason)
    }

    @Test
    fun tcSess106_interruption_isReported() {
        val cap = CaptureController(FakeAudioSource(listOf(1000 to 0.3), loopForever = true), maxDurationMs = 600_000)
        cap.start()
        assertEquals(StopReason.INTERRUPTED, cap.interrupt().stopReason)
    }

    // ---------- recognition ----------

    @Test
    fun tcSess201_restartPerSegment_capturesTheWholeAnswer() {
        val clock = FakeClock()
        val engine = ScriptedEngine(clock, listOf(
            speech(1200) + Final("so in my last role"),
            listOf(Err(RecognitionError.NoMatch)),          // silence between segments: restart, not failure
            speech(1500) + Final("my last role i led the team"),
            speech(900) + Final("and we shipped on time")
        ))
        val s = RecognitionSession(engine, RecognitionMode.RESTART_PER_SEGMENT, clock)
        s.start()
        val out = s.stop()
        assertEquals("so in my last role i led the team and we shipped on time", out.transcript)
        assertNull(out.error)
        assertEquals(3, out.segments.size)
        assertTrue(engine.starts >= 4)
        assertTrue(out.rmsSamples.size > 50)
    }

    @Test
    fun tcSess202_fatalError_endsRecognition_andIsReported() {
        val clock = FakeClock()
        val engine = ScriptedEngine(clock, listOf(speech(600) + Final("hello everyone"), listOf(Err(RecognitionError.Network))))
        val s = RecognitionSession(engine, RecognitionMode.RESTART_PER_SEGMENT, clock)
        s.start()
        val out = s.stop()
        assertEquals("hello everyone", out.transcript)
        assertEquals(RecognitionError.Network, out.error)
    }

    @Test
    fun tcSess203_missingFinal_keepsPartial_flagged() {
        val clock = FakeClock()
        val engine = ScriptedEngine(clock, listOf(speech(600) + Partial("thank you all")), onStop = emptyList())
        val s = RecognitionSession(engine, RecognitionMode.RESTART_PER_SEGMENT, clock)
        s.start()
        val out = s.stop(finalTimeoutMs = 50)
        assertEquals("thank you all", out.transcript)
        assertTrue(out.usedPartialFallback)
        assertTrue(engine.cancelled)
    }

    // ---------- runner ----------

    @Test
    fun tcSess301_t3b_audioOnly_endToEnd() {
        val runner = SpeechAttemptRunner(RecordingArbiter())
        val a = runner.start(
            AttemptRequest("a-t3b", "TPL-ANSWER", maxDurationMs = 120_000), SpeechTier.T3B_AUDIO_ONLY,
            SpeechAttemptRunner.AttemptWiring(audioSource = FakeAudioSource(listOf(500 to 0.0, 4000 to 0.3)))
        )
        Thread.sleep(150)
        val out = a.stop()
        val v = out.evaluation as AttemptEvaluationResult.Valid
        assertEquals(listOf(SpeechMetricKey.M_SPK, SpeechMetricKey.M_PAUSE, SpeechMetricKey.M_RATIO), v.headlineMetrics.map { it.key })
        assertFalse(v.allMetrics.containsKey(SpeechMetricKey.M_WPM))
    }

    @Test
    fun tcSess302_t2_recognizerOwnsMic_featuresFromRms() {
        val clock = FakeClock()
        val engine = ScriptedEngine(clock, listOf(
            silence(600) + speech(4000) + Final("in my last role i led a small team that rebuilt onboarding"),
            silence(1500) + speech(4000) + Final("as a result new staff were productive two weeks sooner")
        ))
        val runner = SpeechAttemptRunner(RecordingArbiter(), clock)
        val a = runner.start(AttemptRequest("a-t2", "TPL-ANSWER", 120_000), SpeechTier.T2_ON_DEVICE_MIC,
            SpeechAttemptRunner.AttemptWiring(recognizer = engine))
        val out = a.stop()
        val v = out.evaluation as AttemptEvaluationResult.Valid
        assertTrue(v.allMetrics.containsKey(SpeechMetricKey.M_WPM))
        assertEquals(1.0, v.allMetrics.getValue(SpeechMetricKey.M_PAUSE).rawValue!!, 0.0)
        assertNull("clipping is not observable on T2", out.features.clippingRatio)
    }

    @Test
    fun tcSess303_t1_pipesPcmToRecognizer_andRecognises() {
        val clock = FakeClock()
        var pipedSamples = 0
        val pipe = object : PcmSink { override fun onPcm(samples: ShortArray, length: Int) { pipedSamples += length } }
        val engine = ScriptedEngine(clock, listOf(listOf(Final("good morning everyone thank you for joining"))),
            onStop = emptyList())
        val runner = SpeechAttemptRunner(RecordingArbiter(), clock)
        val a = runner.start(AttemptRequest("a-t1", "TPL-READ", 30_000, referenceText = "Good morning everyone, thank you for joining."),
            SpeechTier.T1_FULL_ON_DEVICE,
            SpeechAttemptRunner.AttemptWiring(audioSource = FakeAudioSource(listOf(300 to 0.0, 3000 to 0.3)), recognizer = engine, recognizerPipe = pipe))
        Thread.sleep(100)
        val out = a.stop()
        val v = out.evaluation as AttemptEvaluationResult.Valid
        assertTrue(pipedSamples > 0)
        assertEquals(true, v.passed)
    }

    @Test
    fun tcSess304_onlyOneRecordingAtATime() {
        val arbiter = RecordingArbiter()
        val runner = SpeechAttemptRunner(arbiter)
        val wiring = { SpeechAttemptRunner.AttemptWiring(audioSource = FakeAudioSource(listOf(2000 to 0.3))) }
        val first = runner.start(AttemptRequest("one", "TPL-ANSWER", 60_000), SpeechTier.T3B_AUDIO_ONLY, wiring())
        var blocked = false
        try { runner.start(AttemptRequest("two", "TPL-ANSWER", 60_000), SpeechTier.T3B_AUDIO_ONLY, wiring()) } catch (e: IllegalStateException) { blocked = true }
        assertTrue(blocked)
        first.cancel()
        val again = runner.start(AttemptRequest("three", "TPL-ANSWER", 60_000), SpeechTier.T3B_AUDIO_ONLY, wiring())
        again.cancel()
        assertNull(arbiter.activeAttemptId)
    }

    @Test
    fun tcSess305_wrongWiring_isRejected_andReleasesTheArbiter() {
        val arbiter = RecordingArbiter()
        var rejected = false
        try {
            SpeechAttemptRunner(arbiter).start(AttemptRequest("x", "TPL-READ", 30_000), SpeechTier.T1_FULL_ON_DEVICE,
                SpeechAttemptRunner.AttemptWiring(audioSource = FakeAudioSource(listOf(1000 to 0.3)), recognizer = ScriptedEngine(FakeClock(), emptyList())))
        } catch (e: IllegalArgumentException) { rejected = true }
        assertTrue("T1 without the recogniser pipe must be rejected", rejected)
        assertNull(arbiter.activeAttemptId)
    }

    @Test
    fun tcSess306_cameraFrames_areAnalysedWithTheAttempt() {
        val frames = FrameCollector()
        repeat(50) { i -> frames.add(FrameObservation(i * 100L, 120f, FaceObservation(0.5f, 0.4f, 0.3f, 0.4f, 0f, 0f, 0f), null)) }
        val runner = SpeechAttemptRunner(RecordingArbiter())
        val a = runner.start(AttemptRequest("a-vid", "TPL-ANSWER", 120_000), SpeechTier.T3B_AUDIO_ONLY,
            SpeechAttemptRunner.AttemptWiring(audioSource = FakeAudioSource(listOf(400 to 0.0, 4000 to 0.3)), frames = frames))
        Thread.sleep(100)
        val v = a.stop().evaluation as AttemptEvaluationResult.Valid
        assertTrue(v.video is VideoEvaluation.Valid)
    }
}
