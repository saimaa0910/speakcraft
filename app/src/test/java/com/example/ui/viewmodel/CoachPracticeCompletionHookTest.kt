package com.example.ui.viewmodel

import com.example.data.learning.PracticeCompletion
import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.FeatureSource
import com.example.speech.content.BuiltInPractice
import com.example.speech.content.PracticeActivity
import com.example.speech.content.PracticeCatalog
import com.example.speech.pipeline.AttemptInput
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.session.AttemptOutcome
import com.example.speech.session.AttemptRequest
import com.example.speech.session.PracticeEngine
import com.example.speech.session.PracticeHandle
import com.example.speech.session.PracticeProgressListener
import com.example.speech.session.PracticeResult
import com.example.speech.session.PracticeStartOptions
import com.example.speech.session.StopReason
import com.example.speech.tier.SpeechTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The single learning-progress hook of Coach Practice: one [PracticeCompletion] per finished attempt. */
@OptIn(ExperimentalCoroutinesApi::class)
class CoachPracticeCompletionHookTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val activity = PracticeActivity(
        id = "act-pause", title = "Pause drill", goal = "g", prompt = "p", templateId = "TPL-ANSWER",
        targetMinWords = 40, targetMaxWords = 120, techniqueIds = listOf("pause-not-filler"), maxSeconds = 30
    )
    private val catalog = PracticeCatalog(emptyList(), BuiltInPractice.ACTIVITIES + activity)

    private val consent = object : OnlineConsent {
        override var granted: Boolean = false
        override fun set(granted: Boolean) { this.granted = granted }
    }

    /** [speechMs] of voiced audio; a very short recording is inconclusive. */
    private inner class FakeEngine(private val speechMs: Long) : PracticeEngine {
        override fun detectTier(allowOnline: Boolean) = SpeechTier.T1_FULL_ON_DEVICE
        override fun start(request: AttemptRequest, options: PracticeStartOptions, listener: PracticeProgressListener): PracticeHandle =
            object : PracticeHandle {
                override val tier = SpeechTier.T1_FULL_ON_DEVICE
                override val cameraActive = false
                override fun stop(): PracticeResult = PracticeResult(outcomeFor(request), tier, replayFile = null, replayHasVideo = false)
                override fun cancel() {}
            }

        private fun outcomeFor(request: AttemptRequest): AttemptOutcome {
            val frames = (speechMs / 400).toInt().coerceAtLeast(1)
            val features = AcousticFeatures(FeatureSource.PCM, speechMs + 1_000, speechMs, emptyList(), List(frames) { 0.1 + (it % 5) * 0.01 }, 0.005, 0.0, 500, speechMs + 500)
            val transcript = if (speechMs >= 10_000) {
                "our team planned the spring launch carefully because customers asked for faster reports clearer screens " +
                    "better search simple exports reliable alerts friendly onboarding useful dashboards secure sharing"
            } else "yes"
            val input = AttemptInput.fromFeatures(
                attemptId = request.attemptId,
                templateId = request.templateId,
                features = features,
                transcript = transcript,
                targetMinWords = request.targetMinWords,
                targetMaxWords = request.targetMaxWords,
                techniqueIds = request.techniqueIds
            )
            return AttemptOutcome(LocalIntelligencePipeline.evaluate(input), features, StopReason.USER, null, false)
        }
    }

    private fun TestScope.newVm(engine: FakeEngine, sink: MutableList<PracticeCompletion>) =
        CoachPracticeViewModel(
            loadCatalog = { catalog },
            engine = engine,
            consent = consent,
            io = dispatcher,
            clock = { 1_764_000_000_000L + testScheduler.currentTime },
            onAttemptCompleted = { sink += it }
        ).also { advanceUntilIdle() }

    @Test
    fun finished_valid_attempt_reports_one_completion_with_engine_values_only() = runTest(dispatcher) {
        val completions = mutableListOf<PracticeCompletion>()
        val vm = newVm(FakeEngine(speechMs = 15_000), completions)
        vm.openActivity(activity)
        vm.startRecording(); advanceTimeBy(500)
        assertTrue("nothing reported while recording", completions.isEmpty())
        vm.stopRecording(); advanceUntilIdle()

        assertTrue(vm.state.value.session is PracticeSessionState.Done)
        val c = completions.single()
        assertTrue(c.valid)
        assertEquals("act-pause", c.activityId)
        assertEquals("Pause drill", c.title)
        assertEquals(listOf("pause-not-filler"), c.techniqueIds)
        assertEquals("TPL-ANSWER", c.templateId)
        assertEquals(LocalIntelligencePipeline.EVALUATION_VERSION, c.evaluationVersion)
        assertNotNull(c.strengthCode)
        assertNull("free speaking has no score", c.score)
        assertNull("free speaking has no pass mark", c.passed)
        assertNull(c.inconclusiveReason)
        assertTrue(c.attemptId.startsWith("att-"))
    }

    @Test
    fun inconclusive_attempt_is_reported_as_inconclusive_without_score() = runTest(dispatcher) {
        val completions = mutableListOf<PracticeCompletion>()
        val vm = newVm(FakeEngine(speechMs = 600), completions)
        vm.openActivity(activity)
        vm.startRecording(); advanceTimeBy(300); vm.stopRecording(); advanceUntilIdle()

        val c = completions.single()
        assertFalse(c.valid)
        assertNotNull(c.inconclusiveReason)
        assertNull(c.score)
        assertNull(c.passed)
        assertTrue(c.issueCodes.isEmpty())
    }

    @Test
    fun cancelled_attempt_reports_nothing() = runTest(dispatcher) {
        val completions = mutableListOf<PracticeCompletion>()
        val vm = newVm(FakeEngine(speechMs = 15_000), completions)
        vm.openActivity(activity)
        vm.startRecording(); advanceTimeBy(300)
        vm.cancelRecording(); advanceUntilIdle()
        assertTrue(completions.isEmpty())
    }
}
