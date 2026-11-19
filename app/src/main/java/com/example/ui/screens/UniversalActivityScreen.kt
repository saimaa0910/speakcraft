package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.activity.ActivityDefinition
import com.example.data.media.TtsAndSpeechHelper
import com.example.ui.components.ActivityStructuredFeedbackCard
import com.example.ui.components.ExerciseTemplateInteractionPanel
import com.example.ui.components.SeparateConfidenceSliderCard
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import com.example.ui.viewmodel.ActivityEngineViewModel
import com.example.ui.viewmodel.ActivitySessionStage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen(
    activityId: String,
    vm: ActivityEngineViewModel,
    ttsHelper: TtsAndSpeechHelper?,
    userLevel: String = "Intermediate",
    onOpenNextActivity: (String) -> Unit,
    onBackToCatalogue: () -> Unit
) {
    LaunchedEffect(activityId) {
        vm.loadActivity(activityId)
    }

    val uiState by vm.activeState.collectAsStateWithLifecycle()
    val savedList by vm.savedActivities.collectAsStateWithLifecycle()
    val isAudioRecording by (vm.audioRecorder?.isRecording?.collectAsStateWithLifecycle() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) })
    val amplitude by (vm.audioRecorder?.amplitude?.collectAsStateWithLifecycle() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) })

    val ext = MaterialTheme.speakCraftColors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("universal_activity_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val tier = SpeakCraftSpacing.tierForWidth(maxWidth)
        val containerMaxWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)

        when (val stage = uiState.stage) {
            is ActivitySessionStage.Invalid -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .padding(24.dp)
                        .testTag("invalid_activity_card"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Invalid activity",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Activity Unavailable",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "We could not safely render '${stage.activityId}'. Validation prevented a crash:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    stage.errors.forEach { err ->
                        Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onBackToCatalogue,
                        modifier = Modifier
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .testTag("invalid_activity_back_button")
                    ) {
                        Text("Browse All 150 Activities")
                    }
                }
            }

            else -> {
                val activity = uiState.activity ?: return@BoxWithConstraints
                val isSaved = savedList.any { it.activityId == activity.activityId }
                val targetSec = (activity.duration * 60).coerceAtLeast(60)
                val progressFraction = when (stage) {
                    is ActivitySessionStage.Ready -> 0.12f
                    is ActivitySessionStage.Active -> (0.2f + (uiState.elapsedSeconds.toFloat() / targetSec) * 0.65f).coerceIn(0.2f, 0.9f)
                    is ActivitySessionStage.Evaluating -> 0.95f
                    is ActivitySessionStage.Completed -> 1f
                    is ActivitySessionStage.Invalid -> 0f
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = containerMaxWidth),
                    contentPadding = PaddingValues(SpeakCraftSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(SpeakCraftSpacing.md)
                ) {
                    // 1. Header Card: Title, Module, Skill, Difficulty, Duration, Template, Save Toggle
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.module?.title ?: "SpeakCraft Module",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = activity.title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("activity_title_text")
                                        )
                                    }
                                    IconButton(
                                        onClick = { vm.toggleSavedActivity(activity) },
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = SpeakCraftSpacing.minTouchTarget, minHeight = SpeakCraftSpacing.minTouchTarget)
                                            .testTag("activity_bookmark_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (isSaved) "Remove bookmark" else "Save activity",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = activity.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetaBadge(activity.skillCategory.label, ext.pastelLavenderBg, ext.pastelLavenderInk)
                                    MetaBadge(activity.difficulty.label, ext.pastelSkyBg, ext.pastelSkyInk)
                                    MetaBadge("${activity.duration} min", ext.pastelPeachBg, ext.pastelPeachInk)
                                    MetaBadge(activity.exerciseType.displayName, ext.pastelMintBg, ext.pastelMintInk)
                                }

                                // Progress & Live Timer Strip
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (stage) {
                                                is ActivitySessionStage.Ready -> "Step 1 of 3 • Review Objective"
                                                is ActivitySessionStage.Active -> "Step 2 of 3 • Practice & Respond"
                                                is ActivitySessionStage.Evaluating -> "Step 3 of 3 • Evaluating..."
                                                is ActivitySessionStage.Completed -> "Completed • Review Feedback"
                                                is ActivitySessionStage.Invalid -> ""
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "%02d:%02d".format(uiState.elapsedSeconds / 60, uiState.elapsedSeconds % 60),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.testTag("activity_timer_text")
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }

                    // 2. Learning Objective, Instructions & Concrete Examples
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Learning Objective", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(activity.learningObjective, style = MaterialTheme.typography.bodyMedium)

                                Text("Step-by-Step Instructions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                activity.instructions.forEachIndexed { idx, step ->
                                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                                }

                                Text("Example Model", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                activity.examples.forEach { ex ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "\"$ex\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Stage-specific Action / Exercise Renderer / Evaluation / Result
                    when (stage) {
                        is ActivitySessionStage.Ready -> {
                            item {
                                Button(
                                    onClick = { vm.startActivity() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 54.dp)
                                        .testTag("activity_start_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start Exercise", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        is ActivitySessionStage.Active -> {
                            item {
                                ExerciseTemplateInteractionPanel(
                                    activity = activity,
                                    textResponse = uiState.textResponse,
                                    recordedAudioPath = uiState.recordedAudioPath,
                                    recordedVideoPath = uiState.recordedVideoPath,
                                    isAudioRecording = isAudioRecording,
                                    recordingAmplitude = amplitude,
                                    completedChecklistItems = uiState.completedChecklistItems,
                                    dialogueTurns = uiState.dialogueTurns,
                                    dialogueReplyLoading = uiState.dialogueReplyLoading,
                                    ttsHelper = ttsHelper,
                                    onTextChange = { vm.updateTextResponse(it) },
                                    onToggleChecklist = { vm.toggleChecklistItem(it) },
                                    onStartAudioRecording = { vm.startAudioRecording() },
                                    onStopAudioRecording = { vm.stopAudioRecording() },
                                    onPlayAudioRecording = { vm.audioRecorder?.startPlayback() },
                                    onVideoCaptured = { file, sec -> vm.attachRecordedVideo(file, sec) },
                                    onSendDialogueMessage = { msg -> vm.sendDialogueMessage(msg, userLevel) }
                                )
                            }

                            if (activity.interactionConfiguration.allowConfidenceSelfRating) {
                                item {
                                    SeparateConfidenceSliderCard(
                                        confidenceRating = uiState.confidenceSelfRating,
                                        onRatingChange = { vm.updateConfidenceSelfRating(it) }
                                    )
                                }
                            }

                            if (!uiState.validationError.isNullOrBlank()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.validationError.orEmpty(),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = { vm.submitActiveResponse() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 54.dp)
                                        .testTag("activity_submit_button")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Submit for Evaluation", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        is ActivitySessionStage.Evaluating -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            "Running ${activity.evaluationConfiguration.strategy.evaluatorName}...",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Analyzing your response against ${activity.title}'s scoring rubric.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        is ActivitySessionStage.Completed -> {
                            item {
                                ActivityStructuredFeedbackCard(evaluation = stage.outcome.evaluation)
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { vm.retryCurrentActivity() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                            .testTag("activity_retry_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                    val firstNext = stage.outcome.recommendedNextActivities.firstOrNull()
                                    if (firstNext != null) {
                                        Button(
                                            onClick = { onOpenNextActivity(firstNext.activityId) },
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                                .testTag("activity_continue_next_button")
                                        ) {
                                            Text("Continue Next", maxLines = 1)
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                        }
                                    }
                                }
                            }

                            if (stage.outcome.recommendedNextActivities.isNotEmpty()) {
                                item {
                                    RecommendedNextActivitiesCard(
                                        recommendations = stage.outcome.recommendedNextActivities,
                                        onSelect = onOpenNextActivity
                                    )
                                }
                            }
                        }

                        is ActivitySessionStage.Invalid -> Unit
                    }

                    // Attribution footer
                    item {
                        Text(
                            text = "${activity.sourceAttribution} • v${activity.contentVersion} • ID: ${activity.activityId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaBadge(
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color
) {
    Surface(color = bg, shape = RoundedCornerShape(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun RecommendedNextActivitiesCard(
    recommendations: List<ActivityDefinition>,
    onSelect: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Recommended Next Activities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            recommendations.forEach { rec ->
                OutlinedButton(
                    onClick = { onSelect(rec.activityId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("recommended_next_${rec.activityId}"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${rec.skillCategory.label} • ${rec.difficulty.label} • ${rec.duration} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open ${rec.title}")
                    }
                }
            }
        }
    }
}
