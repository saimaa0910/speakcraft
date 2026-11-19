package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.local.LocalIntelligenceOrchestrator
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.media.AudioRecorderManager
import com.example.data.model.SpeakingSession
import com.example.data.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_MEDIA_BYTES = 19 * 1024 * 1024L

class SpeakingAndLabsViewModel(
    private val repository: SpeakCraftRepository,
    private val localOrchestrator: LocalIntelligenceOrchestrator,
    val audioManager: AudioRecorderManager
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

    private var pendingUnsavedSession: SpeakingSession? = null
    private var uploadedMediaBytes: ByteArray? = null
    private var uploadedMimeType: String = "audio/mp4"
    private var uploadedMediaPath: String = ""
    private var uploadedDurationSeconds: Int = 0
    private var countdownJob: Job? = null

    fun startRecording(challengeSeconds: Int? = null) {
        uploadedMediaBytes = null
        uploadedMediaPath = ""
        uploadedDurationSeconds = 0
        _uploadedMediaLabel.value = null
        _activeMediaPath.value = null
        _activeMediaIsVideo.value = false
        _cloudSyncError.value = null
        pendingUnsavedSession = null
        _analysisState.value = null
        _playbackStatusNote.value = null
        val started = audioManager.startRecording(viewModelScope)
        if (!started) {
            _analysisState.value = UiState.Error("Could not start microphone recorder. Check audio permission.")
            return
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
        val recordedFile = audioManager.stopRecording()
        if (recordedFile != null) {
            _activeMediaPath.value = recordedFile.absolutePath
            _activeMediaIsVideo.value = false
        }
    }

    fun attachUploadedMedia(context: Context, uri: Uri) {
        viewModelScope.launch {
            _analysisState.value = null
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
            _analysisState.value = null
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
            uploadedDurationSeconds = durationSeconds.coerceAtLeast(1)
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
        extraInstructions: String = ""
    ) {
        viewModelScope.launch {
            if (audioManager.isRecording.value) {
                stopRecording()
            }
            val hasUpload = uploadedMediaBytes != null
            val bytes = uploadedMediaBytes ?: audioManager.readRecordedBytes()
            val mime = if (hasUpload) uploadedMimeType else "audio/mp4"
            val localPath = if (hasUpload) uploadedMediaPath else audioManager.recordedFilePath.value.orEmpty()
            val duration = if (hasUpload) {
                uploadedDurationSeconds.coerceAtLeast(1)
            } else {
                audioManager.elapsedSeconds.value.coerceAtLeast(1)
            }

            if (bytes == null || bytes.isEmpty()) {
                _activeAnalysisModule.value = moduleType
                _analysisState.value = UiState.Error("Please record your voice or attach a video/audio clip first.")
                return@launch
            }

            _activeAnalysisModule.value = moduleType
            _analysisState.value = UiState.Loading
            _cloudSyncError.value = null
            pendingUnsavedSession = null

            val analysisResult = localOrchestrator.analyzeSpeechRecording(
                mediaBytes = bytes,
                mimeType = mime,
                moduleType = moduleType,
                promptTitle = promptTitle,
                durationSeconds = duration,
                previousSession = previousSession,
                extraInstructions = extraInstructions,
                localMediaPath = localPath
            )

            analysisResult.onSuccess { session ->
                val saveRes = repository.saveSpeakingSession(session)
                if (saveRes.isSuccess) {
                    _cloudSyncError.value = null
                    pendingUnsavedSession = null
                    _analysisState.value = UiState.Success(session.copy(id = saveRes.getOrDefault("")))
                } else {
                    pendingUnsavedSession = session
                    val errDetail = saveRes.exceptionOrNull()?.localizedMessage ?: "Firestore unreachable"
                    _cloudSyncError.value = "AI feedback generated, but cloud sync failed ($errDetail). Tap Retry Cloud Sync so your progress is saved."
                    _analysisState.value = UiState.Success(session)
                }
            }.onFailure { err ->
                _analysisState.value = UiState.Error(err.localizedMessage ?: "AI speech analysis failed.")
            }
        }
    }

    fun retryCloudSync() {
        val pending = pendingUnsavedSession ?: return
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
        _activeAnalysisModule.value = null
        _playbackStatusNote.value = null
        _cloudSyncError.value = null
        pendingUnsavedSession = null
        uploadedMediaBytes = null
        uploadedMediaPath = ""
        uploadedDurationSeconds = 0
        _uploadedMediaLabel.value = null
        _activeMediaPath.value = audioManager.recordedFilePath.value
        _activeMediaIsVideo.value = false
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        audioManager.release()
    }
}
