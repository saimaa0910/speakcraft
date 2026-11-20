package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.HybridAiCoachCoordinator
import com.example.data.ai.IntelligenceCapabilityManager
import com.example.data.ai.IntelligenceCapabilityMatrix
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.ai.local.audio.LiveTranscriptionManager
import com.example.data.ai.local.audio.LiveTranscriptionState
import com.example.data.evaluation.CoachEvaluationReport
import com.example.data.evaluation.CoachEvaluationState
import com.example.data.evaluation.EvaluationEvidenceValidator
import com.example.data.evaluation.EvidenceBasedScoringEngine
import com.example.data.evaluation.EvidenceValidationInput
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.SpeakingSession
import com.example.data.model.UiState
import com.example.data.preferences.AISettings
import com.example.data.preferences.AISettingsRepository
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

class SpeakingAndLabsViewModel(
    private val repository: SpeakCraftRepository,
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    val audioManager: AudioRecorderManager,
    private val hybridCoordinator: HybridAiCoachCoordinator? = null,
    private val aiSettingsRepo: AISettingsRepository? = null,
    private val liveTranscriptionManager: LiveTranscriptionManager? = null,
    private val capabilityManager: IntelligenceCapabilityManager? = null
) : ViewModel() {

    @Deprecated("Use constructor with LocalIntelligenceOrchestrator")
    constructor(
        repository: SpeakCraftRepository,
        geminiCoach: com.example.data.ai.GeminiCoachService,
        audioManager: AudioRecorderManager
    ) : this(
        repository = repository,
        localOrchestrator = LocalIntelligenceOrchestrator(audioManager.context),
        audioManager = audioManager
    )

    private val effectiveCoordinator: HybridAiCoachCoordinator by lazy {
        hybridCoordinator ?: HybridAiCoachCoordinator(
            localOrchestrator = localOrchestrator,
            appContext = audioManager.context,
            aiSettingsRepo = aiSettingsRepo
        )
    }

    val aiSettings: StateFlow<AISettings> =
        (aiSettingsRepo?.aiSettingsFlow ?: effectiveCoordinator.aiSettingsFlow)
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

    val cloudAiEnabled: StateFlow<Boolean> = effectiveCoordinator.cloudAiEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    private val _liveTranscriptConfidence = MutableStateFlow(0.85f)
    val liveTranscriptConfidence: StateFlow<Float> = _liveTranscriptConfidence.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcriptionError = MutableStateFlow<String?>(null)
    val transcriptionError: StateFlow<String?> = _transcriptionError.asStateFlow()

    var onSessionCompleted: ((SpeakingSession) -> Unit)? = null

    init {
        viewModelScope.launch {
            aiSettings.collect { settings ->
                liveTranscriptionManager?.refreshFromSettings(settings)
            }
        }
    }

    fun setLiveTranscript(text: String, confidence: Float = 0.85f) {
        _liveTranscript.value = text
        _liveTranscriptConfidence.value = confidence.coerceIn(0f, 1f)
        liveTranscriptionManager?.updateEditedTranscript(text)
    }

    fun setCloudAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            effectiveCoordinator.setCloudAiEnabled(enabled)
        }
    }

    fun clearTranscriptionError() {
        _transcriptionError.value = null
    }

    fun retryLiveTranscription() {
        liveTranscriptionManager?.retryTranscription()
    }

    fun transcribeRecordedMedia(hintPrompt: String = "") {
        viewModelScope.launch {
            if (audioManager.isRecording.value) {
                stopRecording()
            }
            val bytes = uploadedMediaBytes ?: audioManager.readRecordedBytes()
            val path = if (uploadedMediaPath.isNotBlank()) uploadedMediaPath else audioManager.recordedFilePath.value.orEmpty()
            val mime = if (uploadedMediaBytes != null) uploadedMimeType else "audio/mp4"
            val file = path.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() && it.length() > 0L }
            if ((bytes == null || bytes.isEmpty()) && file == null) {
                _transcriptionError.value = "Record or attach audio first before transcribing."
                return@launch
            }
            _isTranscribing.value = true
            _transcriptionError.value = null
            val mediaBytes = bytes ?: withContext(Dispatchers.IO) { file!!.readBytes() }
            val result = effectiveCoordinator.transcribeAudio(
                audioBytes = mediaBytes,
                mimeType = mime,
                hintPrompt = hintPrompt,
                mediaFile = file
            )
            _isTranscribing.value = false
            result.onSuccess { text ->
                _liveTranscript.value = text
                _liveTranscriptConfidence.value = 0.94f
                liveTranscriptionManager?.updateEditedTranscript(text)
            }.onFailure { err ->
                _transcriptionError.value = err.localizedMessage ?: "Audio transcription failed."
            }
        }
    }

    private var pendingUnsavedSession: SpeakingSession? = null
    private var uploadedMediaBytes: ByteArray? = null
    private var uploadedMimeType: String = "audio/mp4"
    private var uploadedMediaPath: String = ""
    private var uploadedDurationSeconds: Int = 0
    private var countdownJob: Job? = null

    fun startRecording(challengeSeconds: Int? = null) {
        val newSessionId = UUID.randomUUID().toString()
        _currentSessionId.value = newSessionId
        _liveTranscript.value = ""
        _transcriptionError.value = null
        uploadedMediaBytes = null
        uploadedMediaPath = ""
        uploadedDurationSeconds = 0
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
                    return@withContext Result.failure<Triple<String, ByteArray, Pair<Int, String>>>(
                        IllegalArgumentException("Selected file exceeds 19MB limit (${declaredBytes / (1024 * 1024)}MB). Please pick a shorter clip.")
                    )
                }

                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(8192)
                    val outStream = java.io.ByteArrayOutputStream()
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > MAX_MEDIA_BYTES) {
                            return@withContext Result.failure<Triple<String, ByteArray, Pair<Int, String>>>(
                                IllegalArgumentException("Selected file exceeds 19MB limit. Please pick a shorter clip.")
                            )
                        }
                        outStream.write(buffer, 0, read)
                    }
                    outStream.toByteArray()
                }
                if (bytes == null || bytes.isEmpty()) {
                    return@withContext Result.failure<Triple<String, ByteArray, Pair<Int, String>>>(
                        IllegalStateException("Unable to read selected media file.")
                    )
                }
                val dur = audioManager.inspectMediaDurationSeconds(uri)
                val isVideo = mime.contains("video", ignoreCase = true)
                val ext = if (isVideo) "mp4" else "m4a"
                val out = File(audioManager.recordingsDir(), "speakcraft_clip_${System.currentTimeMillis()}.$ext")
                out.writeBytes(bytes)
                Result.success(Triple(mime, bytes, Pair(dur, out.absolutePath)))
            }

            checkOutcome.onFailure { err ->
                _analysisState.value = UiState.Error(err.message ?: "Unable to read selected media file.")
            }.onSuccess { (mime, bytes, meta) ->
                uploadedMediaBytes = bytes
                uploadedMimeType = mime
                uploadedDurationSeconds = meta.first
                uploadedMediaPath = meta.second
                _activeMediaPath.value = meta.second
                _activeMediaIsVideo.value = mime.contains("video", ignoreCase = true) || meta.second.endsWith(".mp4", ignoreCase = true)
                val durNote = if (uploadedDurationSeconds > 0) " • ${uploadedDurationSeconds}s" else ""
                _uploadedMediaLabel.value = "Attached clip (${bytes.size / 1024} KB$durNote • $mime)"
            }
        }
    }

    fun attachRecordedVideoFile(file: File, durationSeconds: Int) {
        viewModelScope.launch {
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
            if (!file.exists() || file.length() <= 0L) {
                _analysisState.value = UiState.Error("Recorded video file was empty.")
                return@launch
            }
            if (file.length() > MAX_MEDIA_BYTES) {
                _analysisState.value = UiState.Error("Recorded video exceeds 19MB upload limit (${file.length() / (1024 * 1024)}MB). Try a 1–2 minute clip.")
                return@launch
            }
            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
            uploadedMediaBytes = bytes
            uploadedMimeType = "video/mp4"
            uploadedMediaPath = file.absolutePath
            uploadedDurationSeconds = durationSeconds.coerceAtLeast(0)
            _activeMediaPath.value = file.absolutePath
            _activeMediaIsVideo.value = true
            _uploadedMediaLabel.value = "CameraX Video (${bytes.size / 1024} KB • ${uploadedDurationSeconds}s)"
        }
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

    fun submitForAiAnalysis(
        moduleType: String,
        promptTitle: String,
        previousSession: SpeakingSession?,
        extraInstructions: String = "",
        liveTranscriptOverride: String = "",
        asrConfidenceOverride: Float = _liveTranscriptConfidence.value,
        videoAssessmentSelected: Boolean = _activeMediaIsVideo.value
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

            val hasUpload = uploadedMediaBytes != null
            val bytes = uploadedMediaBytes ?: audioManager.readRecordedBytes()
            val mime = if (hasUpload) uploadedMimeType else "audio/mp4"
            val localPath = if (hasUpload) uploadedMediaPath else audioManager.recordedFilePath.value.orEmpty()
            val localFile = localPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() && it.length() > 0L }

            // Strict evidence gate: NEVER bypass missing audio via typed/live transcript or coerced durations
            if ((bytes == null || bytes.isEmpty()) && localFile == null) {
                val missingBundle = EvaluationEvidenceValidator.validate(
                    EvidenceValidationInput(
                        sessionId = sessionId,
                        audioRequired = true,
                        audioBytes = ByteArray(0),
                        audioFile = null,
                        videoAssessmentSelected = videoAssessmentSelected
                    )
                )
                val report = EvidenceBasedScoringEngine.score(
                    EvidenceBasedScoringEngine.ScoringInput(
                        sessionId = sessionId,
                        moduleType = moduleType,
                        promptTitle = promptTitle,
                        mimeType = mime,
                        localMediaPath = "",
                        validated = missingBundle,
                        previousSession = previousSession
                    )
                )
                _activeAnalysisModule.value = moduleType
                _evaluationState.value = CoachEvaluationState.INSUFFICIENT_EVIDENCE
                _evaluationReport.value = report
                _analysisState.value = UiState.Error("${report.statusHeadline} ${report.statusMessage}".trim())
                return@launch
            }

            val measuredReportedDuration = if (hasUpload) {
                uploadedDurationSeconds
            } else {
                audioManager.elapsedSeconds.value
            }

            _activeAnalysisModule.value = moduleType
            _evaluationState.value = CoachEvaluationState.VALIDATING_EVIDENCE
            _evaluationReport.value = null
            _analysisState.value = UiState.Loading
            _cloudSyncError.value = null
            pendingUnsavedSession = null

            _evaluationState.value = CoachEvaluationState.EVALUATING
            val report = effectiveCoordinator.evaluateSpeechWithReport(
                sessionId = sessionId,
                mediaBytes = bytes ?: ByteArray(0),
                mimeType = mime,
                moduleType = moduleType,
                promptTitle = promptTitle,
                durationSeconds = measuredReportedDuration,
                previousSession = previousSession,
                extraInstructions = extraInstructions,
                localMediaPath = localPath,
                liveTranscript = effectiveTranscript,
                transcriptSessionId = sessionId,
                asrConfidence = asrConfidenceOverride,
                videoAssessmentSelected = videoAssessmentSelected
            )

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
                // Incomplete / Insufficient / Failed analysis: NEVER save fake session or notify completion
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
        uploadedMediaBytes = null
        uploadedMediaPath = ""
        uploadedDurationSeconds = 0
        _uploadedMediaLabel.value = null
        _activeMediaPath.value = null
        _activeMediaIsVideo.value = false
        _liveTranscript.value = ""
        _transcriptionError.value = null
        val freshId = UUID.randomUUID().toString()
        _currentSessionId.value = freshId
        liveTranscriptionManager?.prepareNewSession(freshId, aiSettings.value)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        liveTranscriptionManager?.release()
        audioManager.release()
    }
}
