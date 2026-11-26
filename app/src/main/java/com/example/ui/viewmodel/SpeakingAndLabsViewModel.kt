package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.ai.IntelligenceCapabilityMatrix
import com.example.data.ai.local.audio.LiveTranscriptionManager
import com.example.data.ai.local.audio.LiveTranscriptionState
import com.example.data.evaluation.CoachEvaluationReport
import com.example.data.evaluation.CoachEvaluationState
import com.example.data.evaluation.EvaluationEvidenceValidator
import com.example.data.evaluation.EvidenceValidationInput
import com.example.data.evaluation.LabAttemptEvaluator
import com.example.data.evaluation.LabAttemptRequest
import com.example.data.evaluation.LabPracticeTarget
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.SpeakingSession
import com.example.data.model.UiState
import com.example.data.preferences.AISettings
import com.example.data.preferences.AISettingsRepository
import com.example.speech.analysis.AcousticFeatures
import com.example.speech.android.RecordedAudioAnalyzer
import com.example.speech.feedback.TipDefinition
import com.example.speech.feedback.TipSelector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val MAX_MEDIA_BYTES = 19 * 1024 * 1024L

/**
 * Record-and-review state for the practice labs (Speaking Lab, Challenges, pronunciation, shadowing, grammar
 * speaking, presentation, voice training and lesson practice).
 *
 * Feedback comes from the local intelligence engine (ARC-05) through [LabAttemptEvaluator] — the same engine as
 * Coach Practice: the recording is measured on the device ([recordingAnalyzer]), the screen's transcript (if any)
 * is added, and the lab's template decides what is checked. Transcripts come only from the platform recogniser
 * ([LiveTranscriptionManager]); there is no file transcription.
 */
class SpeakingAndLabsViewModel(
    private val repository: SpeakCraftRepository,
    val audioManager: AudioRecorderManager,
    private val aiSettingsRepo: AISettingsRepository? = null,
    private val liveTranscriptionManager: LiveTranscriptionManager? = null,
    private val capabilityManager: IntelligenceCapabilityManager? = null,
    /** Coach tips: the content pack's when valid, else the built-in list (same as Coach Practice). */
    private val tipCatalog: () -> List<TipDefinition> = { TipSelector.CATALOG },
    /** Measures a finished recording; `null` when its audio cannot be read. */
    private val recordingAnalyzer: (File) -> AcousticFeatures? = RecordedAudioAnalyzer::analyse,
    private val evaluationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    val aiSettings: StateFlow<AISettings> =
        (aiSettingsRepo?.aiSettingsFlow ?: flowOf(AISettings()))
            .stateIn(viewModelScope, SharingStarted.Eagerly, AISettings())

    val capabilityMatrix: StateFlow<IntelligenceCapabilityMatrix> =
        (if (capabilityManager != null && aiSettingsRepo != null) {
            capabilityManager.observeMatrix(aiSettingsRepo)
        } else {
            val mgr = capabilityManager ?: IntelligenceCapabilityManager(audioManager.context)
            flowOf(mgr.computeMatrix(AISettings()))
        }).stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            (capabilityManager ?: IntelligenceCapabilityManager(audioManager.context)).computeMatrix(AISettings())
        )

    val liveTranscriptionState: StateFlow<LiveTranscriptionState> =
        liveTranscriptionManager?.state ?: MutableStateFlow(LiveTranscriptionState()).asStateFlow()

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _evaluationState = MutableStateFlow(CoachEvaluationState.NOT_STARTED)
    val evaluationState: StateFlow<CoachEvaluationState> = _evaluationState.asStateFlow()

    private val _evaluationReport = MutableStateFlow<CoachEvaluationReport?>(null)
    val evaluationReport: StateFlow<CoachEvaluationReport?> = _evaluationReport.asStateFlow()

    private val _analysisState = MutableStateFlow<UiState<SpeakingSession>?>(null)
    val analysisState: StateFlow<UiState<SpeakingSession>?> = _analysisState.asStateFlow()

    private val _activeAnalysisModule = MutableStateFlow<String?>(null)
    val activeAnalysisModule: StateFlow<String?> = _activeAnalysisModule.asStateFlow()

    private val _challengeRemainingSeconds = MutableStateFlow<Int?>(null)
    val challengeRemainingSeconds: StateFlow<Int?> = _challengeRemainingSeconds.asStateFlow()

    private val _uploadedMediaLabel = MutableStateFlow<String?>(null)
    val uploadedMediaLabel: StateFlow<String?> = _uploadedMediaLabel.asStateFlow()

    private val _playbackStatusNote = MutableStateFlow<String?>(null)
    val playbackStatusNote: StateFlow<String?> = _playbackStatusNote.asStateFlow()

    private val _activeMediaPath = MutableStateFlow<String?>(null)
    val activeMediaPath: StateFlow<String?> = _activeMediaPath.asStateFlow()

    private val _activeMediaIsVideo = MutableStateFlow(false)
    val activeMediaIsVideo: StateFlow<Boolean> = _activeMediaIsVideo.asStateFlow()

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    var onSessionCompleted: ((SpeakingSession) -> Unit)? = null

    init {
        viewModelScope.launch {
            aiSettings.collect { settings ->
                liveTranscriptionManager?.refreshFromSettings(settings)
            }
        }
    }

    fun setLiveTranscript(text: String) {
        _liveTranscript.value = text
        liveTranscriptionManager?.updateEditedTranscript(text)
    }

    fun retryLiveTranscription() {
        liveTranscriptionManager?.retryTranscription()
    }

    private var pendingUnsavedSession: SpeakingSession? = null
    private var uploadedMimeType: String = "audio/mp4"
    private var uploadedMediaPath: String = ""
    private var countdownJob: Job? = null

    fun startRecording(challengeSeconds: Int? = null) {
        val newSessionId = UUID.randomUUID().toString()
        _currentSessionId.value = newSessionId
        _liveTranscript.value = ""
        uploadedMediaPath = ""
        _uploadedMediaLabel.value = null
        _activeMediaPath.value = null
        _activeMediaIsVideo.value = false
        _cloudSyncError.value = null
        pendingUnsavedSession = null
        _analysisState.value = null
        _evaluationReport.value = null
        _playbackStatusNote.value = null

        val settings = aiSettings.value
        liveTranscriptionManager?.prepareNewSession(newSessionId, settings)

        val started = audioManager.startRecording(viewModelScope)
        if (!started) {
            val failValidation = EvaluationEvidenceValidator.validate(
                EvidenceValidationInput(
                    sessionId = newSessionId,
                    audioRequired = true,
                    audioBytes = ByteArray(0),
                    videoAssessmentSelected = false
                )
            )
            val failReport = CoachEvaluationReport(
                sessionId = newSessionId,
                evaluationState = CoachEvaluationState.RECORDING_FAILED,
                overallScore = null,
                statusHeadline = "Recording could not start.",
                statusMessage = "Could not start microphone recorder. Check microphone permission and try again.",
                isPartialEvaluation = false,
                partialReason = null,
                dimensions = emptyMap(),
                validation = failValidation.validation
            )
            _evaluationState.value = CoachEvaluationState.RECORDING_FAILED
            _evaluationReport.value = failReport
            _analysisState.value = UiState.Error(failReport.statusMessage)
            return
        }

        _evaluationState.value = CoachEvaluationState.RECORDING
        if (settings.liveTranscriptionEnabled) {
            liveTranscriptionManager?.startTranscription(newSessionId, settings)
        }

        countdownJob?.cancel()
        if (challengeSeconds != null && challengeSeconds > 0) {
            countdownJob = viewModelScope.launch {
                var remaining = challengeSeconds
                _challengeRemainingSeconds.value = remaining
                while (isActive && remaining > 0) {
                    delay(1000L)
                    if (!audioManager.isPaused.value) {
                        remaining--
                        _challengeRemainingSeconds.value = remaining
                        if (remaining == 0) {
                            stopRecording()
                            break
                        }
                    }
                }
            }
        } else {
            _challengeRemainingSeconds.value = null
        }
    }

    fun pauseRecording() {
        audioManager.pauseRecording()
    }

    fun resumeRecording() {
        audioManager.resumeRecording()
    }

    fun stopRecording() {
        countdownJob?.cancel()
        countdownJob = null
        _challengeRemainingSeconds.value = null
        liveTranscriptionManager?.stopTranscription()
        val liveText = liveTranscriptionManager?.state?.value?.let { st ->
            if (st.sessionId == _currentSessionId.value) st.displayText else ""
        }.orEmpty()
        if (liveText.isNotBlank()) {
            _liveTranscript.value = liveText
        }
        val recordedFile = audioManager.stopRecording()
        if (recordedFile != null) {
            _activeMediaPath.value = recordedFile.absolutePath
            _activeMediaIsVideo.value = false
        }
        if (_evaluationState.value == CoachEvaluationState.RECORDING) {
            _evaluationState.value = CoachEvaluationState.NOT_STARTED
        }
    }

    fun attachUploadedMedia(context: Context, uri: Uri) {
        viewModelScope.launch {
            resetForNewMedia()

            val checkOutcome = withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "video/mp4"
                var declaredBytes = -1L
                runCatching {
                    resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (idx >= 0 && !cursor.isNull(idx)) {
                                declaredBytes = cursor.getLong(idx)
                            }
                        }
                    }
                }
                if (declaredBytes <= 0L) {
                    runCatching {
                        resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                            if (afd.length > 0L) declaredBytes = afd.length
                        }
                    }
                }
                if (declaredBytes > MAX_MEDIA_BYTES) {
                    return@withContext Result.failure<AttachedMedia>(
                        IllegalArgumentException("Selected file exceeds 19MB limit (${declaredBytes / (1024 * 1024)}MB). Please pick a shorter clip.")
                    )
                }

                val isVideo = mime.contains("video", ignoreCase = true)
                val ext = if (isVideo) "mp4" else "m4a"
                val out = File(audioManager.recordingsDir(), "speakcraft_clip_${System.currentTimeMillis()}.$ext")
                var total = 0L
                val copied = resolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            total += read
                            if (total > MAX_MEDIA_BYTES) return@use false
                            output.write(buffer, 0, read)
                        }
                        true
                    }
                }
                if (copied == false) {
                    out.delete()
                    return@withContext Result.failure<AttachedMedia>(
                        IllegalArgumentException("Selected file exceeds 19MB limit. Please pick a shorter clip.")
                    )
                }
                if (copied == null || total == 0L) {
                    out.delete()
                    return@withContext Result.failure<AttachedMedia>(
                        IllegalStateException("Unable to read selected media file.")
                    )
                }
                val dur = audioManager.inspectMediaDurationSeconds(uri)
                Result.success(AttachedMedia(mime, total, dur, out.absolutePath))
            }

            checkOutcome.onFailure { err ->
                _analysisState.value = UiState.Error(err.message ?: "Unable to read selected media file.")
            }.onSuccess { media ->
                uploadedMimeType = media.mime
                uploadedMediaPath = media.path
                _activeMediaPath.value = media.path
                _activeMediaIsVideo.value = media.mime.contains("video", ignoreCase = true) || media.path.endsWith(".mp4", ignoreCase = true)
                val durNote = if (media.durationSeconds > 0) " • ${media.durationSeconds}s" else ""
                _uploadedMediaLabel.value = "Attached clip (${media.sizeBytes / 1024} KB$durNote • ${media.mime})"
            }
        }
    }

    fun attachRecordedVideoFile(file: File, durationSeconds: Int) {
        viewModelScope.launch {
            resetForNewMedia()
            if (!file.exists() || file.length() <= 0L) {
                _analysisState.value = UiState.Error("Recorded video file was empty.")
                return@launch
            }
            if (file.length() > MAX_MEDIA_BYTES) {
                _analysisState.value = UiState.Error("Recorded video exceeds 19MB upload limit (${file.length() / (1024 * 1024)}MB). Try a 1–2 minute clip.")
                return@launch
            }
            uploadedMimeType = "video/mp4"
            uploadedMediaPath = file.absolutePath
            _activeMediaPath.value = file.absolutePath
            _activeMediaIsVideo.value = true
            _uploadedMediaLabel.value = "CameraX Video (${file.length() / 1024} KB • ${durationSeconds.coerceAtLeast(0)}s)"
        }
    }

    private fun resetForNewMedia() {
        val newSessionId = UUID.randomUUID().toString()
        _currentSessionId.value = newSessionId
        _liveTranscript.value = ""
        liveTranscriptionManager?.prepareNewSession(newSessionId, aiSettings.value)
        _analysisState.value = null
        _evaluationReport.value = null
        _evaluationState.value = CoachEvaluationState.NOT_STARTED
        _playbackStatusNote.value = null
        _cloudSyncError.value = null
        pendingUnsavedSession = null
    }

    fun playSavedOrCurrentMedia(path: String?, muted: Boolean = false) {
        val target = path?.takeIf { it.isNotBlank() }
            ?: uploadedMediaPath.takeIf { it.isNotBlank() }
            ?: audioManager.recordedFilePath.value
        if (target.isNullOrBlank()) {
            _playbackStatusNote.value = "No local audio/video file found for this recording."
            return
        }
        val ok = audioManager.playFile(target, muted)
        _playbackStatusNote.value = if (ok) {
            if (muted) "Playing muted (Stage 1 visual/posture check)" else "Playing recording audio"
        } else {
            "Local media file is no longer present on this device."
        }
    }

    /**
     * Evaluates the current recording (or attached clip) with the local intelligence engine.
     *
     * [practiceTarget] chooses what is checked; when it is `null` the template comes from the prompt
     * ([LabPracticeTarget.fromPrompt]: PREP/STAR → structure check, otherwise free speaking). The transcript is
     * [liveTranscriptOverride], else this session's live transcript; blank means audio-only feedback.
     * Only a valid (not inconclusive) result is saved.
     */
    fun submitForAiAnalysis(
        moduleType: String,
        promptTitle: String,
        extraInstructions: String = "",
        practiceTarget: LabPracticeTarget? = null,
        liveTranscriptOverride: String = ""
    ) {
        viewModelScope.launch {
            if (audioManager.isRecording.value) {
                stopRecording()
            }
            val sessionId = _currentSessionId.value
            val liveState = liveTranscriptionManager?.state?.value
            val sessionScopedLive = liveState?.takeIf { it.sessionId == sessionId }?.displayText.orEmpty()
            val effectiveTranscript = liveTranscriptOverride.trim()
                .ifBlank { sessionScopedLive }
                .ifBlank { _liveTranscript.value.trim() }

            val hasUpload = uploadedMediaPath.isNotBlank()
            val mime = if (hasUpload) uploadedMimeType else "audio/mp4"
            val localPath = if (hasUpload) uploadedMediaPath else audioManager.recordedFilePath.value.orEmpty()
            val localFile = localPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() && it.length() > 0L }

            _activeAnalysisModule.value = moduleType
            // Never evaluate without a recording: a typed or live transcript alone is not speech evidence.
            if (localFile == null) {
                val missing = EvaluationEvidenceValidator.validate(
                    EvidenceValidationInput(
                        sessionId = sessionId,
                        audioRequired = true,
                        audioBytes = ByteArray(0),
                        audioFile = null,
                        videoAssessmentSelected = false
                    )
                ).validation
                val report = CoachEvaluationReport(
                    sessionId = sessionId,
                    evaluationState = CoachEvaluationState.INSUFFICIENT_EVIDENCE,
                    overallScore = null,
                    statusHeadline = missing.userTitle.ifBlank { "No recording found" },
                    statusMessage = missing.userMessage.ifBlank { "Record your answer first, then ask for feedback." },
                    isPartialEvaluation = false,
                    partialReason = null,
                    dimensions = emptyMap(),
                    honestyNotice = "No score was given or saved because there was no recording to evaluate.",
                    validation = missing
                )
                _evaluationState.value = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                _evaluationReport.value = report
                _analysisState.value = UiState.Error("${report.statusHeadline} ${report.statusMessage}".trim())
                return@launch
            }

            _evaluationState.value = CoachEvaluationState.EVALUATING
            _evaluationReport.value = null
            _analysisState.value = UiState.Loading
            _cloudSyncError.value = null
            pendingUnsavedSession = null

            val request = LabAttemptRequest(
                sessionId = sessionId,
                moduleType = moduleType,
                promptTitle = promptTitle,
                target = practiceTarget ?: LabPracticeTarget.fromPrompt(promptTitle, extraInstructions),
                mimeType = mime,
                localMediaPath = localPath
            )
            val report = withContext(evaluationDispatcher) {
                val features = runCatching { recordingAnalyzer(localFile) }.getOrNull()
                LabAttemptEvaluator.evaluate(request, features, effectiveTranscript, tipCatalog())
            }

            _evaluationReport.value = report
            _evaluationState.value = report.evaluationState

            val evaluatedSession = report.session
            if (report.evaluationState == CoachEvaluationState.COMPLETED && evaluatedSession != null) {
                val saveRes = repository.saveSpeakingSession(evaluatedSession)
                if (saveRes.isSuccess) {
                    _cloudSyncError.value = null
                    pendingUnsavedSession = null
                    val savedSession = evaluatedSession.copy(id = saveRes.getOrDefault(""))
                    _analysisState.value = UiState.Success(savedSession)
                    onSessionCompleted?.invoke(savedSession)
                } else {
                    pendingUnsavedSession = evaluatedSession
                    val errDetail = saveRes.exceptionOrNull()?.localizedMessage ?: "Firestore unreachable"
                    _cloudSyncError.value = "Coaching feedback generated, but cloud sync failed ($errDetail). Tap Retry Cloud Sync so your progress is saved."
                    _analysisState.value = UiState.Success(evaluatedSession)
                    onSessionCompleted?.invoke(evaluatedSession)
                }
            } else {
                // Inconclusive / unreadable: never save a session or notify completion.
                _analysisState.value = UiState.Error(
                    "${report.statusHeadline} ${report.statusMessage}".trim()
                )
            }
        }
    }

    fun recordAgain() {
        clearAnalysis()
        _evaluationState.value = CoachEvaluationState.WAITING_FOR_AUDIO
    }

    fun cancelEvaluation() {
        if (audioManager.isRecording.value) {
            stopRecording()
        }
        clearAnalysis()
        _evaluationState.value = CoachEvaluationState.CANCELLED
    }

    fun retryCloudSync() {
        val pending = pendingUnsavedSession ?: return
        if (pending.evaluationStatus != "COMPLETED") return
        viewModelScope.launch {
            val res = repository.saveSpeakingSession(pending)
            res.onSuccess { savedId ->
                pendingUnsavedSession = null
                _cloudSyncError.value = null
                _analysisState.value = UiState.Success(pending.copy(id = savedId))
            }.onFailure { err ->
                _cloudSyncError.value = "Retry failed (${err.localizedMessage ?: "network error"}). Check connection and try again."
            }
        }
    }

    fun deleteSavedSession(session: SpeakingSession) {
        if (session.id.isBlank()) return
        viewModelScope.launch {
            repository.deleteSpeakingSession(session.id, session.feedbackId, session.localMediaPath)
            audioManager.deleteFile(session.localMediaPath)
        }
    }

    fun clearAnalysis() {
        _analysisState.value = null
        _evaluationReport.value = null
        _evaluationState.value = CoachEvaluationState.NOT_STARTED
        _activeAnalysisModule.value = null
        _playbackStatusNote.value = null
        _cloudSyncError.value = null
        pendingUnsavedSession = null
        uploadedMediaPath = ""
        _uploadedMediaLabel.value = null
        _activeMediaPath.value = null
        _activeMediaIsVideo.value = false
        _liveTranscript.value = ""
        val freshId = UUID.randomUUID().toString()
        _currentSessionId.value = freshId
        liveTranscriptionManager?.prepareNewSession(freshId, aiSettings.value)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        // Only stop active playback/transcription; never permanently destroy AppContainer-shared singletons.
        liveTranscriptionManager?.stopTranscription()
        audioManager.stopPlayback()
        if (audioManager.isRecording.value) {
            audioManager.stopRecording()
        }
    }

    private data class AttachedMedia(val mime: String, val sizeBytes: Long, val durationSeconds: Int, val path: String)
}
