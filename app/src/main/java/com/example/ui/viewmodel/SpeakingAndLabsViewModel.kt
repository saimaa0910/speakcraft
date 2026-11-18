package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiCoachService
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

class SpeakingAndLabsViewModel(
    private val repository: SpeakCraftRepository,
    private val geminiCoach: GeminiCoachService,
    val audioManager: AudioRecorderManager
) : ViewModel() {

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
        audioManager.stopRecording()
    }

    fun attachUploadedMedia(context: Context, uri: Uri) {
        viewModelScope.launch {
            _analysisState.value = null
            _playbackStatusNote.value = null
            val result = withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "video/mp4"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                val dur = audioManager.inspectMediaDurationSeconds(uri)
                val ext = if (mime.contains("video")) "mp4" else "m4a"
                val cachedCopy = if (bytes != null && bytes.isNotEmpty() && bytes.size <= 19 * 1024 * 1024) {
                    val out = File(audioManager.recordingsDir(), "speakcraft_clip_${System.currentTimeMillis()}.$ext")
                    out.writeBytes(bytes)
                    out.absolutePath
                } else ""
                Triple(mime, bytes, Pair(dur, cachedCopy))
            }
            val bytes = result.second
            if (bytes == null || bytes.isEmpty()) {
                _analysisState.value = UiState.Error("Unable to read selected media file.")
                return@launch
            }
            if (bytes.size > 19 * 1024 * 1024) {
                _analysisState.value = UiState.Error("Selected file exceeds 19MB limit (${bytes.size / (1024 * 1024)}MB). Please pick a shorter clip.")
                return@launch
            }
            uploadedMediaBytes = bytes
            uploadedMimeType = result.first
            uploadedDurationSeconds = result.third.first
            uploadedMediaPath = result.third.second
            val durNote = if (uploadedDurationSeconds > 0) " • ${uploadedDurationSeconds}s" else ""
            _uploadedMediaLabel.value = "Attached clip (${bytes.size / 1024} KB$durNote • ${result.first})"
        }
    }

    fun attachRecordedVideoFile(file: File, durationSeconds: Int) {
        viewModelScope.launch {
            _analysisState.value = null
            _playbackStatusNote.value = null
            val bytes = withContext(Dispatchers.IO) {
                if (file.exists() && file.length() > 0L) file.readBytes() else null
            }
            if (bytes == null || bytes.isEmpty()) {
                _analysisState.value = UiState.Error("Recorded video file was empty.")
                return@launch
            }
            if (bytes.size > 19 * 1024 * 1024) {
                _analysisState.value = UiState.Error("Recorded video exceeds 19MB upload limit. Try a 1–2 minute clip.")
                return@launch
            }
            uploadedMediaBytes = bytes
            uploadedMimeType = "video/mp4"
            uploadedMediaPath = file.absolutePath
            uploadedDurationSeconds = durationSeconds.coerceAtLeast(1)
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
            val analysisResult = geminiCoach.analyzeSpeechRecording(
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
                    _analysisState.value = UiState.Success(session.copy(id = saveRes.getOrDefault("")))
                } else {
                    _analysisState.value = UiState.Success(session)
                }
            }.onFailure { err ->
                _analysisState.value = UiState.Error(err.localizedMessage ?: "AI speech analysis failed.")
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
        uploadedMediaBytes = null
        uploadedMediaPath = ""
        uploadedDurationSeconds = 0
        _uploadedMediaLabel.value = null
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        audioManager.release()
    }
}
