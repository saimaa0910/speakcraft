package com.example.ui.viewmodel

import com.example.speech.analysis.AcousticFeatures
import com.example.speech.analysis.FeatureSource
import com.example.speech.content.BuiltInPractice
import com.example.speech.content.PracticeActivity
import com.example.speech.content.PracticeCatalog
import com.example.speech.content.PracticeTechnique
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.pipeline.AttemptInput
import com.example.speech.pipeline.IssueOverride
import com.example.speech.pipeline.IssueOverrideEntry
import com.example.speech.pipeline.LocalIntelligencePipeline
import com.example.speech.rules.IssueCode
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Coach practice screen state with a fake device engine (EP-08). */
@OptIn(ExperimentalCoroutinesApi::class)
class CoachPracticeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val technique = PracticeTechnique(
        id = "pause-not-filler", name = "Pause instead of filler words", summary = "s", whyItWorks = "w",
        steps = listOf("a", "b"), activityIds = listOf("act-pause"), selfChecks = emptyList(),
        overrides = listOf(IssueOverrideEntry(IssueCode.ISSUE_PAUSES_LONG, IssueOverride.Suppress)),
        trackIds = listOf("TRK-WORK"), pendingReview = true
    )
    private val pauseActivity = PracticeActivity(
        id = "act-pause", title = "Pause drill", goal = "g", prompt = "p", templateId = "TPL-ANSWER",
        targetMinWords = 40, targetMaxWords = 120, techniqueIds = listOf(technique.id), maxSeconds = 30
    )
    private val catalog = PracticeCatalog(listOf(technique), BuiltInPractice.ACTIVITIES + pauseActivity)

    private class FakeConsent(override var granted: Boolean = false) : OnlineConsent {
        override fun set(granted: Boolean) { this.granted = granted }
    }

    private inner class FakeEngine : PracticeEngine {
        val requests = mutableListOf<AttemptRequest>()
        var tierDetections = mutableListOf<Boolean>()
        var failWith: Exception? = null
        var cancelled = 0
        var listener: PracticeProgressListener? = null

        override fun detectTier(allowOnline: Boolean): SpeechTier {
            tierDetections += allowOnline
            return if (allowOnline) SpeechTier.T3A_ONLINE_CONSENTED else SpeechTier.T1_FULL_ON_DEVICE
        }

        override fun start(request: AttemptRequest, options: PracticeStartOptions, listener: PracticeProgressListener): PracticeHandle {
            failWith?.let { throw it }
            requests += request
            this.listener = listener
            return object : PracticeHandle {
                override val tier = SpeechTier.T1_FULL_ON_DEVICE
                override val cameraActive = options.withCamera
                override fun stop(): PracticeResult = PracticeResult(outcomeFor(request), tier, replayFile = null, replayHasVideo = false)
                override fun cancel() { cancelled++ }
            }
        }
    }

    private fun outcomeFor(request: AttemptRequest): AttemptOutcome {
        val features = AcousticFeatures(FeatureSource.PCM, 20_000, 15_000, emptyList(), List(50) { 0.1 + (it % 5) * 0.01 }, 0.005, 0.0, 500, 19_500)
        val input = AttemptInput.fromFeatures(
            attemptId = request.attemptId,
            templateId = request.templateId,
            features = features,
            transcript = "our team planned the spring launch carefully because customers asked for faster reports clearer screens " +
                "better search simple exports reliable alerts friendly onboarding useful dashboards secure sharing",
            targetMinWords = request.targetMinWords,
            targetMaxWords = request.targetMaxWords,
            techniqueIds = request.techniqueIds,
            previousMetrics = request.previousMetrics,
            issueOverrides = request.issueOverrides
        )
        return AttemptOutcome(LocalIntelligencePipeline.evaluate(input), features, StopReason.USER, null, false)
    }

    private fun TestScope.newVm(engine: FakeEngine, consent: FakeConsent = FakeConsent()) =
        CoachPracticeViewModel(
            loadCatalog = { catalog },
            engine = engine,
            consent = consent,
            io = dispatcher,
            clock = { testScheduler.currentTime }
        ).also { advanceUntilIdle() }

    @Test
    fun tcVm001_loadsTheCatalogueAndTier() = runTest(dispatcher) {
        val vm = newVm(FakeEngine())
        assertEquals(catalog, vm.state.value.catalog)
        assertEquals(SpeechTier.T1_FULL_ON_DEVICE, vm.state.value.tier)
    }

    @Test
    fun tcVm002_recordStopAndShowFeedback_withTechniqueOverrides() = runTest(dispatcher) {
        val engine = FakeEngine()
        val vm = newVm(engine)
        vm.openActivity(pauseActivity)
        vm.startRecording()
        advanceTimeBy(1_000)
        val rec = vm.state.value.session as PracticeSessionState.Recording
        assertEquals(30_000L, rec.limitMs)
        assertTrue(rec.elapsedMs in 500..1_000)

        vm.stopRecording()
        advanceUntilIdle()
        val done = vm.state.value.session as PracticeSessionState.Done
        assertTrue(done.result.outcome.evaluation is AttemptEvaluationResult.Valid)
        assertEquals(IssueOverride.Suppress, engine.requests.single().issueOverrides[IssueCode.ISSUE_PAUSES_LONG])
        assertEquals(listOf("pause-not-filler"), engine.requests.single().techniqueIds)
        assertEquals(1, vm.state.value.attemptsThisVisit)
    }

    @Test
    fun tcVm003_secondAttempt_comparesWithTheFirst() = runTest(dispatcher) {
        val engine = FakeEngine()
        val vm = newVm(engine)
        vm.openActivity(pauseActivity)
        vm.startRecording(); advanceTimeBy(500); vm.stopRecording(); advanceUntilIdle()
        vm.tryAgain()
        vm.startRecording(); advanceTimeBy(500); vm.stopRecording(); advanceUntilIdle()
        assertNull(engine.requests[0].previousMetrics)
        assertNotNull(engine.requests[1].previousMetrics)
    }

    @Test
    fun tcVm004_timeLimit_stopsAndEvaluatesAutomatically() = runTest(dispatcher) {
        val vm = newVm(FakeEngine())
        vm.openActivity(pauseActivity)
        vm.startRecording()
        advanceTimeBy(21_000)
        assertTrue((vm.state.value.session as PracticeSessionState.Recording).nearLimit)
        advanceTimeBy(10_000)
        advanceUntilIdle()
        assertTrue(vm.state.value.session is PracticeSessionState.Done)
    }

    @Test
    fun tcVm005_missingMicPermission_showsAClearMessage() = runTest(dispatcher) {
        val engine = FakeEngine().apply { failWith = SecurityException("RECORD_AUDIO permission not granted") }
        val vm = newVm(engine)
        vm.openActivity(BuiltInPractice.ACTIVITIES.first())
        vm.startRecording()
        advanceUntilIdle()
        val failed = vm.state.value.session as PracticeSessionState.Failed
        assertTrue(failed.message.contains("microphone permission"))
    }

    @Test
    fun tcVm006_discard_cancelsWithoutEvaluating() = runTest(dispatcher) {
        val engine = FakeEngine()
        val vm = newVm(engine)
        vm.openActivity(pauseActivity)
        vm.startRecording(); advanceTimeBy(500)
        vm.cancelRecording(); advanceUntilIdle()
        assertEquals(PracticeSessionState.Ready, vm.state.value.session)
        assertEquals(1, engine.cancelled)
    }

    @Test
    fun tcVm007_onlineConsent_isStoredAndChangesTheTier() = runTest(dispatcher) {
        val consent = FakeConsent()
        val vm = newVm(FakeEngine(), consent)
        vm.setAllowOnline(true)
        advanceUntilIdle()
        assertTrue(consent.granted)
        assertEquals(SpeechTier.T3A_ONLINE_CONSENTED, vm.state.value.tier)
    }

    @Test
    fun tcVm008_engineAutoStop_isHandled() = runTest(dispatcher) {
        val engine = FakeEngine()
        val vm = newVm(engine)
        vm.openActivity(pauseActivity)
        vm.startRecording(); advanceTimeBy(500)
        engine.listener!!.onAutoStopped(StopReason.SOURCE_ERROR)
        advanceUntilIdle()
        assertTrue(vm.state.value.session is PracticeSessionState.Done)
    }
}
