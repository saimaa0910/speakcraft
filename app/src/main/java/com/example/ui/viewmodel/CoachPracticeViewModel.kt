package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speech.content.PracticeActivity
import com.example.speech.content.PracticeCatalog
import com.example.speech.metrics.SpeechMetricKey
import com.example.speech.metrics.SpeechMetricValue
import com.example.speech.pipeline.AttemptEvaluationResult
import com.example.speech.session.AttemptRequest
import com.example.speech.session.PracticeEngine
import com.example.speech.session.PracticeHandle
import com.example.speech.session.PracticeProgressListener
import com.example.speech.session.PracticeResult
import com.example.speech.session.PracticeStartOptions
import com.example.speech.session.StopReason
import com.example.speech.tier.SpeechTier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Device preference for online transcription (SRS-BR-023); see `OnlineTranscriptionConsentStore`. */
interface OnlineConsent {
    val granted: Boolean
    fun set(granted: Boolean)
}

/** Where one attempt is in its life cycle. */
sealed interface PracticeSessionState {
    data object Ready : PracticeSessionState
    data class Recording(
        val elapsedMs: Long,
        val limitMs: Long,
        val nearLimit: Boolean,
        val tier: SpeechTier,
        val cameraActive: Boolean
    ) : PracticeSessionState
    data object Evaluating : PracticeSessionState
    data class Done(val result: PracticeResult) : PracticeSessionState
    data class Failed(val message: String) : PracticeSessionState
}

data class CoachPracticeUiState(
    val catalog: PracticeCatalog? = null,
    val selectedTechniqueId: String? = null,
    val activity: PracticeActivity? = null,
    val session: PracticeSessionState = PracticeSessionState.Ready,
    val tier: SpeechTier? = null,
    val allowOnline: Boolean = false,
    val cameraOn: Boolean = false,
    val cameraReady: Boolean = false,
    /** Self-check answers for the current result (`YES` · `PARTLY` · `NO`; stored as self-reported). */
    val selfCheckAnswers: Map<String, String> = emptyMap(),
    val attemptsThisVisit: Int = 0
)

/**
 * Coach practice: pick an activity, record on the phone, get on-device feedback (local intelligence engine).
 * The device work happens behind [PracticeEngine]; this class holds the screen state.
 */
class CoachPracticeViewModel(
    private val loadCatalog: suspend () -> PracticeCatalog,
    private val engine: PracticeEngine,
    private val consent: OnlineConsent,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _state = MutableStateFlow(CoachPracticeUiState(allowOnline = consent.granted))
    val state: StateFlow<CoachPracticeUiState> = _state.asStateFlow()

    private var handle: PracticeHandle? = null
    private var ticker: Job? = null
    /** Last metrics per activity, for "improved since last time" (STR_IMPROVED). */
    private val previousMetrics = HashMap<String, Map<SpeechMetricKey, SpeechMetricValue>>()

    init {
        viewModelScope.launch {
            val catalog = withContext(io) { loadCatalog() }
            val tier = withContext(io) { runCatching { engine.detectTier(consent.granted) }.getOrNull() }
            _state.update { it.copy(catalog = catalog, tier = tier) }
        }
    }

    fun openTechnique(id: String?) = _state.update { it.copy(selectedTechniqueId = id) }

    fun openActivity(activity: PracticeActivity) = _state.update {
        it.copy(activity = activity, session = PracticeSessionState.Ready, selfCheckAnswers = emptyMap())
    }

    fun closeActivity() {
        cancelRecording()
        _state.update { it.copy(activity = null, session = PracticeSessionState.Ready, selfCheckAnswers = emptyMap()) }
    }

    fun setAllowOnline(allow: Boolean) {
        consent.set(allow)
        _state.update { it.copy(allowOnline = allow) }
        viewModelScope.launch {
            val tier = withContext(io) { runCatching { engine.detectTier(allow) }.getOrNull() }
            _state.update { it.copy(tier = tier) }
        }
    }

    fun setCameraOn(on: Boolean) = _state.update { it.copy(cameraOn = on, cameraReady = if (on) it.cameraReady else false) }

    fun onCameraReady(ready: Boolean) = _state.update { it.copy(cameraReady = ready, cameraOn = it.cameraOn && ready) }

    fun startRecording() {
        val s = _state.value
        val activity = s.activity ?: return
        val catalog = s.catalog ?: return
        if (s.session is PracticeSessionState.Recording || s.session is PracticeSessionState.Evaluating) return

        val limitMs = activity.maxSeconds * 1000L
        val request = AttemptRequest(
            attemptId = "att-" + UUID.randomUUID().toString(),
            templateId = activity.templateId,
            maxDurationMs = limitMs,
            referenceText = activity.referenceText,
            targetMinWords = activity.targetMinWords,
            targetMaxWords = activity.targetMaxWords,
            framework = activity.framework,
            techniqueIds = activity.techniqueIds,
            previousMetrics = previousMetrics[activity.id],
            issueOverrides = catalog.overridesFor(activity)
        )
        val options = PracticeStartOptions(withCamera = s.cameraOn && s.cameraReady, allowOnline = s.allowOnline)

        viewModelScope.launch {
            val started = withContext(io) {
                runCatching { engine.start(request, options, listener) }
            }
            started.onFailure { e ->
                _state.update { it.copy(session = PracticeSessionState.Failed(startErrorMessage(e))) }
            }.onSuccess { h ->
                handle = h
                val startedAt = clock()
                _state.update {
                    it.copy(session = PracticeSessionState.Recording(0, limitMs, false, h.tier, h.cameraActive), tier = h.tier)
                }
                ticker = launch {
                    while (isActive) {
                        val elapsed = clock() - startedAt
                        _state.update { st ->
                            val rec = st.session as? PracticeSessionState.Recording ?: return@update st
                            st.copy(session = rec.copy(elapsedMs = elapsed, nearLimit = rec.nearLimit || limitMs - elapsed <= 10_000))
                        }
                        if (elapsed >= limitMs) { stopRecording(); break }
                        delay(250)
                    }
                }
            }
        }
    }

    fun stopRecording() {
        val h = handle ?: return
        handle = null
        ticker?.cancel()
        ticker = null
        _state.update { it.copy(session = PracticeSessionState.Evaluating) }
        viewModelScope.launch {
            val result = withContext(io) { runCatching { h.stop() } }
            result.onSuccess { r ->
                val activityId = _state.value.activity?.id
                (r.outcome.evaluation as? AttemptEvaluationResult.Valid)?.let { v ->
                    if (activityId != null) previousMetrics[activityId] = v.allMetrics
                }
                _state.update {
                    it.copy(session = PracticeSessionState.Done(r), selfCheckAnswers = emptyMap(), attemptsThisVisit = it.attemptsThisVisit + 1)
                }
            }.onFailure { e ->
                _state.update { it.copy(session = PracticeSessionState.Failed("Something went wrong while analysing: ${e.message ?: "unknown error"}")) }
            }
        }
    }

    fun cancelRecording() {
        val h = handle ?: return
        handle = null
        ticker?.cancel()
        ticker = null
        viewModelScope.launch { withContext(io) { runCatching { h.cancel() } } }
        _state.update { it.copy(session = PracticeSessionState.Ready) }
    }

    fun tryAgain() = _state.update { it.copy(session = PracticeSessionState.Ready, selfCheckAnswers = emptyMap()) }

    fun answerSelfCheck(itemId: String, answer: String) =
        _state.update { it.copy(selfCheckAnswers = it.selfCheckAnswers + (itemId to answer)) }

    private val listener = object : PracticeProgressListener {
        override fun onLimitWarning(remainingMs: Long) = _state.update { st ->
            val rec = st.session as? PracticeSessionState.Recording ?: return@update st
            st.copy(session = rec.copy(nearLimit = true))
        }

        /** Time limit or a device problem ended the recording: evaluate what was captured, honestly. */
        override fun onAutoStopped(reason: StopReason) {
            viewModelScope.launch { stopRecording() }
        }
    }

    private fun startErrorMessage(e: Throwable): String = when {
        e is SecurityException -> "SpeakCraft needs microphone permission to record. Allow it and try again."
        e.message?.contains("another recording", ignoreCase = true) == true -> "Another recording is still running. Please wait a moment."
        e.message?.contains("busy", ignoreCase = true) == true -> "The microphone is being used by another app. Close it and try again."
        else -> "Recording could not start: ${e.message ?: "unknown error"}"
    }

    override fun onCleared() {
        handle?.let { h -> runCatching { h.cancel() } }
        handle = null
        super.onCleared()
    }
}
