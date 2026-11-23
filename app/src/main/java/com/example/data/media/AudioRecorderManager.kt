package com.example.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AudioRecorderMgr"

class AudioRecorderManager(val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var outputFile: File? = null
    private var meteringJob: Job? = null
    private var accumulatedActiveMs: Long = 0L
    private var activeSegmentStartMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playingFilePath = MutableStateFlow<String?>(null)
    val playingFilePath: StateFlow<String?> = _playingFilePath.asStateFlow()

    private val _isMutedPlayback = MutableStateFlow(false)
    val isMutedPlayback: StateFlow<Boolean> = _isMutedPlayback.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _recordedFilePath = MutableStateFlow<String?>(null)
    val recordedFilePath: StateFlow<String?> = _recordedFilePath.asStateFlow()

    fun recordingsDir(): File = File(context.noBackupFilesDir, "recordings").apply { mkdirs() }

    fun startRecording(scope: CoroutineScope): Boolean {
        stopPlayback()
        releaseRecorder()
        val file = File(recordingsDir(), "speakcraft_rec_${System.currentTimeMillis()}.m4a")
        outputFile = file
        return try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            accumulatedActiveMs = 0L
            activeSegmentStartMs = SystemClock.elapsedRealtime()
            _isRecording.value = true
            _isPaused.value = false
            _elapsedSeconds.value = 0
            _recordedFilePath.value = null

            meteringJob?.cancel()
            meteringJob = scope.launch(Dispatchers.Default) {
                while (isActive && _isRecording.value) {
                    if (!_isPaused.value) {
                        val maxAmp = try {
                            recorder?.maxAmplitude ?: 0
                        } catch (_: Exception) {
                            0
                        }
                        _amplitude.value = (maxAmp / 32767f).coerceIn(0.05f, 1f)
                        val totalMs = accumulatedActiveMs + (SystemClock.elapsedRealtime() - activeSegmentStartMs)
                        _elapsedSeconds.value = (totalMs / 1000L).toInt().coerceAtLeast(0)
                    } else {
                        _amplitude.value = 0.04f
                    }
                    delay(80L)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder", e)
            releaseRecorder()
            false
        }
    }

    fun pauseRecording(): Boolean {
        if (!_isRecording.value || _isPaused.value) return false
        return try {
            recorder?.pause()
            accumulatedActiveMs += (SystemClock.elapsedRealtime() - activeSegmentStartMs).coerceAtLeast(0L)
            _isPaused.value = true
            _amplitude.value = 0.04f
            true
        } catch (e: Exception) {
            Log.w(TAG, "Pause recording failed: ${e.message}")
            false
        }
    }

    fun resumeRecording(): Boolean {
        if (!_isRecording.value || !_isPaused.value) return false
        return try {
            recorder?.resume()
            activeSegmentStartMs = SystemClock.elapsedRealtime()
            _isPaused.value = false
            true
        } catch (e: Exception) {
            Log.w(TAG, "Resume recording failed: ${e.message}")
            false
        }
    }

    fun stopRecording(): File? {
        meteringJob?.cancel()
        meteringJob = null
        if (_isRecording.value && !_isPaused.value) {
            accumulatedActiveMs += (SystemClock.elapsedRealtime() - activeSegmentStartMs).coerceAtLeast(0L)
            _elapsedSeconds.value = (accumulatedActiveMs / 1000L).toInt().coerceAtLeast(0)
        }
        _isRecording.value = false
        _isPaused.value = false
        _amplitude.value = 0f
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Stop recorder warning: ${e.message}")
        }
        releaseRecorder()
        val file = outputFile
        if (file != null && file.exists() && file.length() > 0L) {
            val inspectedDuration = inspectMediaDurationSeconds(Uri.fromFile(file))
            if (inspectedDuration > 0) {
                _elapsedSeconds.value = inspectedDuration
            }
            _recordedFilePath.value = file.absolutePath
            return file
        }
        return null
    }

    fun startPlayback(muted: Boolean = false) {
        val path = _recordedFilePath.value ?: return
        playFile(path, muted)
    }

    fun playFile(path: String, muted: Boolean = false): Boolean {
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return false
        stopPlayback()
        return try {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                val vol = if (muted) 0f else 1f
                setVolume(vol, vol)
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playingFilePath.value = null
                }
                start()
            }
            player = mp
            _playingFilePath.value = file.absolutePath
            _isMutedPlayback.value = muted
            _isPlaying.value = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Playback error for $path", e)
            stopPlayback()
            false
        }
    }

    fun toggleMutePlayback() {
        val muted = !_isMutedPlayback.value
        _isMutedPlayback.value = muted
        val vol = if (muted) 0f else 1f
        player?.setVolume(vol, vol)
    }

    fun stopPlayback() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        _isPlaying.value = false
        _playingFilePath.value = null
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        if (_playingFilePath.value == path) {
            stopPlayback()
        }
        if (_recordedFilePath.value == path) {
            _recordedFilePath.value = null
        }
        return runCatching {
            val f = File(path)
            if (f.exists()) f.delete() else true
        }.getOrDefault(false)
    }

    fun readRecordedBytes(): ByteArray? {
        val path = _recordedFilePath.value ?: return null
        val file = File(path)
        return if (file.exists() && file.length() > 0L) file.readBytes() else null
    }

    fun inspectMediaDurationSeconds(uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            (ms / 1000L).toInt().coerceIn(0, 3600)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect media duration: ${e.message}")
            0
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun releaseRecorder() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    fun release() {
        meteringJob?.cancel()
        stopPlayback()
        releaseRecorder()
    }
}
