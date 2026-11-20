package com.example.ui.components

import android.media.MediaPlayer
import android.provider.Settings
import android.widget.VideoView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ai.local.audio.LiveTranscriptionState
import com.example.data.ai.local.audio.LiveTranscriptionStatus
import com.example.data.evaluation.CoachEvaluationReport
import com.example.data.evaluation.DimensionAvailability
import com.example.data.model.SpeakingSession
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import java.io.File

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        }.getOrDefault(false)
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepTitle: String,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val targetFraction = (currentStep.toFloat() / totalSteps.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (reducedMotion) tween(0) else tween(260),
        label = "step_progress"
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step $currentStep of $totalSteps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stepTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    delta: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.14f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
            if (!delta.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = delta,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveWaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val ext = MaterialTheme.speakCraftColors
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isRecording, isPaused, reducedMotion) {
        if (isRecording && !isPaused && !reducedMotion) {
            var lastMs = 0L
            while (true) {
                withFrameMillis { frameMs ->
                    if (lastMs != 0L) {
                        phase += ((frameMs - lastMs) / 1000f) * 5.6f
                    }
                    lastMs = frameMs
                }
            }
        }
    }

    val barColor = when {
        isRecording && isPaused -> MaterialTheme.colorScheme.secondary
        isRecording -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val glowColor = if (isRecording && !isPaused) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
    val a11yDescription = when {
        isRecording && isPaused -> "Audio recorder waveform paused"
        isRecording -> "Live microphone waveform active"
        else -> "Audio recorder waveform idle"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                )
            )
            .semantics { contentDescription = a11yDescription }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val centerY = size.height / 2f
            if (isRecording && !isPaused) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(size.width / 2f, centerY),
                        radius = size.width * 0.48f
                    ),
                    radius = size.width * 0.48f,
                    center = Offset(size.width / 2f, centerY)
                )
            }
            drawLine(
                color = barColor.copy(alpha = 0.22f),
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 2f
            )
            val barCount = 34
            val spacing = size.width / barCount
            val barWidth = (spacing * 0.50f).coerceAtLeast(4f)
            for (i in 0 until barCount) {
                val edgeEnvelope = 1f - (kotlin.math.abs((i - barCount / 2f) / (barCount / 2f)) * 0.35f)
                val waveShape = kotlin.math.sin((i * 0.45f) + phase)
                val ampFactor = when {
                    isRecording && !isPaused -> (((amplitude * 0.88f) + (kotlin.math.abs(waveShape) * 0.38f)) * edgeEnvelope).coerceIn(0.14f, 1f)
                    isRecording && isPaused -> (0.24f * edgeEnvelope).coerceAtLeast(0.14f)
                    else -> (0.14f + kotlin.math.abs(kotlin.math.sin(i * 0.5f)) * 0.14f) * edgeEnvelope
                }
                val barHeight = (size.height * ampFactor).coerceAtLeast(8f)
                val x = i * spacing + (spacing - barWidth) / 2f
                drawRoundRect(
                    color = if (i % 3 == 0 && isRecording && !isPaused) secondaryColor else barColor,
                    topLeft = Offset(x, centerY - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            shape = RoundedCornerShape(10.dp),
            color = when {
                isRecording && !isPaused -> ext.recordingPulse.copy(alpha = 0.22f)
                isRecording && isPaused -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
            }
        ) {
            Text(
                text = when {
                    isRecording && isPaused -> "PAUSED"
                    isRecording -> "LIVE"
                    else -> "STUDIO"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isRecording && !isPaused -> MaterialTheme.colorScheme.error
                    isRecording && isPaused -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ScoreTrendSparkline(
    scores: List<Int>,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    modifier: Modifier = Modifier
) {
    val safeScores = if (scores.size >= 2) scores else listOf(scores.firstOrNull() ?: 60, scores.firstOrNull() ?: 62)
    val pointCenterColor = MaterialTheme.speakCraftColors.sparklinePointCenter
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val summaryText = "Score trend across ${scores.size.coerceAtLeast(1)} sessions, latest ${safeScores.last()} out of 100"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .semantics { contentDescription = summaryText }
    ) {
        val minVal = (safeScores.minOrNull() ?: 0).coerceAtMost(40).toFloat()
        val maxVal = (safeScores.maxOrNull() ?: 100).coerceAtLeast(95).toFloat()
        val range = (maxVal - minVal).coerceAtLeast(10f)
        val topPad = 14f
        val bottomPad = 12f
        val usableHeight = (size.height - topPad - bottomPad).coerceAtLeast(24f)
        val horizontalPad = 8f
        val usableWidth = (size.width - horizontalPad * 2f).coerceAtLeast(20f)
        val stepX = usableWidth / (safeScores.size - 1).coerceAtLeast(1)

        // Subtle editorial baseline guides
        listOf(0.25f, 0.6f, 0.9f).forEach { frac ->
            val gy = topPad + usableHeight * frac
            drawLine(
                color = guideColor,
                start = Offset(horizontalPad, gy),
                end = Offset(size.width - horizontalPad, gy),
                strokeWidth = 1.5f
            )
        }

        val points = safeScores.mapIndexed { idx, score ->
            val x = horizontalPad + idx * stepX
            val normalizedY = ((score - minVal) / range).coerceIn(0f, 1f)
            val y = size.height - bottomPad - (normalizedY * usableHeight)
            Offset(x, y)
        }

        val linePath = Path().apply {
            points.forEachIndexed { idx, pt ->
                if (idx == 0) {
                    moveTo(pt.x, pt.y)
                } else {
                    val prev = points[idx - 1]
                    val midX = (prev.x + pt.x) / 2f
                    cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                }
            }
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height - bottomPad)
            lineTo(points.first().x, size.height - bottomPad)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.28f), accentColor.copy(alpha = 0.0f))
            )
        )
        drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        points.forEachIndexed { idx, pt ->
            val isLatest = idx == points.lastIndex
            if (isLatest) {
                drawCircle(color = accentColor.copy(alpha = 0.22f), radius = 14f, center = pt)
            }
            drawCircle(color = accentColor, radius = if (isLatest) 7.5f else 5.5f, center = pt)
            drawCircle(color = pointCenterColor, radius = if (isLatest) 3.5f else 2.5f, center = pt)
        }
    }
}

@Composable
fun VideoReviewPlayerCard(
    videoPath: String,
    auditStage: String,
    modifier: Modifier = Modifier
) {
    val fileExists = remember(videoPath) { File(videoPath).let { it.exists() && it.length() > 0L } }
    val isStage1Muted = auditStage.startsWith("Stage 1")
    val isStage2EyesClosed = auditStage.startsWith("Stage 2")
    var userMutedOverride by remember(auditStage) { mutableStateOf(isStage1Muted) }
    var isPlaying by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(userMutedOverride, mediaPlayerRef) {
        val vol = if (userMutedOverride) 0f else 1f
        runCatching { mediaPlayerRef?.setVolume(vol, vol) }
    }

    DisposableEffect(videoPath) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
            mediaPlayerRef = null
            videoViewRef = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("video_review_player_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isStage1Muted -> "Stage 1: Muted Visual Pass (Posture & Gestures)"
                        isStage2EyesClosed -> "Stage 2: Eyes-Closed Vocal Pass (Audio Only)"
                        else -> "Stage 3: Full Audio + Visual Review"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (!fileExists) {
                Text(
                    "Video file is no longer cached locally on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.speakCraftColors.cameraBackdrop),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoPath(videoPath)
                                setOnPreparedListener { mp ->
                                    mediaPlayerRef = mp
                                    val vol = if (userMutedOverride) 0f else 1f
                                    mp.setVolume(vol, vol)
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                                videoViewRef = this
                            }
                        },
                        update = { view ->
                            videoViewRef = view
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isStage2EyesClosed) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.speakCraftColors.cameraBackdrop.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = "Visual hidden for vocal audit",
                                    tint = MaterialTheme.speakCraftColors.onHeroBadge
                                )
                                Text(
                                    "Close your eyes & listen purely to Rate, Pauses, Melody & Volume",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.speakCraftColors.onHeroTitle
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val vv = videoViewRef ?: return@Button
                            if (isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("video_play_pause_button")
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying) "Pause Video" else "Play Video")
                    }
                    OutlinedButton(
                        onClick = {
                            videoViewRef?.seekTo(0)
                            videoViewRef?.start()
                            isPlaying = true
                        },
                        modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Replay from start")
                    }
                    OutlinedButton(
                        onClick = { userMutedOverride = !userMutedOverride },
                        modifier = Modifier.weight(1f).testTag("video_mute_toggle_button")
                    ) {
                        Icon(
                            if (userMutedOverride) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (userMutedOverride) "Muted" else "Audio On")
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDestructiveDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("confirm_destructive_dialog"),
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_dialog_proceed")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_dialog_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CloudSyncWarningBanner(
    message: String,
    onRetrySync: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        colors = CardDefaults.cardColors(containerColor = ext.warningContainer),
        border = BorderStroke(1.dp, ext.warning),
        modifier = Modifier.fillMaxWidth().testTag("cloud_sync_warning_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CloudOff, contentDescription = "Cloud sync pending", tint = ext.warning)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            OutlinedButton(
                onClick = onRetrySync,
                modifier = Modifier.testTag("retry_cloud_sync_button")
            ) {
                Text("Retry Cloud Sync")
            }
        }
    }
}

@Composable
fun SkillBreakdownBars(
    metrics: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val reducedMotion = rememberReducedMotionEnabled()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.forEach { (name, score) ->
            val target = (score / 100f).coerceIn(0f, 1f)
            val animatedFraction by animateFloatAsState(
                targetValue = target,
                animationSpec = if (reducedMotion) tween(0) else tween(360),
                label = "skill_bar_$name"
            )
            val barColor = when {
                score >= 80 -> ext.success
                score >= 65 -> MaterialTheme.colorScheme.primary
                else -> ext.info
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = barColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "$score / 100",
                            style = MaterialTheme.typography.labelSmall,
                            color = barColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun LessonAttributionBadge(
    isCreatorDerived: Boolean,
    sourceVerified: Boolean = true
) {
    val ext = MaterialTheme.speakCraftColors
    val label = when {
        !sourceVerified -> "Pending Transcript Verification"
        isCreatorDerived -> "Creator Framework (@askvinh)"
        else -> "AI Coaching Supplement"
    }
    val tint = when {
        !sourceVerified -> ext.warning
        isCreatorDerived -> ext.info
        else -> MaterialTheme.colorScheme.primary
    }
    val icon = if (isCreatorDerived) Icons.Default.Verified else Icons.Default.AutoAwesome
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

@Composable
fun UpdateBannerCard(
    versionName: String,
    mandatory: Boolean,
    onOpenUpdater: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (mandatory) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth().testTag("update_banner_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = "Update available",
                tint = if (mandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (mandatory) "Required Update Available: v$versionName" else "SpeakCraft v$versionName Available",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (mandatory) "Update required for continued release compatibility." else "Includes verified release notes and integrity check.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onOpenUpdater, modifier = Modifier.testTag("open_updater_button")) {
                Text("View")
            }
            if (!mandatory) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss update banner")
                }
            }
        }
    }
}

@Composable
fun ShimmerSkeletonCard(heightDp: Int = 110) {
    val reducedMotion = rememberReducedMotionEnabled()
    val alpha = if (reducedMotion) {
        0.55f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val anim by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
            label = "skeleton_alpha"
        )
        anim
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
        modifier = Modifier.fillMaxWidth().height(heightDp.dp)
    ) {
        Box(Modifier.padding(16.dp))
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.AutoAwesome
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (ctaLabel != null && onCta != null) {
                OutlinedButton(
                    onClick = onCta,
                    modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                ) { Text(ctaLabel) }
            }
        }
    }
}

@Composable
fun AnimatedThinkingDots(
    label: String,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val transition = rememberInfiniteTransition(label = "thinking_dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Restart),
        label = "thinking_phase"
    )
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { idx ->
                    val alpha = if (reducedMotion) {
                        0.75f
                    } else {
                        val dist = kotlin.math.abs(phase - idx).coerceAtMost(1.5f)
                        (1f - dist * 0.5f).coerceIn(0.3f, 1f)
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ErrorRecoveryCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            if (onRetry != null || (secondaryActionLabel != null && onSecondaryAction != null)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onRetry != null) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("error_recovery_retry_button")
                        ) { Text("Try Again") }
                    }
                    if (secondaryActionLabel != null && onSecondaryAction != null) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("error_recovery_secondary_button")
                        ) { Text(secondaryActionLabel) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveTranscriptPanel(
    state: LiveTranscriptionState,
    liveTranscriptionEnabled: Boolean,
    editedTranscript: String = "",
    editable: Boolean = false,
    onEditedTranscriptChange: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val scrollState = rememberScrollState()
    val display = state.displayText.ifBlank { editedTranscript.trim() }

    LaunchedEffect(display) {
        if (display.isNotBlank()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val statusLabel = when {
        !liveTranscriptionEnabled || state.status == LiveTranscriptionStatus.DISABLED -> "Disabled"
        state.status == LiveTranscriptionStatus.INITIALIZING -> "Initializing"
        state.status == LiveTranscriptionStatus.LISTENING -> "Listening"
        state.status == LiveTranscriptionStatus.PARTIAL_RESULT -> "Streaming"
        state.status == LiveTranscriptionStatus.FINAL_RESULT -> "Captured"
        state.status == LiveTranscriptionStatus.ERROR -> "Error"
        state.status == LiveTranscriptionStatus.UNAVAILABLE -> "Unavailable"
        else -> "Ready"
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_transcript_panel")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Subtitles,
                        contentDescription = "Live Transcript",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Live Transcript",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (liveTranscriptionEnabled && state.status != LiveTranscriptionStatus.UNAVAILABLE) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (state.offlineVerified) ext.successContainer else ext.warningContainer
                        ) {
                            Text(
                                text = if (state.offlineVerified) "Offline Ready" else "Device ASR",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.offlineVerified) ext.success else ext.warning,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when (state.status) {
                            LiveTranscriptionStatus.LISTENING,
                            LiveTranscriptionStatus.PARTIAL_RESULT -> MaterialTheme.colorScheme.primaryContainer
                            LiveTranscriptionStatus.ERROR,
                            LiveTranscriptionStatus.UNAVAILABLE -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (state.status == LiveTranscriptionStatus.LISTENING ||
                                state.status == LiveTranscriptionStatus.PARTIAL_RESULT
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (state.status) {
                                    LiveTranscriptionStatus.ERROR,
                                    LiveTranscriptionStatus.UNAVAILABLE -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            if (!liveTranscriptionEnabled || state.status == LiveTranscriptionStatus.DISABLED) {
                Text(
                    text = "Live transcription is turned off. Enable it centrally in Settings → AI & Intelligence → Speech & Transcription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onOpenAiSettings,
                    modifier = Modifier
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("live_transcript_open_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open AI & Transcription Settings")
                }
            } else {
                Text(
                    text = "Engine: ${state.activeEngineLabel} (${state.languageTag})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp, max = 140.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = when {
                            display.isNotBlank() -> display
                            state.status == LiveTranscriptionStatus.LISTENING ||
                                state.status == LiveTranscriptionStatus.INITIALIZING ->
                                "Listening for your voice — words appear live as you speak..."
                            state.status == LiveTranscriptionStatus.ERROR ||
                                state.status == LiveTranscriptionStatus.UNAVAILABLE ->
                                state.errorMessage ?: "Speech recognition could not capture words."
                            else -> "Start recording to capture a live on-device transcript."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (display.isNotBlank()) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(10.dp)
                            .testTag("live_transcript_text")
                    )
                }

                if ((state.status == LiveTranscriptionStatus.ERROR ||
                        state.status == LiveTranscriptionStatus.UNAVAILABLE) &&
                    !state.errorMessage.isNullOrBlank()
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                .testTag("live_transcript_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Recognition")
                        }
                        if (!state.hasMicPermission) {
                            TextButton(
                                onClick = onOpenAiSettings,
                                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            ) {
                                Text("Check Permissions")
                            }
                        }
                    }
                }
            }

            if (editable) {
                OutlinedTextField(
                    value = editedTranscript,
                    onValueChange = onEditedTranscriptChange,
                    label = { Text("Transcript Preview / Spoken Notes (Editable)") },
                    placeholder = { Text("Recognized speech appears here after or during recording...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speaking_transcript_preview")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachEvaluationStatusCard(
    report: CoachEvaluationReport,
    onRecordAgain: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val isFailure = report.evaluationState.name in setOf("INSUFFICIENT_EVIDENCE", "RECORDING_FAILED", "ANALYSIS_FAILED")
    val container = if (isFailure) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (isFailure) MaterialTheme.colorScheme.error else ext.warning

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("coach_evaluation_status_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = report.statusHeadline,
                    tint = tint
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.statusHeadline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFailure) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Status: ${report.evaluationState.name} • No score awarded",
                        style = MaterialTheme.typography.labelSmall,
                        color = tint
                    )
                }
            }

            Text(
                text = report.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFailure) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )

            val missingDims = report.dimensions.values.filter {
                it.availabilityStatus != DimensionAvailability.EVALUATED &&
                    it.availabilityStatus != DimensionAvailability.NOT_REQUIRED
            }
            if (missingDims.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    missingDims.forEach { dim ->
                        Text(
                            text = "• ${dim.label}: ${dim.availabilityStatus.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onRecordAgain,
                    modifier = Modifier
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("coach_record_again_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Record Again")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .testTag("coach_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackReportCard(
    session: SpeakingSession,
    previousSession: SpeakingSession? = null,
    evaluationReport: CoachEvaluationReport? = null
) {
    val ext = MaterialTheme.speakCraftColors
    var selectedTab by remember(session.id) { mutableStateOf("Insights") }

    val metricsList = remember(session, evaluationReport) {
        if (evaluationReport != null && evaluationReport.dimensions.isNotEmpty()) {
            evaluationReport.dimensions.values
                .filter { it.score != null && it.availabilityStatus == DimensionAvailability.EVALUATED }
                .map { it.label to it.score!! }
        } else {
            buildList {
                add("Fluency" to session.fluencyScore.toInt())
                if (session.pronunciationAssessable) {
                    add("Pronunciation" to session.pronunciationScore.toInt())
                }
                add("Vocabulary" to session.vocabularyScore.toInt())
                add("Grammar" to session.grammarScore.toInt())
                add("Articulation" to session.articulationScore.toInt())
            }
        }
    }
    val compositeScore = remember(metricsList, evaluationReport) {
        evaluationReport?.overallScore
            ?: (metricsList.sumOf { it.second } / metricsList.size.coerceAtLeast(1)).coerceIn(0, 100)
    }
    val prevComposite = remember(previousSession) {
        previousSession?.takeIf { it.evaluationStatus == "COMPLETED" }?.let {
            ((it.fluencyScore + it.vocabularyScore + it.grammarScore + it.articulationScore) / 4L).toInt()
        }
    }
    val delta = prevComposite?.let { compositeScore - it }

    val paceStatus = remember(session.wordsPerMinute) {
        when {
            session.wordsPerMinute <= 0L -> "Audio Only"
            session.wordsPerMinute in 120L..170L -> "Ideal Pace"
            session.wordsPerMinute > 170L -> "Brisk Pace"
            else -> "Measured Pace"
        }
    }

    val scoreSweep by animateFloatAsState(
        targetValue = (compositeScore / 100f).coerceIn(0f, 1f) * 360f,
        animationSpec = tween(durationMillis = 650),
        label = "coach_score_ring"
    )

    val parsedRewrites = remember(session.betterAlternatives) {
        session.betterAlternatives.mapNotNull { raw ->
            val arrowIdx = raw.indexOf("→")
            if (arrowIdx > 0) {
                val left = raw.substring(0, arrowIdx)
                    .removePrefix("Instead of:")
                    .trim()
                    .trim('"')
                val right = raw.substring(arrowIdx + 1)
                    .replace(Regex("^\\s*Say:\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*\\[Rule:[^\\]]+\\]\\s*$"), "")
                    .trim()
                    .trim('"')
                if (left.isNotBlank() && right.isNotBlank()) left to right else null
            } else null
        }
    }

    val cleanAudioNote = remember(session.audioQualityNote) {
        session.audioQualityNote.substringBefore("|").trim()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coach_feedback_report_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header: SpeakCraft Coach Analysis + Confidence Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "SpeakCraft Coach Analysis",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "SpeakCraft Coach",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (cleanAudioNote.isNotBlank()) {
                            Text(
                                text = cleanAudioNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (session.confidenceLevel) {
                        "High" -> ext.successContainer
                        "Medium" -> ext.warningContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = "${session.confidenceLevel} Delivery",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (session.confidenceLevel) {
                            "High" -> ext.success
                            "Medium" -> ext.warning
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (evaluationReport?.isPartialEvaluation == true) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.warningContainer,
                    border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coach_partial_evaluation_banner")
                ) {
                    Text(
                        text = evaluationReport.partialReason
                            ?: "Partial evaluation: only dimensions backed by valid evidence were scored. Partial results are not directly comparable to full multimodal sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Score Ring + Pace + Trend Strip
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val primaryRingColor = MaterialTheme.colorScheme.primary
                    val trackRingColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 7.dp.toPx()
                            drawArc(
                                color = trackRingColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryRingColor,
                                startAngle = -90f,
                                sweepAngle = scoreSweep,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$compositeScore",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (session.wordsPerMinute > 0L) "${session.wordsPerMinute} WPM" else "Voice Review",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ext.pastelSkyBg
                            ) {
                                Text(
                                    text = paceStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.pastelSkyInk,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (delta != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (delta >= 0) ext.pastelMintBg else ext.pastelPeachBg
                            ) {
                                Text(
                                    text = if (delta >= 0) "+$delta pts vs previous session" else "$delta pts vs previous session",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (delta >= 0) ext.pastelMintInk else ext.pastelPeachInk,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Baseline score captured — your next take will show progress deltas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Conversational Coach Takeaway Callout
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Coach's Takeaway",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = session.overallAssessment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Interactive Deep-Dive Tabs
            val tabs = remember(session.transcript, parsedRewrites) {
                buildList {
                    add("Insights")
                    if (parsedRewrites.isNotEmpty() || session.betterAlternatives.isNotEmpty()) add("Rewrites")
                    add("Skills")
                    if (session.transcript.isNotBlank()) add("Transcript")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) },
                        modifier = Modifier.testTag("feedback_tab_${tab.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                "Insights" -> {
                    if (session.strengths.isNotEmpty()) {
                        FeedbackHighlightBox(
                            title = "What Landed Well",
                            items = session.strengths,
                            icon = Icons.Default.CheckCircle,
                            tint = ext.success,
                            container = ext.pastelMintBg.copy(alpha = 0.55f)
                        )
                    }
                    val focusItems = buildList {
                        addAll(session.mistakes.filterNot { it.startsWith("No major") })
                        if (session.fillerWordsDetected.isNotEmpty()) {
                            add("Fillers noticed: ${session.fillerWordsDetected.take(5).joinToString(", ")}")
                        }
                        if (session.repeatedExpressions.isNotEmpty()) {
                            add("Repeated phrasing: ${session.repeatedExpressions.take(3).joinToString(", ")}")
                        }
                    }
                    if (focusItems.isNotEmpty()) {
                        FeedbackHighlightBox(
                            title = "Focus Next",
                            items = focusItems,
                            icon = Icons.Default.Lightbulb,
                            tint = ext.warning,
                            container = ext.pastelPeachBg.copy(alpha = 0.55f)
                        )
                    }
                    if (session.improvementSuggestions.isNotEmpty()) {
                        FeedbackBulletSection(
                            title = "Coaching Tips",
                            items = session.improvementSuggestions,
                            icon = Icons.Default.AutoAwesome,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                "Rewrites" -> {
                    if (parsedRewrites.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            parsedRewrites.forEach { (before, after) ->
                                BeforeAfterRewriteCard(before = before, after = after)
                            }
                        }
                    } else if (session.betterAlternatives.isNotEmpty()) {
                        FeedbackBulletSection(
                            title = "Stronger Phrasing",
                            items = session.betterAlternatives,
                            icon = Icons.Default.Lightbulb,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                "Skills" -> {
                    if (evaluationReport != null && evaluationReport.dimensions.isNotEmpty()) {
                        SkillBreakdownBars(metrics = metricsList)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            evaluationReport.dimensions.values.forEach { detail ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("coach_dimension_row_${detail.key}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(detail.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = if (detail.score != null) "${detail.score}/100" else detail.availabilityStatus.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (detail.score != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Text(
                                            text = "${detail.evidenceSource} • ${(detail.confidence * 100).toInt()}% confidence",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (detail.note.isNotBlank()) {
                                            Text(detail.note, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (!session.pronunciationAssessable) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Phonetic accuracy was skipped because the spoken clip was too short or noisy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        SkillBreakdownBars(metrics = metricsList)
                    }
                    if (session.articulationClarity.isNotBlank() || session.thoughtOrganization.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (session.articulationClarity.isNotBlank()) {
                                Text(
                                    text = "Clarity: ${session.articulationClarity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (session.thoughtOrganization.isNotBlank()) {
                                Text(
                                    text = "Structure: ${session.thoughtOrganization}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (session.pronunciationNotes.isNotEmpty()) {
                        FeedbackBulletSection(
                            title = "Vocal & Enunciation Notes",
                            items = session.pronunciationNotes,
                            icon = Icons.Default.GraphicEq,
                            tint = ext.info
                        )
                    }
                }

                "Transcript" -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = session.transcript,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    if (session.fillerWordsDetected.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            session.fillerWordsDetected.take(8).forEach { filler ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = filler,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Next 60-Second Action Drill Footer
            if (session.nextRecommendedActivity.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next Micro-Drill",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = session.nextRecommendedActivity,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BeforeAfterRewriteCard(
    before: String,
    after: String
) {
    val ext = MaterialTheme.speakCraftColors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "YOU SAID",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "\"$before\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TRY SAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.success,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"$after\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FeedbackHighlightBox(
    title: String,
    items: List<String>,
    icon: ImageVector,
    tint: Color,
    container: Color
) {
    if (items.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
            }
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FeedbackBulletSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    tint: Color
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = tint)
        }
        items.forEach { item ->
            Text("• $item", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 22.dp))
        }
    }
}
