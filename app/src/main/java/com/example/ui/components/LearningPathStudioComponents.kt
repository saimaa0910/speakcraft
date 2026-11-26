package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AdaptiveWindowTier
import com.example.ui.theme.SpeakCraftExtendedColors
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

/**
 * Distinct Studio visual identity for each of SpeakCraft's 7 Learning Path programs.
 */
data class ProgramVisualStyle(
    val containerColor: Color,
    val accentColor: Color,
    val badgeBg: Color,
    val badgeInk: Color,
    val icon: ImageVector
)

fun programVisualStyleFor(
    programId: String,
    ext: SpeakCraftExtendedColors
): ProgramVisualStyle = when (programId) {
    "30_day_challenge" -> ProgramVisualStyle(
        containerColor = ext.pastelLavenderBg,
        accentColor = ext.pastelLavenderInk,
        badgeBg = ext.pastelLavenderBg,
        badgeInk = ext.pastelLavenderInk,
        icon = Icons.Default.EmojiEvents
    )
    "60_day_fluency" -> ProgramVisualStyle(
        containerColor = ext.pastelSkyBg,
        accentColor = ext.pastelSkyInk,
        badgeBg = ext.pastelSkyBg,
        badgeInk = ext.pastelSkyInk,
        icon = Icons.Default.Bolt
    )
    "90_day_advanced" -> ProgramVisualStyle(
        containerColor = ext.pastelGoldBg,
        accentColor = ext.pastelGoldInk,
        badgeBg = ext.pastelGoldBg,
        badgeInk = ext.pastelGoldInk,
        icon = Icons.Default.WorkspacePremium
    )
    "vocab_challenge" -> ProgramVisualStyle(
        containerColor = ext.pastelMintBg,
        accentColor = ext.pastelMintInk,
        badgeBg = ext.pastelMintBg,
        badgeInk = ext.pastelMintInk,
        icon = Icons.AutoMirrored.Filled.MenuBook
    )
    "pronunciation_program" -> ProgramVisualStyle(
        containerColor = ext.pastelPeachBg,
        accentColor = ext.pastelPeachInk,
        badgeBg = ext.pastelPeachBg,
        badgeInk = ext.pastelPeachInk,
        icon = Icons.Default.RecordVoiceOver
    )
    "public_speaking_program" -> ProgramVisualStyle(
        containerColor = ext.pastelRoseBg,
        accentColor = ext.pastelRoseInk,
        badgeBg = ext.pastelRoseBg,
        badgeInk = ext.pastelRoseInk,
        icon = Icons.Default.Campaign
    )
    else -> ProgramVisualStyle(
        containerColor = ext.pastelPeriwinkleBg,
        accentColor = ext.pastelPeriwinkleInk,
        badgeBg = ext.pastelPeriwinkleBg,
        badgeInk = ext.pastelPeriwinkleInk,
        icon = Icons.Default.Work
    )
}

/**
 * Responsive container centering screen content symmetrically inside SpeakCraft window bounds
 * and supplying current [AdaptiveWindowTier] to child layouts.
 */
@Composable
fun StudioResponsiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable (tier: AdaptiveWindowTier, maxWidthDp: Dp) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val availableWidth = maxWidth
        val tier = SpeakCraftSpacing.tierForWidth(availableWidth)
        val cappedWidth = SpeakCraftSpacing.contentMaxWidthFor(tier)
        Box(
            modifier = Modifier
                .widthIn(max = cappedWidth)
                .fillMaxSize()
        ) {
            content(tier, availableWidth)
        }
    }
}

/**
 * Animated circular progress/score ring respecting system reduced-motion settings.
 */
@Composable
fun AnimatedMasteryRing(
    progress: Float,
    centerValueText: String,
    centerSubLabel: String,
    accentColor: Color,
    trackColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 74.dp,
    strokeWidth: Dp = 7.dp
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reducedMotion) tween(0) else tween(420),
        label = "mastery_ring"
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$centerSubLabel $centerValueText" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Text(
                text = centerSubLabel,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Signature SpeakCraft gradient hero header featuring glassy stat pills and an animated progress ring.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioGradientHeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    ringProgress: Float,
    ringValueText: String,
    ringLabel: String,
    modifier: Modifier = Modifier,
    eyebrowIcon: ImageVector? = null,
    bottomPills: List<Pair<String, String>> = emptyList(),
    actionsContent: (@Composable () -> Unit)? = null
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ext.heroOverlayStart,
                            ext.heroOverlayMid,
                            ext.heroOverlayEnd
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (eyebrowIcon != null) {
                                Icon(
                                    imageVector = eyebrowIcon,
                                    contentDescription = null,
                                    tint = ext.onHeroBadge,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = eyebrow,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ext.onHeroBadge
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ext.onHeroTitle
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.onHeroSubtitle
                        )
                    }

                    AnimatedMasteryRing(
                        progress = ringProgress,
                        centerValueText = ringValueText,
                        centerSubLabel = ringLabel,
                        accentColor = ext.onHeroBadge,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        textColor = ext.onHeroTitle
                    )
                }

                if (bottomPills.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        bottomPills.forEach { (label, value) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.onHeroTitle
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.onHeroSubtitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (actionsContent != null) {
                    actionsContent()
                }
            }
        }
    }
}

/**
 * Interactive horizontal timeline bead strip allowing quick visual scan & direct day jump.
 */
@Composable
fun DayProgressBeadStrip(
    totalDays: Int,
    completedDays: Set<Int>,
    nextDayNumber: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val days = (1..totalDays).toList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Timeline ($totalDays Days)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap any day to jump",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(days, key = { it }) { day ->
                val isDone = completedDays.contains(day)
                val isCurrent = !isDone && day == nextDayNumber
                val bg = when {
                    isDone -> ext.pastelMintBg
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                }
                val fg = when {
                    isDone -> ext.pastelMintInk
                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val border = when {
                    isDone -> BorderStroke(1.dp, ext.success.copy(alpha = 0.45f))
                    isCurrent -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else -> null
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    border = border,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectDay(day) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = ext.success,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "D$day",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Medium,
                            color = fg
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data summary surfaced on Dashboard so active Learning Paths connect directly with home quick resume.
 */
data class ActiveLearningPathBannerUi(
    val programId: String,
    val programTitle: String,
    val completedDaysCount: Int,
    val totalDays: Int,
    val nextDayNumber: Int,
    val nextLessonTitle: String,
    val completionFraction: Float
)

/**
 * Compact Dashboard card highlighting the learner's active Learning Program or inviting entry.
 */
@Composable
fun ActiveLearningPathDashboardCard(
    activeBanner: ActiveLearningPathBannerUi?,
    onResumeLesson: (programId: String, dayNumber: Int) -> Unit,
    onExploreAllPaths: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val style = if (activeBanner != null) {
        programVisualStyleFor(activeBanner.programId, ext)
    } else {
        ProgramVisualStyle(
            containerColor = ext.pastelLavenderBg,
            accentColor = ext.pastelLavenderInk,
            badgeBg = ext.pastelLavenderBg,
            badgeInk = ext.pastelLavenderInk,
            icon = Icons.Default.EmojiEvents
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_active_learning_path_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeBanner != null) {
                AnimatedMasteryRing(
                    progress = activeBanner.completionFraction,
                    centerValueText = "${(activeBanner.completionFraction * 100).toInt()}%",
                    centerSubLabel = "Path",
                    accentColor = style.accentColor,
                    trackColor = style.accentColor.copy(alpha = 0.16f),
                    textColor = MaterialTheme.colorScheme.onSurface,
                    size = 64.dp,
                    strokeWidth = 6.dp
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = style.containerColor,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (activeBanner != null) {
                    Text(
                        text = "ACTIVE PATH · ${activeBanner.completedDaysCount}/${activeBanner.totalDays} DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = style.accentColor
                    )
                    Text(
                        text = activeBanner.programTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Next: Day ${activeBanner.nextDayNumber} — ${activeBanner.nextLessonTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { activeBanner.completionFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = style.accentColor,
                        trackColor = style.accentColor.copy(alpha = 0.15f)
                    )
                } else {
                    Text(
                        text = "7 STRUCTURED LEARNING PATHS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = style.accentColor
                    )
                    Text(
                        text = "280 Guided Lessons & Capstones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily structured plans across fluency, pronunciation, vocabulary, and interviews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (activeBanner != null) {
                Button(
                    onClick = { onResumeLesson(activeBanner.programId, activeBanner.nextDayNumber) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Day ${activeBanner.nextDayNumber}")
                }
            } else {
                TextButton(onClick = onExploreAllPaths) {
                    Text("Browse")
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
