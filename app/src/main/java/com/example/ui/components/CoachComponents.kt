package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SpeakingSession
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.AmberSpotlight
import com.example.ui.theme.EmeraldSuccess

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
                color = MaterialTheme.colorScheme.onBackground
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
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    delta: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!delta.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = delta,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
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
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            var lastMs = 0L
            while (true) {
                withFrameMillis { frameMs ->
                    if (lastMs != 0L) {
                        phase += ((frameMs - lastMs) / 1000f) * 5.2f
                    }
                    lastMs = frameMs
                }
            }
        }
    }

    val barColor = when {
        isRecording && isPaused -> MaterialTheme.colorScheme.secondary
        isRecording -> AmberSpotlight
        else -> MaterialTheme.colorScheme.outline
    }
    val secondaryColor = AcousticTeal

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val barCount = 30
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.52f).coerceAtLeast(4f)
        val centerY = size.height / 2f
        for (i in 0 until barCount) {
            val waveShape = kotlin.math.sin((i * 0.42f) + phase)
            val ampFactor = when {
                isRecording && !isPaused -> ((amplitude * 0.85f) + (kotlin.math.abs(waveShape) * 0.35f)).coerceIn(0.14f, 1f)
                isRecording && isPaused -> 0.22f
                else -> 0.14f
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
}

@Composable
fun ScoreTrendSparkline(
    scores: List<Int>,
    accentColor: Color = AcousticTeal,
    modifier: Modifier = Modifier
) {
    val safeScores = if (scores.size >= 2) scores else listOf(scores.firstOrNull() ?: 60, scores.firstOrNull() ?: 62)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val minVal = (safeScores.minOrNull() ?: 0).coerceAtMost(40).toFloat()
        val maxVal = (safeScores.maxOrNull() ?: 100).coerceAtLeast(95).toFloat()
        val range = (maxVal - minVal).coerceAtLeast(10f)
        val stepX = size.width / (safeScores.size - 1).coerceAtLeast(1)

        val points = safeScores.mapIndexed { idx, score ->
            val x = idx * stepX
            val normalizedY = ((score - minVal) / range).coerceIn(0f, 1f)
            val y = size.height - (normalizedY * (size.height - 20f)) - 10f
            Offset(x, y)
        }

        val linePath = Path().apply {
            points.forEachIndexed { idx, pt ->
                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
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
        points.forEach { pt ->
            drawCircle(color = accentColor, radius = 6f, center = pt)
            drawCircle(color = Color.White, radius = 3f, center = pt)
        }
    }
}

@Composable
fun SkillBreakdownBars(
    metrics: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.forEach { (name, score) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("$score / 100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (score / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (score >= 75) EmeraldSuccess else MaterialTheme.colorScheme.primary,
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
    val label = when {
        !sourceVerified -> "Pending Transcript Verification"
        isCreatorDerived -> "Creator Framework (@askvinh)"
        else -> "AI Coaching Supplement"
    }
    val tint = when {
        !sourceVerified -> AmberSpotlight
        isCreatorDerived -> AcousticTeal
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
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "skeleton_alpha"
    )
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
    onCta: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (ctaLabel != null && onCta != null) {
                OutlinedButton(onClick = onCta) { Text(ctaLabel) }
            }
        }
    }
}

@Composable
fun ErrorRecoveryCard(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Try Again") }
            }
        }
    }
}

@Composable
fun FeedbackReportCard(
    session: SpeakingSession,
    previousSession: SpeakingSession? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Assessment report", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Evidence-Based AI Feedback", style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (session.confidenceLevel) {
                        "High" -> EmeraldSuccess.copy(alpha = 0.15f)
                        "Medium" -> AmberSpotlight.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = "${session.confidenceLevel} Confidence",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (session.audioQualityNote.isNotBlank()) {
                Text(
                    text = "Audio Check: ${session.audioQualityNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(session.overallAssessment, style = MaterialTheme.typography.bodyLarge)

            val metricsList = buildList {
                add("Fluency" to session.fluencyScore.toInt())
                if (session.pronunciationAssessable) {
                    add("Pronunciation" to session.pronunciationScore.toInt())
                }
                add("Vocabulary" to session.vocabularyScore.toInt())
                add("Grammar" to session.grammarScore.toInt())
                add("Articulation" to session.articulationScore.toInt())
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Pace", "${session.wordsPerMinute} WPM", null, AcousticTeal, Modifier.weight(1f))
                val avg = (metricsList.sumOf { it.second } / metricsList.size.coerceAtLeast(1))
                val prevAvg = previousSession?.let {
                    ((it.fluencyScore + it.vocabularyScore + it.grammarScore + it.articulationScore) / 4).toInt()
                }
                val deltaStr = prevAvg?.let {
                    val diff = avg - it
                    if (diff >= 0) "+$diff vs prior" else "$diff vs prior"
                }
                MetricTile("Composite", "$avg/100", deltaStr, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }

            if (!session.pronunciationAssessable) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Pronunciation Score Withheld: Spoken audio was too brief or noisy to score phonetic accuracy honestly.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            SkillBreakdownBars(metrics = metricsList)

            if (session.articulationClarity.isNotBlank() || session.thoughtOrganization.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (session.articulationClarity.isNotBlank()) {
                        Text("Articulation Clarity: ${session.articulationClarity}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (session.thoughtOrganization.isNotBlank()) {
                        Text("Thought Organization: ${session.thoughtOrganization}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (session.transcript.isNotBlank()) {
                HorizontalDivider()
                Text("Observed Transcript", style = MaterialTheme.typography.labelLarge)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(session.transcript, style = MaterialTheme.typography.bodyMedium)
                }
            }

            FeedbackBulletSection("What Was Done Well", session.strengths, Icons.Default.CheckCircle, EmeraldSuccess)
            FeedbackBulletSection(
                "Mistakes & Filler Words",
                session.mistakes + session.fillerWordsDetected.map { "Filler: $it" },
                Icons.Default.Warning,
                MaterialTheme.colorScheme.error
            )
            FeedbackBulletSection("Repeated Expressions", session.repeatedExpressions, Icons.Default.Repeat, AmberSpotlight)
            FeedbackBulletSection("Pronunciation & Quoted Examples", session.pronunciationNotes, Icons.Default.GraphicEq, AcousticTeal)
            FeedbackBulletSection("Stronger Alternatives & Drills", session.betterAlternatives + session.recommendedExercises, Icons.Default.Lightbulb, AmberSpotlight)

            if (session.nextRecommendedActivity.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "Next Recommended Activity: ${session.nextRecommendedActivity}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }
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
