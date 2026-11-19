package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.activity.ActivityDefinition
import com.example.data.activity.ActivityEvaluationResult
import com.example.data.activity.DialogueTurn
import com.example.data.activity.InteractionFamily
import com.example.data.media.InAppCameraVideoDialog
import com.example.data.media.TtsAndSpeechHelper
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseTemplateInteractionPanel(
    activity: ActivityDefinition,
    textResponse: String,
    recordedAudioPath: String,
    recordedVideoPath: String,
    isAudioRecording: Boolean,
    recordingAmplitude: Float,
    completedChecklistItems: Set<String>,
    dialogueTurns: List<DialogueTurn>,
    dialogueReplyLoading: Boolean,
    ttsHelper: TtsAndSpeechHelper?,
    onTextChange: (String) -> Unit,
    onToggleChecklist: (String) -> Unit,
    onStartAudioRecording: () -> Unit,
    onStopAudioRecording: () -> Unit,
    onPlayAudioRecording: () -> Unit,
    onVideoCaptured: (File, Int) -> Unit,
    onSendDialogueMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val ext = MaterialTheme.speakCraftColors
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionMessage = null
            onStartAudioRecording()
        } else {
            permissionMessage = "Microphone permission was denied. You can still type your spoken response below."
        }
    }

    val cameraPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val camOk = grants[Manifest.permission.CAMERA] == true
        val micOk = grants[Manifest.permission.RECORD_AUDIO] == true
        if (camOk && micOk) {
            permissionMessage = null
            showCameraDialog = true
        } else {
            permissionMessage = "Camera & Microphone permissions are needed for video capture. You can still complete the self-review checklist & reflection below."
        }
    }

    if (showCameraDialog) {
        InAppCameraVideoDialog(
            onDismiss = { showCameraDialog = false },
            onVideoSaved = { _, file, sec ->
                showCameraDialog = false
                onVideoCaptured(file, sec)
            },
            onError = { err ->
                showCameraDialog = false
                permissionMessage = err
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("template_family_${activity.exerciseType.interactionFamily.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Core Exercise Prompt Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Exercise Prompt • ${activity.exerciseType.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = activity.interactionConfiguration.promptText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activity.interactionConfiguration.targetWordOrPhrase.isNotBlank()) {
                    AssistChip(
                        onClick = {
                            ttsHelper?.speak(activity.interactionConfiguration.targetWordOrPhrase, 0.92f)
                        },
                        label = { Text("Focus cue: ${activity.interactionConfiguration.targetWordOrPhrase}") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen to cue", modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }

        // Listening / Reference Audio Player (for LISTENING_AUDIO or any activity with listeningScript)
        if (activity.interactionConfiguration.listeningScript.isNotBlank()) {
            ListeningAudioCard(
                script = activity.interactionConfiguration.listeningScript,
                hideScriptInitially = activity.exerciseType.interactionFamily == InteractionFamily.LISTENING_AUDIO,
                ttsHelper = ttsHelper
            )
        }

        // Warmup cues pills (Breathing / Vocal / Articulation warm-ups)
        if (activity.interactionConfiguration.warmupCues.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activity.interactionConfiguration.warmupCues.forEachIndexed { index, cue ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.pastelMintBg,
                        border = BorderStroke(1.dp, ext.pastelMintInk.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${index + 1}. $cue",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.pastelMintInk,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Audio Recording Controls
        if (activity.interactionConfiguration.supportsAudioRecording) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Studio Recorder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (recordedAudioPath.isNotBlank()) {
                            Surface(color = ext.successContainer, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "Audio Captured",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.success,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (isAudioRecording) {
                        LinearProgressIndicator(
                            progress = { recordingAmplitude.coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isAudioRecording) {
                                    onStopAudioRecording()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) onStartAudioRecording()
                                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAudioRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("activity_audio_record_button")
                        ) {
                            Icon(if (isAudioRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isAudioRecording) "Stop Recording" else "Record Audio")
                        }
                        if (recordedAudioPath.isNotBlank() && !isAudioRecording) {
                            OutlinedButton(
                                onClick = onPlayAudioRecording,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("activity_audio_play_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play audio")
                                Spacer(Modifier.width(4.dp))
                                Text("Play")
                            }
                        }
                    }
                }
            }
        }

        // Video Capture Controls (Video Recording, Body Language, Presentation)
        if (activity.interactionConfiguration.supportsVideoRecording) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CameraX Video & Presence Audit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (recordedVideoPath.isNotBlank()) {
                            Surface(color = ext.successContainer, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "Video Attached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.success,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val camOk = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            val micOk = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (camOk && micOk) {
                                showCameraDialog = true
                            } else {
                                cameraPermissionsLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("activity_video_record_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (recordedVideoPath.isBlank()) "Open Camera Video Studio" else "Re-record Video Clip")
                    }
                }
            }
        }

        if (!permissionMessage.isNullOrBlank()) {
            Surface(
                color = ext.warningContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ext.warning)
                    Text(permissionMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Interactive Multi-Turn Dialogue Simulator (Conversation, Roleplay, Interview)
        if (activity.exerciseType.interactionFamily == InteractionFamily.INTERACTIVE_DIALOGUE) {
            DialogueSimulatorCard(
                partnerRole = activity.interactionConfiguration.dialoguePartnerRole.ifBlank { "Practice Partner" },
                turns = dialogueTurns,
                replyLoading = dialogueReplyLoading,
                onSendTurn = onSendDialogueMessage
            )
        }

        // Guided Checklist (Warmups, Body-Language Self-Review, Real-World Tasks)
        if (activity.interactionConfiguration.checklistItems.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Guided Practice Checklist (${completedChecklistItems.size}/${activity.interactionConfiguration.checklistItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    activity.interactionConfiguration.checklistItems.forEachIndexed { idx, item ->
                        val checked = item in completedChecklistItems
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleChecklist(item) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("activity_checklist_item_$idx"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (checked) "Completed" else "Not completed",
                                tint = if (checked) ext.success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Universal Response / Transcript / Reflection Composer
        if (activity.interactionConfiguration.supportsTextInput) {
            val wordCount = remember(textResponse) {
                textResponse.trim().split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) }
            }
            val labelText = when (activity.exerciseType.interactionFamily) {
                InteractionFamily.INTERACTIVE_DIALOGUE -> "Additional reflection or key takeaway (optional)"
                InteractionFamily.VIDEO_PRESENCE -> "Self-review notes or spoken summary"
                InteractionFamily.LISTENING_AUDIO -> "Your comprehension / reflective response"
                InteractionFamily.LANGUAGE_BUILDER -> "Write your sentences / response here"
                InteractionFamily.REAL_WORLD_REFLECTION -> "Log your real-world reflection & observations"
                else -> "Spoken transcript or typed response (for detailed wording feedback)"
            }
            OutlinedTextField(
                value = textResponse,
                onValueChange = onTextChange,
                label = { Text(labelText) },
                supportingText = {
                    Text("$wordCount words • Target ${activity.interactionConfiguration.targetWordCountMin}+ words")
                },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_response_input")
            )
        }
    }
}

@Composable
private fun ListeningAudioCard(
    script: String,
    hideScriptInitially: Boolean,
    ttsHelper: TtsAndSpeechHelper?
) {
    var rate by remember { mutableFloatStateOf(1.0f) }
    var revealScript by remember { mutableStateOf(!hideScriptInitially) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Audio Reference Player", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { ttsHelper?.speak(script, rate) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("activity_tts_play_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play Audio")
                }
                listOf(0.85f to "0.85x", 1.0f to "1.0x", 1.15f to "1.15x").forEach { (speed, label) ->
                    FilterChip(
                        selected = rate == speed,
                        onClick = { rate = speed },
                        label = { Text(label) },
                        modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    )
                }
            }
            if (hideScriptInitially) {
                OutlinedButton(
                    onClick = { revealScript = !revealScript },
                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                ) {
                    Text(if (revealScript) "Hide Transcript" else "Show Transcript After Listening")
                }
            }
            if (revealScript) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = script,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogueSimulatorCard(
    partnerRole: String,
    turns: List<DialogueTurn>,
    replyLoading: Boolean,
    onSendTurn: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Partner: $partnerRole", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${turns.count { it.role == "user" }} turn(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            turns.forEach { turn ->
                val isUser = turn.role == "user"
                Surface(
                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isUser) "You" else partnerRole,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (replyLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Partner is responding...", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Type your next spoken reply...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("activity_dialogue_input")
                )
                Button(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSendTurn(draft)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank() && !replyLoading,
                    modifier = Modifier
                        .defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("activity_dialogue_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply")
                }
            }
        }
    }
}

@Composable
fun SeparateConfidenceSliderCard(
    confidenceRating: Int,
    onRatingChange: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Self-Assessed Confidence (Separate from Score)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("$confidenceRating / 100", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Tracked independently so you can compare how confident you felt vs objective speaking scores.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = confidenceRating.toFloat(),
                onValueChange = { onRatingChange(it.toInt()) },
                valueRange = 1f..100f,
                modifier = Modifier.testTag("activity_confidence_slider")
            )
        }
    }
}

@Composable
fun ActivityStructuredFeedbackCard(
    evaluation: ActivityEvaluationResult
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_evaluation_feedback_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Objective Score vs Separate Confidence Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (evaluation.passedCriteria) "Exercise Completed" else "Keep Practicing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (evaluation.passedCriteria) ext.success else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${evaluation.evaluatorName} • ${if (evaluation.aiEvaluated) "Gemini AI Coaching" else "Local Rubric Check"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${evaluation.overallObjectiveScore}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("Objective", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Metrics chips (WPM, Word count, Fillers, Separate Self-Confidence)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniPill("Words", "${evaluation.wordCount}", Modifier.weight(1f))
                MetricMiniPill("Fillers", "${evaluation.fillerCount}", Modifier.weight(1f))
                if (evaluation.wordsPerMinute > 0) {
                    MetricMiniPill("Pace", "${evaluation.wordsPerMinute} WPM", Modifier.weight(1f))
                }
                evaluation.confidenceSelfRating?.let { conf ->
                    MetricMiniPill("Self-Conf", "$conf%", Modifier.weight(1f))
                }
            }

            if (evaluation.honestyNotice.isNotBlank()) {
                Surface(
                    color = ext.infoContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ext.info, modifier = Modifier.size(18.dp))
                        Text(evaluation.honestyNotice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Rubric Dimension Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Rubric Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                evaluation.dimensionScores.forEach { dim ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dim.label, style = MaterialTheme.typography.labelMedium)
                            Text("${dim.score}/100", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (dim.score / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        if (dim.note.isNotBlank()) {
                            Text(dim.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // 6 Mandatory Structured Feedback Sections
            FeedbackBulletBlock("1. What You Did Well", evaluation.whatWentWell, ext.success)
            FeedbackBulletBlock("2. Specific Mistakes & Gaps", evaluation.specificMistakes, ext.warning)
            FeedbackBulletBlock("3. Corrected Examples", evaluation.correctedExamples, MaterialTheme.colorScheme.primary)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("4. Coaching Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(evaluation.explanation, style = MaterialTheme.typography.bodyMedium)
            }

            FeedbackBulletBlock("5. Improvement Suggestions", evaluation.improvementSuggestions, ext.info)

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("6. Next Practice Task", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(evaluation.nextPracticeTask, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeedbackBulletBlock(
    title: String,
    items: List<String>,
    accent: androidx.compose.ui.graphics.Color
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = accent)
        items.forEach { item ->
            Text("• $item", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
