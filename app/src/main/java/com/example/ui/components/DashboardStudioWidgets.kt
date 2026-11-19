package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.RecommendedNextAction
import com.example.data.learning.LearningStreakState
import com.example.data.learning.PersonalizedDailyPlan
import com.example.data.model.DailyActivity
import com.example.data.model.UserProfile
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class SpotlightSlide(
    val id: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val primaryCta: String,
    val primaryRoute: String,
    val quickPracticeRoute: String
)

@Composable
fun WelcomeBackBanner(
    displayName: String,
    learningTrack: String,
    learningDay: Int,
    overallScore: Int,
    personalizedCue: String,
    reducedMotion: Boolean = false,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (reducedMotion || hasAnimated) 1f else 0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion || hasAnimated) {
            progress.snapTo(1f)
            hasAnimated = true
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
            hasAnimated = true
        }
    }

    val p = progress.value
    val initials = remember(displayName) {
        displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "S" }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = 0.2f + 0.8f * p
                translationY = (1f - p) * 20f
                val s = 0.97f + 0.03f * p
                scaleX = s
                scaleY = s
            }
            .testTag("welcome_back_banner"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick)
                    .testTag("welcome_avatar_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Welcome back, $displayName!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = personalizedCue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ext.pastelLavenderBg
                    ) {
                        Text(
                            text = "$learningTrack • Day $learningDay",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.pastelLavenderInk,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ext.pastelMintBg
                    ) {
                        Text(
                            text = "Score $overallScore/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.pastelMintInk,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpotlightHeroCard(
    profile: UserProfile?,
    recommendedNextAction: RecommendedNextAction,
    overallScore: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    learningDay: Int = ((profile?.streakDays ?: 1L).coerceAtLeast(1L)).toInt(),
    reducedMotion: Boolean = false
) {
    val ext = MaterialTheme.speakCraftColors
    val scope = rememberCoroutineScope()
    val levelLabel = profile?.communicationLevel?.ifBlank { "Foundation" } ?: "Foundation"
    val slides = remember(recommendedNextAction, levelLabel, learningDay, overallScore) {
        val baseSlides = listOf(
            SpotlightSlide(
                id = "slide_3vs",
                badge = "$levelLabel Track • Day $learningDay",
                title = "The 3 Vs of Communication",
                subtitle = "Build a strong foundation with Visual, Vocal and Verbal communication.",
                primaryCta = "Start Lesson",
                primaryRoute = "lesson_detail/lesson_vinh_3stage_audit",
                quickPracticeRoute = "speaking_lab"
            ),
            SpotlightSlide(
                id = "slide_clarity",
                badge = "Articulation & Pace • Day $learningDay",
                title = "Speak with Clarity",
                subtitle = "Improve your articulation, vocal projection, and natural speaking pace.",
                primaryCta = "Start Lesson",
                primaryRoute = "articulation_lab",
                quickPracticeRoute = "voice_training"
            ),
            SpotlightSlide(
                id = "slide_confidence",
                badge = "Everyday Fluency • Score $overallScore",
                title = "Speak with Confidence",
                subtitle = "Practice speaking naturally in real conversations and daily scenarios.",
                primaryCta = "Start Lesson",
                primaryRoute = "conversation",
                quickPracticeRoute = "challenges"
            ),
            SpotlightSlide(
                id = "slide_recommended",
                badge = "Recommended Next • $levelLabel",
                title = recommendedNextAction.title.ifBlank { "Vocabulary Speaking Drill" },
                subtitle = recommendedNextAction.reason.ifBlank {
                    "Complete your 5-stage spoken vocabulary workflow and contextual sentences."
                },
                primaryCta = recommendedNextAction.ctaLabel.ifBlank { "Continue Lesson" },
                primaryRoute = recommendedNextAction.targetRoute.ifBlank { "vocabulary" },
                quickPracticeRoute = "speaking_lab"
            )
        )
        baseSlides
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { slides.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("welcome_carousel_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome_hero_pager")
        ) { pageIdx ->
            val slide = slides[pageIdx]
            val pageOffset = ((pagerState.currentPage - pageIdx) + pagerState.currentPageOffsetFraction).absoluteValue
            val cardScale = if (reducedMotion) 1f else (1f - (pageOffset.coerceIn(0f, 1f) * 0.04f))
            val cardAlpha = if (reducedMotion) 1f else (1f - (pageOffset.coerceIn(0f, 1f) * 0.16f))

            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
                    .testTag("hero_slide_$pageIdx")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(252.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_coach_hero),
                        contentDescription = "SpeakCraft studio microphone stage",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ext.heroOverlayStart,
                                        ext.heroOverlayMid,
                                        ext.heroOverlayEnd
                                    )
                                )
                            )
                    )
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 22.dp)
                            .size(width = 88.dp, height = 96.dp)
                    ) {
                        val heights = listOf(0.22f, 0.44f, 0.72f, 0.96f, 0.58f, 0.84f, 0.40f, 0.24f)
                        val gap = size.width / heights.size
                        val barW = (gap * 0.46f).coerceAtLeast(4f)
                        val midY = size.height / 2f
                        heights.forEachIndexed { idx, frac ->
                            val h = size.height * frac
                            val x = idx * gap + (gap - barW) / 2f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(ext.sidebarActiveEnd, ext.onHeroBadge)
                                ),
                                topLeft = Offset(x, midY - h / 2f),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                                alpha = 0.72f
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ext.onHeroTitle.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, ext.onHeroTitle.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ext.onHeroBadge,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = slide.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.onHeroBadge
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ext.heroOverlayStart.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, ext.onHeroTitle.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "Score $overallScore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.onHeroTitle,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth(0.78f)) {
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = ext.onHeroTitle,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = slide.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.onHeroSubtitle,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onNavigate(slide.primaryRoute) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.onHeroTitle,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag(if (pageIdx == pagerState.currentPage) "recommended_action_button" else "recommended_action_button_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = slide.primaryCta,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = { onNavigate(slide.quickPracticeRoute) },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, ext.onHeroTitle.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ext.onHeroTitle.copy(alpha = 0.14f),
                                    contentColor = ext.onHeroTitle
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag(if (pageIdx == pagerState.currentPage) "hero_quick_practice_button" else "hero_quick_practice_button_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Quick Practice", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Swipe indicator dots (tappable to jump cleanly)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome_carousel_dots"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.indices.forEach { idx ->
                val selected = idx == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                        .size(width = if (selected) 20.dp else 7.dp, height = 7.dp)
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(idx) }
                        }
                        .testTag("carousel_dot_$idx")
                )
            }
        }
    }
}

private data class PracticePlanRowSpec(
    val stepId: String,
    val title: String,
    val duration: String,
    val route: String,
    val icon: ImageVector,
    val swatchIndex: Int
)

@Composable
fun TodaysPracticePlanCard(
    todayActivity: DailyActivity?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    personalizedPlan: PersonalizedDailyPlan? = null,
    onCompleteDailyTask: ((String) -> Unit)? = null
) {
    val ext = MaterialTheme.speakCraftColors
    val completedIds = todayActivity?.completedStepIds.orEmpty().toSet()

    val rows = remember(personalizedPlan) {
        if (personalizedPlan != null && personalizedPlan.tasks.isNotEmpty()) {
            personalizedPlan.tasks.mapIndexed { idx, task ->
                val icon = when {
                    task.category.contains("Vocab", ignoreCase = true) -> Icons.Default.Book
                    task.category.contains("Pronun", ignoreCase = true) -> Icons.Default.GraphicEq
                    task.category.contains("Grammar", ignoreCase = true) -> Icons.Default.FormatQuote
                    task.category.contains("Listen", ignoreCase = true) -> Icons.Default.Headphones
                    task.category.contains("Reflec", ignoreCase = true) -> Icons.Default.EditNote
                    else -> Icons.Default.Mic
                }
                PracticePlanRowSpec(
                    stepId = task.id,
                    title = task.title,
                    duration = "${task.durationMinutes} min",
                    route = task.targetRoute.ifBlank { "daily_practice" },
                    icon = icon,
                    swatchIndex = idx % 7
                )
            }
        } else {
            listOf(
                PracticePlanRowSpec("vocab_3_words", "Learn 3 New Vocabulary Words", "5 min", "vocabulary", Icons.Default.Book, 0),
                PracticePlanRowSpec("articulation_drill", "Articulation & Enunciation Drill", "7 min", "articulation_lab", Icons.Default.GraphicEq, 2),
                PracticePlanRowSpec("sentence_builder", "Contextual Sentence Construction", "5 min", "grammar_speaking", Icons.Default.FormatQuote, 5),
                PracticePlanRowSpec("vocal_warmup", "Vocal Foundations Practice", "8 min", "voice_training", Icons.Default.Mic, 6),
                PracticePlanRowSpec("listening_shadow", "Active Listening Exercise", "6 min", "listening_lab", Icons.Default.Headphones, 4),
                PracticePlanRowSpec("daily_reflection", "Daily Reflection", "3 min", "daily_practice", Icons.Default.EditNote, 3)
            )
        }
    }

    val planDoneIds = remember(personalizedPlan) {
        personalizedPlan?.tasks?.filter { it.isCompleted }?.map { it.id }?.toSet().orEmpty()
    }
    val completedCount = if (personalizedPlan != null) {
        personalizedPlan.completedCount
    } else {
        rows.count { it.stepId in completedIds }
            .coerceAtLeast((todayActivity?.completedStepIds?.size ?: 0).coerceAtMost(rows.size))
    }

    val swatches = listOf(
        ext.pastelLavenderBg to ext.pastelLavenderInk,
        ext.pastelSkyBg to ext.pastelSkyInk,
        ext.pastelMintBg to ext.pastelMintInk,
        ext.pastelPeachBg to ext.pastelPeachInk,
        ext.pastelRoseBg to ext.pastelRoseInk,
        ext.pastelGoldBg to ext.pastelGoldInk,
        ext.pastelPeriwinkleBg to ext.pastelPeriwinkleInk
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("todays_practice_plan_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Practice Plan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (personalizedPlan != null) {
                        Text(
                            text = if (personalizedPlan.isRecoveryPlan) {
                                "Gentle Recovery Plan • ${personalizedPlan.targetMinutes} min"
                            } else {
                                "${personalizedPlan.level.label} • ${personalizedPlan.targetMinutes} min target"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "$completedCount / ${rows.size} completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            rows.forEachIndexed { idx, row ->
                val isDone = if (personalizedPlan != null) {
                    row.stepId in planDoneIds
                } else {
                    row.stepId in completedIds || idx < completedCount
                }
                val (bg, tint) = swatches[row.swatchIndex % swatches.size]
                val isHighlighted = !isDone && rows.indexOfFirst { r ->
                    if (personalizedPlan != null) r.stepId !in planDoneIds else r.stepId !in completedIds
                } == idx
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isHighlighted) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigate(row.route) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isDone) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                                .clickable(enabled = !isDone && onCompleteDailyTask != null) {
                                    onCompleteDailyTask?.invoke(row.stepId)
                                }
                                .testTag("daily_task_check_${row.stepId}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = row.icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = row.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = row.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isHighlighted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakHighlightCard(
    streakDays: Long,
    modifier: Modifier = Modifier,
    streakState: LearningStreakState? = null
) {
    val ext = MaterialTheme.speakCraftColors
    val displayDays = streakState?.currentStreakDays?.toLong() ?: streakDays.coerceAtLeast(1L)
    val longestDays = streakState?.longestStreakDays?.toLong()?.coerceAtLeast(displayDays) ?: displayDays
    val subtitle = when {
        streakState == null -> "Keep practicing daily!"
        streakState.todayRequirementMet -> "Today's goal met! Best streak: $longestDays days"
        displayDays > 0 -> "Complete 2 tasks today to extend • Best: ${longestDays}d"
        else -> "Complete 2 tasks today to start your streak!"
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("streak_highlight_card"),
        shape = MaterialTheme.shapes.large,
        color = ext.streakSurface,
        border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ext.warningContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Practice streak",
                        tint = ext.warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$displayDays Day Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            streakState?.latestMilestoneUnlocked?.let { milestone ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ext.warningContainer
                ) {
                    Text(
                        text = "${milestone}d ★",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ext.warning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TodaysProgressRingCard(
    completedTasks: Int,
    totalTasks: Int = 6,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val clampedDone = completedTasks.coerceIn(0, totalTasks)
    val fraction = if (totalTasks > 0) clampedDone.toFloat() / totalTasks.toFloat() else 0f
    val ringStart = MaterialTheme.colorScheme.primary
    val ringEnd = ext.sidebarActiveEnd
    val trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Today's Progress",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 11.dp.toPx()
                        val inset = strokePx / 2f
                        val arcSize = Size(size.width - strokePx, size.height - strokePx)
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        val sweep = (fraction * 360f).coerceAtLeast(if (clampedDone == 0) 28f else 0f)
                        drawArc(
                            brush = Brush.sweepGradient(listOf(ringStart, ringEnd, ringStart)),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$clampedDone / $totalTasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pillars = listOf(
                        Triple("Learn", Icons.Outlined.Book, "learning_paths"),
                        Triple("Practice", Icons.Outlined.Mic, "speaking_lab"),
                        Triple("Review", Icons.Outlined.BarChart, "analytics"),
                        Triple("Reflect", Icons.Outlined.StarOutline, "daily_practice")
                    )
                    pillars.forEach { (label, icon, route) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigate(route) }
                                .padding(vertical = 3.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
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
fun WeeklyGoalCard(
    activeDaysThisWeek: Int,
    targetDays: Int = 5,
    modifier: Modifier = Modifier,
    weeklyCompletionMap: Map<String, Boolean>? = null
) {
    val dayEntries = remember(weeklyCompletionMap, activeDaysThisWeek) {
        if (!weeklyCompletionMap.isNullOrEmpty()) {
            listOf("Mon" to "M", "Tue" to "T", "Wed" to "W", "Thu" to "T", "Fri" to "F", "Sat" to "S", "Sun" to "S")
                .map { (key, letter) -> letter to (weeklyCompletionMap[key] == true) }
        } else {
            val doneDays = activeDaysThisWeek.coerceIn(0, 5)
            listOf("M", "T", "W", "T", "F").mapIndexed { idx, letter -> letter to (idx < doneDays) }
        }
    }
    val doneDays = dayEntries.count { it.second }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Weekly Goal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$doneDays / $targetDays days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayEntries.forEach { (letter, done) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (done) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Day completed",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = letter,
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
fun StudioMetricCard(
    label: String,
    value: String,
    pillText: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    pillBg: Color,
    pillInk: Color,
    modifier: Modifier = Modifier
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            Surface(
                shape = RoundedCornerShape(50),
                color = pillBg
            ) {
                Text(
                    text = pillText,
                    style = MaterialTheme.typography.labelSmall,
                    color = pillInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineAiMentorCard(
    onOpenMentor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_ai_mentor_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.pastelLavenderBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ext.pastelLavenderInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "AI Mentor",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your personal coach for confident communication.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Rounded prompt bar mirroring reference screenshot
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenMentor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ext.pastelLavenderBg,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = ext.pastelLavenderInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Ask me anything about speaking, vocabulary, pronunciation, or your practice plan...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Open AI Mentor",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Try asking:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prompts = listOf(
                    "Create my 7-day plan" to Icons.Default.CalendarMonth,
                    "Fix my speaking mistakes" to Icons.Default.TrackChanges,
                    "Next lesson for me" to Icons.Default.PlayArrow
                )
                prompts.forEach { (promptText, promptIcon) ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenMentor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = promptIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class QuickAccessTileSpec(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val swatchIndex: Int
)

@Composable
fun QuickAccessPastelGrid(
    columns: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val tiles = remember {
        listOf(
            QuickAccessTileSpec("Vocabulary\nDevelopment", "vocabulary", Icons.Default.Book, 0),
            QuickAccessTileSpec("Pronunciation\nPractice", "pronunciation", Icons.Default.GraphicEq, 1),
            QuickAccessTileSpec("Conversation\nMastery", "conversation", Icons.Default.ChatBubble, 2),
            QuickAccessTileSpec("Storytelling\n& Influence", "module_lessons/storytelling", Icons.Default.AutoStories, 3),
            QuickAccessTileSpec("Public\nSpeaking", "public_speaking", Icons.Default.Campaign, 4),
            QuickAccessTileSpec("Confidence\nTraining", "module_lessons/confidence", Icons.Default.SelfImprovement, 5),
            QuickAccessTileSpec("Body\nLanguage", "module_lessons/body_language", Icons.Default.Gesture, 6),
            QuickAccessTileSpec("Communication\nChallenges", "challenges", Icons.Default.EmojiEvents, 7)
        )
    }
    val swatches = listOf(
        ext.pastelLavenderBg to ext.pastelLavenderInk,
        ext.pastelSkyBg to ext.pastelSkyInk,
        ext.pastelMintBg to ext.pastelMintInk,
        ext.pastelPeachBg to ext.pastelPeachInk,
        ext.pastelRoseBg to ext.pastelRoseInk,
        ext.pastelGoldBg to ext.pastelGoldInk,
        ext.pastelPeriwinkleBg to ext.pastelPeriwinkleInk,
        ext.pastelCoralBg to ext.pastelCoralInk
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_quick_access_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ext.pastelLavenderBg,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = ext.pastelLavenderInk,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val colCount = columns.coerceIn(2, 4)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(colCount).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTiles.forEach { tile ->
                            val (bg, tint) = swatches[tile.swatchIndex % swatches.size]
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = bg,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 86.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onNavigate(tile.route) }
                                    .testTag("quick_access_${tile.route}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title.replace("\n", " "),
                                        tint = tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = tile.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                        repeat((colCount - rowTiles.size).coerceAtLeast(0)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

data class LearningTrackProgressSpec(
    val title: String,
    val subtitle: String,
    val percent: Int,
    val icon: ImageVector,
    val hubFilter: String,
    val swatchIndex: Int
)

@Composable
fun LearningTracksStrip(
    tracks: List<LearningTrackProgressSpec>,
    columns: Int,
    onViewAll: () -> Unit,
    onSelectTrack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.speakCraftColors
    val swatches = listOf(
        ext.pastelGoldBg to ext.pastelGoldInk,
        ext.pastelLavenderBg to ext.pastelLavenderInk,
        ext.pastelSkyBg to ext.pastelSkyInk,
        ext.pastelMintBg to ext.pastelMintInk
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_learning_tracks"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "My Learning Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            TextButton(
                onClick = onViewAll,
                modifier = Modifier.defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
            ) {
                Text("View All", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        val colCount = columns.coerceIn(1, 4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tracks.chunked(colCount).forEach { rowTracks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTracks.forEach { track ->
                        val (bg, tint) = swatches[track.swatchIndex % swatches.size]
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onSelectTrack(track.hubFilter) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = bg,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = track.icon,
                                                contentDescription = null,
                                                tint = tint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { (track.percent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        text = "${track.percent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    repeat((colCount - rowTracks.size).coerceAtLeast(0)) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
