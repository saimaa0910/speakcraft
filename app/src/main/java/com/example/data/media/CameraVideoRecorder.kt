package com.example.data.media

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.speakCraftColors
import kotlinx.coroutines.delay
import java.io.File

private const val TAG = "CameraVideoRecorder"

@SuppressLint("MissingPermission")
@Composable
fun InAppCameraVideoDialog(
    onDismiss: () -> Unit,
    onVideoSaved: (Uri, File, Int) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var useFrontCamera by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSec = 0
            while (isRecording) {
                delay(1000L)
                elapsedSec++
            }
        }
    }

    DisposableEffect(useFrontCamera, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD))
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                videoCapture = capture
            } catch (e: Exception) {
                Log.e(TAG, "CameraX bind error", e)
                onError("Unable to start camera preview: ${e.message}")
            }
        }, mainExecutor)

        onDispose {
            runCatching { activeRecording?.stop() }
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val ext = MaterialTheme.speakCraftColors
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(560.dp),
            shape = RoundedCornerShape(24.dp),
            color = ext.cameraBackdrop
        ) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ext.cameraScrim,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isRecording) "REC %02d:%02d".format(elapsedSec / 60, elapsedSec % 60) else "CameraX Video Audit",
                            color = if (isRecording) ext.recordingPulse else ext.onHeroTitle,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { if (!isRecording) useFrontCamera = !useFrontCamera },
                            enabled = !isRecording
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera", tint = ext.onHeroTitle)
                        }
                        IconButton(
                            onClick = {
                                activeRecording?.stop()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close camera", tint = ext.onHeroTitle)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ext.cameraScrim)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Frame chest-to-waist (Power Zone) for @askvinh 3-Stage Video Audit",
                        color = ext.onHeroSubtitle,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val capture = videoCapture ?: return@Button
                            if (!isRecording) {
                                val dir = File(context.filesDir, "recordings").apply { mkdirs() }
                                val outFile = File(dir, "speakcraft_vid_${System.currentTimeMillis()}.mp4")
                                currentFile = outFile
                                val outputOptions = FileOutputOptions.Builder(outFile).build()
                                val pending = capture.output.prepareRecording(context, outputOptions)
                                    .withAudioEnabled()
                                val durationSnapshotStart = System.currentTimeMillis()
                                val rec = pending.start(mainExecutor) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> isRecording = true
                                        is VideoRecordEvent.Finalize -> {
                                            isRecording = false
                                            if (event.hasError()) {
                                                onError("Video recording failed (${event.error}).")
                                            } else {
                                                val sec = ((System.currentTimeMillis() - durationSnapshotStart) / 1000L)
                                                    .toInt().coerceAtLeast(elapsedSec.coerceAtLeast(1))
                                                onVideoSaved(Uri.fromFile(outFile), outFile, sec)
                                            }
                                        }
                                    }
                                }
                                activeRecording = rec
                            } else {
                                activeRecording?.stop()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("camerax_record_toggle_button")
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null
                        )
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text(if (isRecording) "Stop & Attach Video" else "Start Video Capture")
                    }
                }
            }
        }
    }
}
