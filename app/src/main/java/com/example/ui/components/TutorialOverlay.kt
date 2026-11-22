package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FeatureTutorial
import com.example.data.model.FeatureTutorialRegistry
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

data class TutorialOverlayStep(
    val featureKey: String,
    val title: String,
    val categoryBadge: String,
    val estimatedTime: String,
    val headline: String,
    val benefits: List<String>,
    val usageSteps: List<String>,
    val coachTip: String,
    val targetRoute: String? = null,
    val primaryCtaLabel: String = "Open Module"
)

object TutorialOverlayCatalog {
    const val CORE_TOUR_KEY = "core_tour"

    val coreTourRouteOrder: List<String> = listOf(
        "dashboard",
        "speaking_lab",
        "ai_mentor",
        "daily_practice",
        "conversation",
        "vocabulary",
        "analytics"
    )

    val coreModuleFirstUseRoutes: Set<String> = setOf(
        "speaking_lab",
        "ai_mentor",
        "daily_practice",
        "conversation",
        "vocabulary",
        "voice_training",
        "public_speaking",
        "analytics"
    )

    fun coreWelcomeTourSteps(): List<TutorialOverlayStep> =
        coreTourRouteOrder.map { route ->
            FeatureTutorialRegistry.forRoute(route).toOverlayStep(
                targetRoute = if (route == "dashboard") null else route,
                primaryCtaLabel = when (route) {
                    "speaking_lab" -> "Open Speaking Lab"
                    "ai_mentor" -> "Open AI Mentor"
                    "daily_practice" -> "Start Daily Practice"
                    "conversation" -> "Try Conversation Sim"
                    "vocabulary" -> "Open Vocabulary"
                    "analytics" -> "View Analytics"
                    else -> "Explore"
                }
            )
        }

    fun singleModuleStep(route: String): TutorialOverlayStep {
        val tutorial = FeatureTutorialRegistry.forRoute(route)
        return tutorial.toOverlayStep(
            targetRoute = null,
            primaryCtaLabel = "Start Practicing"
        )
    }
}

fun FeatureTutorial.toOverlayStep(
    targetRoute: String? = featureKey,
    primaryCtaLabel: String = "Open $title"
): TutorialOverlayStep = TutorialOverlayStep(
    featureKey = featureKey,
    title = title,
    categoryBadge = categoryBadge,
    estimatedTime = estimatedTime,
    headline = summary,
    benefits = whatImproves,
    usageSteps = howToUseSteps,
    coachTip = coachTip,
    targetRoute = targetRoute,
    primaryCtaLabel = primaryCtaLabel
)

private fun iconForFeature(featureKey: String): ImageVector = when (featureKey) {
    "dashboard" -> Icons.Default.Home
    "speaking_lab" -> Icons.Default.Mic
    "ai_mentor" -> Icons.Default.Psychology
    "daily_practice" -> Icons.Default.CheckCircle
    "conversation" -> Icons.Default.RecordVoiceOver
    "vocabulary" -> Icons.AutoMirrored.Filled.MenuBook
    "voice_training", "pronunciation", "articulation_lab" -> Icons.Default.GraphicEq
    "analytics" -> Icons.Default.Analytics
    else -> Icons.Default.AutoAwesome
}

@Composable
fun TutorialOverlay(
    visible: Boolean,
    steps: List<TutorialOverlayStep>,
    tourTitle: String = "Core Features Guided Tour",
    tourSubtitle: String = "Learn what each tool improves and how to use it",
    allowDisableFutureOverlays: Boolean = true,
    initialStepIndex: Int = 0,
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    onComplete: (dontShowAgain: Boolean) -> Unit,
    onTryModule: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty()) return

    val ext = MaterialTheme.speakCraftColors
    val reducedMotion = rememberReducedMotionEnabled()
    var stepIndex by remember(steps) {
        mutableIntStateOf(initialStepIndex.coerceIn(0, steps.lastIndex))
    }
    var dontShowAgain by remember(visible, steps) { mutableStateOf(false) }

    LaunchedEffect(visible, steps) {
        if (visible) {
            stepIndex = initialStepIndex.coerceIn(0, steps.lastIndex)
        }
    }

    BackHandler(enabled = visible) {
        onDismiss(dontShowAgain)
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) fadeIn(tween(0)) else fadeIn(tween(220)) + scaleIn(
            initialScale = 0.96f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = if (reducedMotion) fadeOut(tween(0)) else fadeOut(tween(180)) + scaleOut(targetScale = 0.97f),
        modifier = modifier.fillMaxSize()
    ) {
        val currentStep = steps[stepIndex.coerceIn(0, steps.lastIndex)]
        val isFirstStep = stepIndex == 0
        val isLastStep = stepIndex == steps.lastIndex
        val scrimInteraction = remember { MutableInteractionSource() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tutorial_overlay_root"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = { /* Trap background touches while overlay is open */ }
                    )
            )
            val availableWidth = (maxWidth - SpeakCraftSpacing.lg * 2).coerceAtLeast(240.dp)
            val availableHeight = (maxHeight - SpeakCraftSpacing.md * 2).coerceAtLeast(360.dp)
            val maxCardHeight = (availableHeight * 0.9f).coerceAtLeast(360.dp)
            val isWideOverlay = availableWidth >= 720.dp
            val cardMaxWidth = if (isWideOverlay) 720.dp else 560.dp

            Card(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = SpeakCraftSpacing.lg, vertical = SpeakCraftSpacing.md)
                    .widthIn(max = cardMaxWidth)
                    .fillMaxWidth()
                    .heightIn(max = maxCardHeight)
                    .testTag("tutorial_overlay_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Spotlight Header with subtle linear gradient accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        ext.infoContainer.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(horizontal = SpeakCraftSpacing.lg, vertical = SpeakCraftSpacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = if (steps.size > 1) {
                                            "FIRST-USE TOUR • STEP ${stepIndex + 1} OF ${steps.size}"
                                        } else {
                                            "FIRST-USE SPOTLIGHT"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { onDismiss(dontShowAgain) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("tutorial_overlay_skip_button")
                                ) {
                                    Text(
                                        text = if (steps.size > 1) "Skip Tour" else "Dismiss",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = iconForFeature(currentStep.featureKey),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentStep.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        text = "${currentStep.categoryBadge} • ${currentStep.estimatedTime}  •  $tourTitle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Scrollable Feature Benefits & Usage Instructions
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else {
                                (fadeIn(tween(180)) + slideInHorizontally { it / 12 }) togetherWith
                                    (fadeOut(tween(120)) + slideOutHorizontally { -it / 12 })
                            }
                        },
                        label = "tutorial_overlay_step_content",
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = SpeakCraftSpacing.lg, vertical = SpeakCraftSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = tourSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = step.headline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val benefitsPanel: @Composable (Modifier) -> Unit = { panelModifier ->
                                Surface(
                                    modifier = panelModifier,
                                    shape = MaterialTheme.shapes.medium,
                                    color = ext.successContainer.copy(alpha = 0.48f),
                                    border = BorderStroke(1.dp, ext.success.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null,
                                                tint = ext.success,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Benefits — What You'll Improve",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = ext.success
                                            )
                                        }
                                        step.benefits.forEach { benefit ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = ext.success,
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .size(15.dp)
                                                )
                                                Text(
                                                    text = benefit,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val usagePanel: @Composable (Modifier) -> Unit = { panelModifier ->
                                Column(
                                    modifier = panelModifier,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "How to Use It",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    step.usageSteps.forEachIndexed { idx, instruction ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = instruction,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isWideOverlay) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    benefitsPanel(Modifier.weight(1f))
                                    usagePanel(Modifier.weight(1f))
                                }
                            } else {
                                benefitsPanel(Modifier.fillMaxWidth())
                                usagePanel(Modifier.fillMaxWidth())
                            }

                            // Coach Tip Callout
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ext.warningContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = ext.warning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Coach Tip: ${step.coachTip}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Footer: Step dots, optional module quick-open CTA, "Don't show automatically" option, and Back/Next buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpeakCraftSpacing.lg, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val jumpRoute = currentStep.targetRoute
                        if (onTryModule != null && !jumpRoute.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { onTryModule(jumpRoute) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("tutorial_overlay_try_module_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(currentStep.primaryCtaLabel)
                            }
                        }
                        if (steps.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                steps.forEachIndexed { idx, step ->
                                    val selected = idx == stepIndex
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                            )
                                            .clickable { stepIndex = idx }
                                            .testTag("tutorial_overlay_dot_${step.featureKey}")
                                    )
                                }
                            }
                        }

                        if (allowDisableFutureOverlays) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { dontShowAgain = !dontShowAgain }
                                    .minimumInteractiveComponentSize()
                                    .testTag("tutorial_overlay_dont_show_row"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = dontShowAgain,
                                    onCheckedChange = { dontShowAgain = it },
                                    modifier = Modifier.testTag("tutorial_overlay_dont_show_checkbox")
                                )
                                Text(
                                    text = "Don't show first-use guides automatically (replay anytime in Settings)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isFirstStep) {
                                OutlinedButton(
                                    onClick = { stepIndex = (stepIndex - 1).coerceAtLeast(0) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                        .testTag("tutorial_overlay_prev_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Back")
                                }
                            }

                            Button(
                                onClick = {
                                    if (isLastStep) {
                                        onComplete(dontShowAgain)
                                    } else {
                                        stepIndex = (stepIndex + 1).coerceAtMost(steps.lastIndex)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(if (isFirstStep) 1f else 1.3f)
                                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                                    .testTag("tutorial_overlay_next_button")
                            ) {
                                Text(
                                    text = when {
                                        steps.size == 1 -> "Got It, Let's Practice"
                                        isLastStep -> "Finish Tour"
                                        else -> "Next Feature"
                                    }
                                )
                                if (!isLastStep) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
