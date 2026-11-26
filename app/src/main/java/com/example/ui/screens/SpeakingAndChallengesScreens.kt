package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.data.ai.PendingStudioGeminiHandoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.PhoneGeminiBridge
import com.example.data.ai.StudioGeminiFeedbackEntry
import com.example.data.ai.StudioTaskType
import com.example.data.evaluation.CoachEvaluationState
import com.example.data.evaluation.LabAttemptEvaluator
import com.example.data.evaluation.LabPracticeTarget
import com.example.data.media.InAppCameraVideoDialog
import com.example.data.model.CurriculumCatalog
import com.example.data.model.LearningChallenge
import com.example.data.model.SpeakingChallengeItem
import com.example.data.model.SpeakingSession
import com.example.data.model.UiState
import com.example.ui.components.CloudSyncWarningBanner
import com.example.ui.components.CoachEvaluationStatusCard
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRecoveryCard
import com.example.ui.components.FeatureGuideStrip
import com.example.ui.components.FeedbackReportCard
import com.example.ui.components.LiveTranscriptPanel
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.components.StepProgressIndicator
import com.example.ui.components.VideoReviewPlayerCard
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.SpeakingAndLabsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatStudioDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format("%02d:%02d", safe / 60, safe % 60)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakingLabScreen(
    speakingVm: SpeakingAndLabsViewModel,
    pastSessions: List<SpeakingSession>,
    onDeleteSession: (SpeakingSession) -> Unit,
    onOpenAiSettings: () -> Unit = {},
    incomingGeminiFeedback: String? = null,
    onConsumeIncomingGeminiFeedback: () -> Unit = {}
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val isPaused by speakingVm.audioManager.isPaused.collectAsStateWithLifecycle()
    val isPlaying by speakingVm.audioManager.isPlaying.collectAsStateWithLifecycle()
    val playingPath by speakingVm.audioManager.playingFilePath.collectAsStateWithLifecycle()
    val isMuted by speakingVm.audioManager.isMutedPlayback.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val elapsed by speakingVm.audioManager.elapsedSeconds.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val uploadedLabel by speakingVm.uploadedMediaLabel.collectAsStateWithLifecycle()
    val activeMediaPath by speakingVm.activeMediaPath.collectAsStateWithLifecycle()
    val activeMediaIsVideo by speakingVm.activeMediaIsVideo.collectAsStateWithLifecycle()
    val playbackNote by speakingVm.playbackStatusNote.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationState by speakingVm.evaluationState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()
    val remainingCountdown by speakingVm.challengeRemainingSeconds.collectAsStateWithLifecycle()
    val cloudSyncError by speakingVm.cloudSyncError.collectAsStateWithLifecycle()
    val aiSettings by speakingVm.aiSettings.collectAsStateWithLifecycle()
    val liveTranscriptionState by speakingVm.liveTranscriptionState.collectAsStateWithLifecycle()
    val liveTranscript by speakingVm.liveTranscript.collectAsStateWithLifecycle()

    val quickTopicPresets = remember {
        listOf(
            "Elevator Pitch" to "Introduce yourself and explain one communication strength using the 3Vs",
            "STAR Interview" to "Describe a challenging project conflict and how you resolved it clearly using STAR",
            "Team Update" to "Deliver a concise 45-second executive status update with a clear headline and next step",
            "Story Hook" to "Tell a memorable 60-second personal story with purposeful pauses and vocal contrast"
        )
    }

    var activeTaskType by rememberSaveable { mutableStateOf(StudioTaskType.DAILY_VIDEO) }
    var dailyFreeNote by rememberSaveable { mutableStateOf("") }
    var customTopic by rememberSaveable { mutableStateOf(quickTopicPresets.first().second) }
    var sendToGeminiOnSubmit by rememberSaveable { mutableStateOf(true) }
    var geminiStatusMessage by remember { mutableStateOf<String?>(null) }
    var geminiFeedbackDraft by rememberSaveable { mutableStateOf("") }
    var geminiFeedbackHistory by remember(context) {
        mutableStateOf<List<StudioGeminiFeedbackEntry>>(emptyList())
    }
    var pendingHandoff by remember(context) {
        mutableStateOf<PendingStudioGeminiHandoff?>(null)
    }
    var hasCustomStudioPrompt by remember(context) { mutableStateOf(false) }
    LaunchedEffect(context) {
        val loaded = withContext(Dispatchers.IO) {
            Triple(
                PhoneGeminiBridge.loadStudioFeedbackHistory(context),
                PhoneGeminiBridge.getPendingStudioHandoff(context),
                PhoneGeminiBridge.hasCustomStudioGeminiPrompt(context)
            )
        }
        geminiFeedbackHistory = loaded.first
        pendingHandoff = loaded.second
        hasCustomStudioPrompt = loaded.third
    }
    var selectedAuditStage by rememberSaveable { mutableStateOf("Stage 2: Vocal (Eyes Closed)") }
    var showCameraDialog by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var pendingDeleteSession by remember { mutableStateOf<SpeakingSession?>(null) }

    val activePromptTitle = if (activeTaskType == StudioTaskType.DAILY_VIDEO) {
        dailyFreeNote.trim().ifBlank { "Daily Video Recording (Open Speak)" }
    } else {
        customTopic.trim().ifBlank { "Selected Topic Practice" }
    }

    fun persistGeminiFeedback(feedbackText: String, source: String) {
        val clean = feedbackText.trim()
        if (clean.isBlank()) return
        val handoff = pendingHandoff
        val taskLabel = handoff?.taskTypeName?.let { raw ->
            runCatching { StudioTaskType.valueOf(raw).label }.getOrNull()
        } ?: activeTaskType.label
        val entry = StudioGeminiFeedbackEntry(
            taskTypeLabel = taskLabel,
            taskTitle = handoff?.taskTitle?.ifBlank { activePromptTitle } ?: activePromptTitle,
            mediaLabel = handoff?.mediaLabel?.ifBlank {
                if (activeMediaIsVideo) "Video Clip" else "Audio Recording"
            } ?: if (activeMediaIsVideo) "Video Clip" else "Audio Recording",
            isVideo = handoff?.isVideo ?: activeMediaIsVideo,
            feedbackText = clean,
            source = source,
            receivedAtMs = System.currentTimeMillis()
        )
        geminiFeedbackHistory = PhoneGeminiBridge.saveStudioFeedbackEntry(context, entry)
        pendingHandoff = null
    }

    LaunchedEffect(incomingGeminiFeedback) {
        val incoming = incomingGeminiFeedback?.trim()
        if (!incoming.isNullOrBlank()) {
            geminiFeedbackDraft = incoming
            persistGeminiFeedback(incoming, "Shared from Gemini App")
            geminiStatusMessage = "Feedback returned from phone Gemini app and saved in Record & Review Studio."
            onConsumeIncomingGeminiFeedback()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speakingVm.startRecording(null)
    }

    val cameraPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val camOk = perms[Manifest.permission.CAMERA] == true
        val micOk = perms[Manifest.permission.RECORD_AUDIO] == true
        if (camOk && micOk) {
            cameraError = null
            showCameraDialog = true
        } else {
            cameraError = "Camera and Microphone permissions are required for in-app 3-Stage video recording."
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) speakingVm.attachUploadedMedia(context, uri)
    }

    val previousSession = pastSessions.firstOrNull()
    val hasMediaReady = !isRecording && (recordedPath != null || uploadedLabel != null || !activeMediaPath.isNullOrBlank())
    val studioStep = when {
        activeModule == "SPEAKING_LAB" && analysisState is UiState.Success -> 3
        hasMediaReady -> 2
        else -> 1
    }

    if (showCameraDialog) {
        InAppCameraVideoDialog(
            onDismiss = { showCameraDialog = false },
            onVideoSaved = { _, file, sec ->
                showCameraDialog = false
                speakingVm.attachRecordedVideoFile(file, sec)
            },
            onError = { msg ->
                showCameraDialog = false
                cameraError = msg
            }
        )
    }

    pendingDeleteSession?.let { sessToDelete ->
        ConfirmDestructiveDialog(
            title = "Delete Recording?",
            message = "This permanently removes \"${sessToDelete.promptTitle}\" and its local audio/video clip.",
            confirmLabel = "Delete",
            onConfirm = {
                onDeleteSession(sessToDelete)
                pendingDeleteSession = null
            },
            onDismiss = { pendingDeleteSession = null }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val captureCardContent: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Engine: On-Device Local Intelligence",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        androidx.compose.material3.TextButton(
                            onClick = onOpenAiSettings,
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("speaking_open_ai_settings_button")
                        ) {
                            Text("AI Settings")
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().testTag("studio_two_tasks_panel"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Studio Tasks — Choose Your Recording Mode",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                        ) {
                            FilterChip(
                                selected = activeTaskType == StudioTaskType.DAILY_VIDEO,
                                onClick = { activeTaskType = StudioTaskType.DAILY_VIDEO },
                                leadingIcon = {
                                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("1. Daily Video Recording") },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("studio_task_daily_video_chip")
                            )
                            FilterChip(
                                selected = activeTaskType == StudioTaskType.SELECT_TOPIC,
                                onClick = { activeTaskType = StudioTaskType.SELECT_TOPIC },
                                leadingIcon = {
                                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("2. Select Topics") },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("studio_task_select_topics_chip")
                            )
                        }

                        if (activeTaskType == StudioTaskType.DAILY_VIDEO) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth().testTag("studio_daily_video_banner")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Task 1: Daily Video Recording — Speak Whatever You Want",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Speak freely on any daily topic (video or audio), then submit to Studio + Phone Gemini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xs)
                        ) {
                            quickTopicPresets.forEachIndexed { idx, (label, promptText) ->
                                FilterChip(
                                    selected = activeTaskType == StudioTaskType.SELECT_TOPIC && customTopic == promptText,
                                    onClick = {
                                        activeTaskType = StudioTaskType.SELECT_TOPIC
                                        customTopic = promptText
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("speaking_preset_chip_$idx")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = if (activeTaskType == StudioTaskType.DAILY_VIDEO) dailyFreeNote else customTopic,
                            onValueChange = { text ->
                                if (activeTaskType == StudioTaskType.DAILY_VIDEO) {
                                    dailyFreeNote = text
                                } else {
                                    customTopic = text
                                }
                            },
                            label = {
                                Text(
                                    if (activeTaskType == StudioTaskType.DAILY_VIDEO) {
                                        "Task 1 Daily Video Note (Optional — leave blank to speak freely)"
                                    } else {
                                        "Task 2 Speaking Prompt or Topic"
                                    }
                                )
                            },
                            singleLine = activeTaskType == StudioTaskType.DAILY_VIDEO,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().testTag("speaking_prompt_input")
                        )
                    }

                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording, isPaused = isPaused)

                    LiveTranscriptPanel(
                        state = liveTranscriptionState,
                        liveTranscriptionEnabled = aiSettings.liveTranscriptionEnabled,
                        editedTranscript = liveTranscript,
                        editable = false,
                        onRetry = { speakingVm.retryLiveTranscription() },
                        onOpenAiSettings = onOpenAiSettings
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timerText = when {
                            remainingCountdown != null -> "Remaining: ${remainingCountdown}s"
                            isRecording && isPaused -> "Paused at ${elapsed}s"
                            isRecording -> "Recording: ${elapsed}s"
                            else -> "Recorded: ${elapsed}s"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRecording && !isPaused) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = formatStudioDuration(remainingCountdown ?: elapsed),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                timerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uploadedLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    uploadedLabel.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)) {
                        Button(
                            onClick = {
                                if (isRecording) {
                                    speakingVm.stopRecording()
                                } else {
                                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (hasMic) speakingVm.startRecording(null)
                                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("record_speech_button")
                        ) {
                            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRecording) "Stop" else "Record Mic")
                        }

                        if (isRecording) {
                            OutlinedButton(
                                onClick = { if (isPaused) speakingVm.resumeRecording() else speakingVm.pauseRecording() },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("pause_resume_speech_button")
                            ) {
                                Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPaused) "Resume" else "Pause")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val camOk = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    val micOk = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (camOk && micOk) {
                                        cameraError = null
                                        showCameraDialog = true
                                    } else {
                                        cameraPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("record_video_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Video Clip")
                            }
                        }
                    }

                    if (!isRecording) {
                        OutlinedButton(
                            onClick = {
                                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("upload_video_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Attach Video from Photo Picker")
                        }
                    }

                    if (recordedPath != null || uploadedLabel != null || !activeMediaPath.isNullOrBlank()) {
                        Text("3-Stage Self-Audit Playback", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.xs)
                        ) {
                            listOf(
                                "Stage 1: Mute Pass",
                                "Stage 2: Vocal (Eyes Closed)",
                                "Stage 3: Full Review"
                            ).forEach { stage ->
                                FilterChip(
                                    selected = selectedAuditStage == stage,
                                    onClick = { selectedAuditStage = stage },
                                    label = { Text(stage) },
                                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                )
                            }
                        }

                        if (activeMediaIsVideo && !activeMediaPath.isNullOrBlank()) {
                            VideoReviewPlayerCard(
                                videoPath = activeMediaPath.orEmpty(),
                                auditStage = selectedAuditStage
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (isPlaying) speakingVm.audioManager.stopPlayback()
                                        else speakingVm.playSavedOrCurrentMedia(activeMediaPath ?: recordedPath, muted = selectedAuditStage.startsWith("Stage 1"))
                                    },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                ) {
                                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPlaying) "Stop Playback" else "Play Current")
                                }
                                OutlinedButton(
                                    onClick = { speakingVm.audioManager.toggleMutePlayback() },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                ) {
                                    Icon(if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isMuted) "Muted" else "Sound On")
                                }
                            }
                        }
                        if (!playbackNote.isNullOrBlank()) {
                            Text(playbackNote.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ext.info)
                        }

                        OutlinedTextField(
                            value = liveTranscript,
                            onValueChange = { speakingVm.setLiveTranscript(it) },
                            label = { Text("Transcript (correct recognition mistakes if needed)") },
                            placeholder = { Text("Words recognised while you speak appear here when live transcription is on. Without a transcript you get audio-only feedback.") },
                            minLines = 2,
                            maxLines = 4,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("speaking_transcript_preview")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("studio_gemini_forward_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Also submit video/audio to Phone Gemini App",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Sends clip + prompt straight to Gemini chats so you can bring back feedback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = sendToGeminiOnSubmit,
                            onCheckedChange = { sendToGeminiOnSubmit = it },
                            modifier = Modifier.testTag("studio_forward_to_gemini_switch")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_gemini_prompt_link_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hasCustomStudioPrompt) {
                                "Using your custom Gemini prompt from Settings."
                            } else {
                                "Using default Gemini review prompt."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasCustomStudioPrompt) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onOpenAiSettings,
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("studio_edit_gemini_prompt_button")
                        ) {
                            Text("Edit Prompt")
                        }
                    }

                    Button(
                        onClick = {
                            val currentPath = activeMediaPath?.takeIf { it.isNotBlank() }
                                ?: recordedPath?.takeIf { it.isNotBlank() }
                            val currentFile = currentPath?.let(::File)
                            val transcriptText = liveTranscript.ifBlank { liveTranscriptionState.displayText }
                            val durationSec = elapsed

                            speakingVm.submitForAiAnalysis(
                                moduleType = "SPEAKING_LAB",
                                promptTitle = activePromptTitle
                            )

                            if (sendToGeminiOnSubmit && (currentFile != null || transcriptText.isNotBlank())) {
                                val res = PhoneGeminiBridge.sendStudioRecordingToGeminiApp(
                                    context = context,
                                    mediaFile = currentFile,
                                    isVideo = activeMediaIsVideo,
                                    taskType = activeTaskType,
                                    taskTitle = activePromptTitle,
                                    transcript = transcriptText,
                                    durationSeconds = durationSec
                                )
                                pendingHandoff = PhoneGeminiBridge.getPendingStudioHandoff(context)
                                geminiStatusMessage = res.userMessage
                            }
                        },
                        enabled = !isRecording,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("analyze_speech_button")
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (sendToGeminiOnSubmit) {
                                "Submit Recording (Studio + Gemini App)"
                            } else {
                                "Analyze With Local AI Coach"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val currentPath = activeMediaPath?.takeIf { it.isNotBlank() }
                                ?: recordedPath?.takeIf { it.isNotBlank() }
                            val currentFile = currentPath?.let(::File)
                            val transcriptText = liveTranscript.ifBlank { liveTranscriptionState.displayText }
                            val res = PhoneGeminiBridge.sendStudioRecordingToGeminiApp(
                                context = context,
                                mediaFile = currentFile,
                                isVideo = activeMediaIsVideo,
                                taskType = activeTaskType,
                                taskTitle = activePromptTitle,
                                transcript = transcriptText,
                                durationSeconds = elapsed
                            )
                            pendingHandoff = PhoneGeminiBridge.getPendingStudioHandoff(context)
                            geminiStatusMessage = res.userMessage
                        },
                        enabled = !isRecording && (hasMediaReady || liveTranscript.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("send_recording_to_gemini_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send Current Clip / Prompt to Gemini App")
                    }

                    if (!geminiStatusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth().testTag("studio_gemini_status_message")
                        ) {
                            Text(
                                text = geminiStatusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        val sessionCardItem: @Composable (SpeakingSession) -> Unit = { sess ->
            val idx = pastSessions.indexOf(sess)
            val priorForCompare = pastSessions.getOrNull(idx + 1)
            val playingThis = isPlaying && playingPath == sess.localMediaPath && sess.localMediaPath.isNotBlank()

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sess.promptTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (sess.localMediaPath.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (playingThis) speakingVm.audioManager.stopPlayback()
                                    else speakingVm.playSavedOrCurrentMedia(sess.localMediaPath, muted = false)
                                },
                                modifier = Modifier.defaultMinSize(
                                    minWidth = SpeakCraftSpacing.minTouchTarget,
                                    minHeight = SpeakCraftSpacing.minTouchTarget
                                )
                            ) {
                                Icon(
                                    if (playingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (playingThis) "Stop saved recording" else "Play saved recording"
                                )
                            }
                        }
                        IconButton(
                            onClick = { pendingDeleteSession = sess },
                            modifier = Modifier
                                .defaultMinSize(
                                    minWidth = SpeakCraftSpacing.minTouchTarget,
                                    minHeight = SpeakCraftSpacing.minTouchTarget
                                )
                                .testTag("delete_session_${sess.id}")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete recording", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Engine-evaluated sessions have no legacy skill scores: show only what was measured.
                        val pills = if (LabAttemptEvaluator.isEngineEvaluated(sess)) {
                            listOfNotNull(
                                "On-device feedback",
                                if (sess.wordsPerMinute > 0L) "${sess.wordsPerMinute} WPM" else "Audio only".takeIf { sess.transcript.isBlank() }
                            )
                        } else {
                            listOf(
                                "Fluency ${sess.fluencyScore}",
                                if (sess.pronunciationAssessable) "Pron ${sess.pronunciationScore}" else "Pron N/A",
                                "Articulation ${sess.articulationScore}",
                                "${sess.wordsPerMinute} WPM"
                            )
                        }
                        pills.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    val comparable = priorForCompare != null &&
                        !LabAttemptEvaluator.isEngineEvaluated(sess) && !LabAttemptEvaluator.isEngineEvaluated(priorForCompare)
                    if (comparable && priorForCompare != null) {
                        val deltaFlu = sess.fluencyScore - priorForCompare.fluencyScore
                        Surface(
                            color = (if (deltaFlu >= 0) ext.success else MaterialTheme.colorScheme.error).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Compared with prior: Fluency ${if (deltaFlu >= 0) "+$deltaFlu" else "$deltaFlu"}, WPM ${sess.wordsPerMinute - priorForCompare.wordsPerMinute}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (deltaFlu >= 0) ext.success else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        sess.overallAssessment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sess.localMediaPath.isNotBlank() || sess.promptTitle.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val file = sess.localMediaPath.takeIf { it.isNotBlank() }?.let(::File)
                                val inferredTask = if (sess.promptTitle.startsWith("Daily Video", ignoreCase = true)) {
                                    StudioTaskType.DAILY_VIDEO
                                } else {
                                    StudioTaskType.SELECT_TOPIC
                                }
                                val res = PhoneGeminiBridge.sendStudioRecordingToGeminiApp(
                                    context = context,
                                    mediaFile = file,
                                    isVideo = sess.mediaType.contains("video", ignoreCase = true) ||
                                        sess.localMediaPath.endsWith(".mp4", ignoreCase = true),
                                    taskType = inferredTask,
                                    taskTitle = sess.promptTitle,
                                    transcript = sess.transcript,
                                    durationSeconds = sess.durationSeconds.toInt()
                                )
                                pendingHandoff = PhoneGeminiBridge.getPendingStudioHandoff(context)
                                geminiStatusMessage = res.userMessage
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send Saved Clip to Gemini App")
                        }
                    }
                }
            }
        }

        val geminiFeedbackCardContent: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("studio_gemini_feedback_card")
            ) {
                Column(
                    modifier = Modifier.padding(SpeakCraftSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Gemini App Feedback (In-App Review)", style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (pendingHandoff != null) ext.infoContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = when {
                                    pendingHandoff != null -> "Awaiting Reply"
                                    geminiFeedbackHistory.isNotEmpty() -> "${geminiFeedbackHistory.size} Saved"
                                    else -> "Ready"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pendingHandoff != null) ext.info else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "After Gemini reviews your Task 1 or Task 2 recording in the phone app, tap Share -> Practice with SpeakCraft or Copy Gemini's answer and tap Paste below to keep the feedback in this studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipped = PhoneGeminiBridge.readGeminiFeedbackFromClipboard(context)
                                if (clipped != null) {
                                    geminiFeedbackDraft = clipped
                                    persistGeminiFeedback(clipped, "Gemini App Clipboard")
                                    geminiStatusMessage = "Imported feedback from Gemini clipboard into Record & Review Studio!"
                                } else {
                                    geminiStatusMessage = "Copy Gemini's reply inside the Gemini app first, or Share -> Practice with SpeakCraft."
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("studio_paste_gemini_feedback_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Paste Gemini Feedback")
                        }
                        OutlinedButton(
                            onClick = {
                                val openRes = PhoneGeminiBridge.openGeminiApp(context)
                                geminiStatusMessage = openRes.userMessage
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("studio_open_gemini_for_reply_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Gemini App")
                        }
                    }

                    OutlinedTextField(
                        value = geminiFeedbackDraft,
                        onValueChange = { geminiFeedbackDraft = it },
                        label = { Text("Gemini Coaching Feedback Received") },
                        placeholder = { Text("Feedback shared or pasted from the Gemini app appears here...") },
                        minLines = 3,
                        maxLines = 6,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().testTag("studio_gemini_feedback_input")
                    )

                    OutlinedButton(
                        onClick = {
                            persistGeminiFeedback(geminiFeedbackDraft, "Saved in Studio")
                            geminiStatusMessage = "Saved Gemini feedback in Record & Review Studio."
                        },
                        enabled = geminiFeedbackDraft.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("studio_save_gemini_feedback_button")
                    ) {
                        Text("Save Gemini Feedback to Studio")
                    }

                    if (geminiFeedbackHistory.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "Saved Gemini Reviews (${geminiFeedbackHistory.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().testTag("studio_saved_gemini_feedback_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val timeFormatter = remember {
                                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            }
                            geminiFeedbackHistory.take(5).forEachIndexed { idx, entry ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.successContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${entry.taskTypeLabel} • ${entry.mediaLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.success,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (entry.receivedAtMs > 0L) {
                                                Text(
                                                    text = timeFormatter.format(Date(entry.receivedAtMs)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = entry.taskTitle,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = entry.feedbackText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("studio_saved_feedback_text_$idx")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Record and Review Studio",
                    subtitle = "Mic pause/resume, CameraX video or Photo Picker (<19MB) & 3-Stage Audit"
                )
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                StepProgressIndicator(
                    currentStep = studioStep,
                    totalSteps = 3,
                    stepTitle = when (studioStep) {
                        1 -> "Capture Audio or Video"
                        2 -> "Run 3-Stage Self-Audit"
                        else -> "Review AI Diagnostic"
                    }
                )
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "speaking_lab")
            }

            if (cameraError != null) {
                item {
                    ErrorRecoveryCard(cameraError.orEmpty()) { cameraError = null }
                }
            }

            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            captureCardContent()
                            if (cloudSyncError != null) {
                                CloudSyncWarningBanner(
                                    message = cloudSyncError.orEmpty(),
                                    onRetrySync = { speakingVm.retryCloudSync() }
                                )
                            }
                            if (activeModule == "SPEAKING_LAB") {
                                when (val state = analysisState) {
                                    is UiState.Loading -> ShimmerSkeletonCard(180)
                                    is UiState.Error -> {
                                        val rep = evaluationReport
                                        if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                                            CoachEvaluationStatusCard(
                                                report = rep,
                                                onRecordAgain = {
                                                    speakingVm.recordAgain()
                                                    speakingVm.startRecording(null)
                                                },
                                                onCancel = { speakingVm.cancelEvaluation() }
                                            )
                                        } else {
                                            ErrorRecoveryCard(
                                                message = state.message,
                                                onRetry = {
                                                    speakingVm.submitForAiAnalysis(
                                                        moduleType = "SPEAKING_LAB",
                                                        promptTitle = activePromptTitle
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    is UiState.Success -> FeedbackReportCard(
                                        session = state.data,
                                        previousSession = previousSession,
                                        evaluationReport = evaluationReport
                                    )
                                    null -> Unit
                                }
                            }
                            geminiFeedbackCardContent()
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SectionHeader("Past Recorded Sessions", "Play saved audio, compare progress & manage clips")
                            if (pastSessions.isEmpty()) {
                                EmptyStateCard(
                                    title = "No saved recordings yet",
                                    message = "Record your first 30–60 second response to unlock AI speech diagnostics and progress tracking.",
                                    icon = Icons.Default.Mic
                                )
                            } else {
                                pastSessions.take(10).forEach { sess ->
                                    sessionCardItem(sess)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    captureCardContent()
                }

                if (cloudSyncError != null) {
                    item {
                        CloudSyncWarningBanner(
                            message = cloudSyncError.orEmpty(),
                            onRetrySync = { speakingVm.retryCloudSync() }
                        )
                    }
                }

                if (activeModule == "SPEAKING_LAB") {
                    item {
                        when (val state = analysisState) {
                            is UiState.Loading -> ShimmerSkeletonCard(180)
                            is UiState.Error -> {
                                val rep = evaluationReport
                                if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                                    CoachEvaluationStatusCard(
                                        report = rep,
                                        onRecordAgain = {
                                            speakingVm.recordAgain()
                                            speakingVm.startRecording(null)
                                        },
                                        onCancel = { speakingVm.cancelEvaluation() }
                                    )
                                } else {
                                    ErrorRecoveryCard(
                                        message = state.message,
                                        onRetry = {
                                            speakingVm.submitForAiAnalysis(
                                                moduleType = "SPEAKING_LAB",
                                                promptTitle = activePromptTitle
                                            )
                                        }
                                    )
                                }
                            }
                            is UiState.Success -> FeedbackReportCard(
                                session = state.data,
                                previousSession = previousSession,
                                evaluationReport = evaluationReport
                            )
                            null -> Unit
                        }
                    }
                }

                item {
                    geminiFeedbackCardContent()
                }

                item {
                    SectionHeader("Past Recorded Sessions", "Play saved audio, compare progress & manage clips")
                }

                if (pastSessions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No saved recordings yet",
                            message = "Record your first 30–60 second response above to unlock AI speech diagnostics and progress tracking.",
                            icon = Icons.Default.Mic
                        )
                    }
                } else {
                    items(pastSessions.take(10), key = { it.id.ifBlank { it.promptTitle + it.durationSeconds } }) { sess ->
                        sessionCardItem(sess)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengesScreen(
    speakingVm: SpeakingAndLabsViewModel,
    sharedChallenges: List<LearningChallenge>,
    pastSessions: List<SpeakingSession>,
    onOpenAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    val challenges = remember(sharedChallenges) {
        val mergedShared = sharedChallenges.filter { it.active }.map { ch ->
            SpeakingChallengeItem(
                id = ch.id,
                title = ch.title,
                challengeType = ch.challengeType,
                durationSeconds = ch.durationSeconds.toInt(),
                frameworkHint = ch.frameworkHint,
                prompt = ch.prompt,
                sourceReference = ch.sourceReference
            )
        }
        (CurriculumCatalog.speakingChallenges + mergedShared).distinctBy { it.id }
    }

    var filterType by rememberSaveable { mutableStateOf("ALL") }
    val filteredChallenges = remember(challenges, filterType) {
        if (filterType == "ALL") challenges else challenges.filter { it.challengeType == filterType }
    }
    var selectedChallenge by rememberSaveable(
        challenges.firstOrNull()?.id,
        stateSaver = Saver(
            save = { it.id },
            restore = { id -> challenges.firstOrNull { it.id == id } ?: challenges.first() }
        )
    ) { mutableStateOf(challenges.first()) }
    val isRecording by speakingVm.audioManager.isRecording.collectAsStateWithLifecycle()
    val isPaused by speakingVm.audioManager.isPaused.collectAsStateWithLifecycle()
    val amplitude by speakingVm.audioManager.amplitude.collectAsStateWithLifecycle()
    val countdown by speakingVm.challengeRemainingSeconds.collectAsStateWithLifecycle()
    val recordedPath by speakingVm.audioManager.recordedFilePath.collectAsStateWithLifecycle()
    val analysisState by speakingVm.analysisState.collectAsStateWithLifecycle()
    val evaluationReport by speakingVm.evaluationReport.collectAsStateWithLifecycle()
    val activeModule by speakingVm.activeAnalysisModule.collectAsStateWithLifecycle()
    val cloudSyncError by speakingVm.cloudSyncError.collectAsStateWithLifecycle()
    val aiSettings by speakingVm.aiSettings.collectAsStateWithLifecycle()
    val liveTranscriptionState by speakingVm.liveTranscriptionState.collectAsStateWithLifecycle()
    val liveTranscript by speakingVm.liveTranscript.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) speakingVm.startRecording(selectedChallenge.durationSeconds)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        val activeChallengeCard: @Composable () -> Unit = {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(SpeakCraftSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("${selectedChallenge.title} (${selectedChallenge.durationSeconds}s)", style = MaterialTheme.typography.titleLarge)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ext.warningContainer
                        ) {
                            Text(
                                selectedChallenge.challengeType,
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.warning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(selectedChallenge.prompt, style = MaterialTheme.typography.bodyLarge)

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Framework Tip: ${selectedChallenge.frameworkHint}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Source: ${selectedChallenge.sourceReference}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LiveWaveformVisualizer(amplitude = amplitude, isRecording = isRecording, isPaused = isPaused)
                    LiveTranscriptPanel(
                        state = liveTranscriptionState,
                        liveTranscriptionEnabled = aiSettings.liveTranscriptionEnabled,
                        editedTranscript = liveTranscript,
                        editable = false,
                        onRetry = { speakingVm.retryLiveTranscription() },
                        onOpenAiSettings = onOpenAiSettings
                    )
                    if (countdown != null) {
                        val total = selectedChallenge.durationSeconds.coerceAtLeast(1)
                        val fraction = (countdown!!.toFloat() / total).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Time Left: ${countdown}s", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (countdown!! <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (isRecording) {
                                    speakingVm.stopRecording()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (granted) speakingVm.startRecording(selectedChallenge.durationSeconds)
                                    else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("challenge_record_button")
                        ) {
                            Text(if (isRecording) "Stop Early" else "Start ${selectedChallenge.durationSeconds}s Timer")
                        }
                        if (isRecording) {
                            OutlinedButton(
                                onClick = { if (isPaused) speakingVm.resumeRecording() else speakingVm.pauseRecording() },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            ) { Text(if (isPaused) "Resume" else "Pause") }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    speakingVm.submitForAiAnalysis(
                                        moduleType = "DAILY_CHALLENGE",
                                        promptTitle = selectedChallenge.title,
                                        practiceTarget = LabPracticeTarget.fromPrompt(
                                            selectedChallenge.title,
                                            selectedChallenge.frameworkHint,
                                            selectedChallenge.sourceReference
                                        )
                                    )
                                },
                                enabled = !isRecording,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("challenge_submit_button")
                            ) { Text("Submit AI Check") }
                        }
                    }
                }
            }
        }

        val challengeRowItem: @Composable (SpeakingChallengeItem) -> Unit = { chal ->
            val selected = chal.id == selectedChallenge.id
            Card(
                onClick = {
                    selectedChallenge = chal
                    speakingVm.clearAnalysis()
                },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${chal.challengeType} • ${chal.title} (${chal.durationSeconds}s)", style = MaterialTheme.typography.titleMedium)
                    Text(chal.prompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = containerMaxWidth),
            contentPadding = PaddingValues(SpeakCraftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
        ) {
            item {
                SectionHeader("Communication Challenges", "Daily & weekly timed speaking drills")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FeatureGuideStrip(featureKey = "challenges")
                Spacer(Modifier.height(SpeakCraftSpacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                ) {
                    listOf("ALL", "DAILY", "WEEKLY").forEach { type ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = type },
                            label = { Text(type) },
                            modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        )
                    }
                }
            }

            if (tier.isExpandedTablet) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.05f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                        ) {
                            activeChallengeCard()
                            if (cloudSyncError != null) {
                                CloudSyncWarningBanner(
                                    message = cloudSyncError.orEmpty(),
                                    onRetrySync = { speakingVm.retryCloudSync() }
                                )
                            }
                            if (activeModule == "DAILY_CHALLENGE") {
                                when (val state = analysisState) {
                                    is UiState.Loading -> ShimmerSkeletonCard(150)
                                    is UiState.Error -> {
                                        val rep = evaluationReport
                                        if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                                            CoachEvaluationStatusCard(
                                                report = rep,
                                                onRecordAgain = {
                                                    speakingVm.recordAgain()
                                                    speakingVm.startRecording(selectedChallenge.durationSeconds)
                                                },
                                                onCancel = { speakingVm.cancelEvaluation() }
                                            )
                                        } else {
                                            ErrorRecoveryCard(state.message)
                                        }
                                    }
                                    is UiState.Success -> FeedbackReportCard(state.data, pastSessions.firstOrNull(), evaluationReport)
                                    null -> Unit
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(0.95f),
                            verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.sm)
                        ) {
                            filteredChallenges.forEach { chal ->
                                challengeRowItem(chal)
                            }
                        }
                    }
                }
            } else {
                item { activeChallengeCard() }

                if (cloudSyncError != null) {
                    item {
                        CloudSyncWarningBanner(
                            message = cloudSyncError.orEmpty(),
                            onRetrySync = { speakingVm.retryCloudSync() }
                        )
                    }
                }

                if (activeModule == "DAILY_CHALLENGE") {
                    item {
                        when (val state = analysisState) {
                            is UiState.Loading -> ShimmerSkeletonCard(150)
                            is UiState.Error -> {
                                val rep = evaluationReport
                                if (rep != null && rep.evaluationState != CoachEvaluationState.COMPLETED) {
                                    CoachEvaluationStatusCard(
                                        report = rep,
                                        onRecordAgain = {
                                            speakingVm.recordAgain()
                                            speakingVm.startRecording(selectedChallenge.durationSeconds)
                                        },
                                        onCancel = { speakingVm.cancelEvaluation() }
                                    )
                                } else {
                                    ErrorRecoveryCard(state.message)
                                }
                            }
                            is UiState.Success -> FeedbackReportCard(state.data, pastSessions.firstOrNull(), evaluationReport)
                            null -> Unit
                        }
                    }
                }

                items(filteredChallenges, key = { it.id }) { chal ->
                    challengeRowItem(chal)
                }
            }
        }
    }
}
