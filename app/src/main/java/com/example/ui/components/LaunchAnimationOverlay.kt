package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.R
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class LaunchCadence(val durationMs: Int) {
    FIRST_LAUNCH(2900),
    RETURNING_LAUNCH(2100),
    REDUCED_MOTION(600)
}

enum class LaunchStage {
    INITIAL_APPEARANCE,
    BRAND_REVEAL,
    IDENTITY_REVEAL,
    FINAL_TRANSITION,
    COMPLETED
}

data class LaunchVisualMetrics(
    val stage: LaunchStage,
    val backgroundGlowAlpha: Float,
    val studioDialAlpha: Float,
    val emblemAlpha: Float,
    val emblemScale: Float,
    val emblemArcSweep: Float,
    val sheenProgress: Float,
    val ringExpansion: Float,
    val ringAlpha: Float,
    val orbitalAngleDeg: Float,
    val waveReveal: Float,
    val eyebrowAlpha: Float,
    val wordmarkAlpha: Float,
    val wordmarkTranslationDp: Float,
    val wordmarkLetterSpacingSp: Float,
    val dividerSweep: Float,
    val taglineAlpha: Float,
    val taglineTranslationDp: Float,
    val exitAlpha: Float,
    val exitScale: Float
)

object LaunchTimelineSpec {
    private val StudioSpringEasing = CubicBezierEasing(0.18f, 1.10f, 0.30f, 1f)
    private val SmoothSilkEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1f)

    fun interval(progress: Float, start: Float, end: Float): Float {
        if (end <= start) return if (progress >= end) 1f else 0f
        return ((progress - start) / (end - start)).coerceIn(0f, 1f)
    }

    fun stageForProgress(progress: Float, cadence: LaunchCadence): LaunchStage {
        val p = progress.coerceIn(0f, 1f)
        if (p >= 1f) return LaunchStage.COMPLETED
        if (cadence == LaunchCadence.REDUCED_MOTION) {
            return if (p < 0.72f) LaunchStage.IDENTITY_REVEAL else LaunchStage.FINAL_TRANSITION
        }
        return when {
            p < 0.14f -> LaunchStage.INITIAL_APPEARANCE
            p < 0.44f -> LaunchStage.BRAND_REVEAL
            p < 0.80f -> LaunchStage.IDENTITY_REVEAL
            else -> LaunchStage.FINAL_TRANSITION
        }
    }

    fun evaluate(progress: Float, cadence: LaunchCadence): LaunchVisualMetrics {
        val p = progress.coerceIn(0f, 1f)
        if (cadence == LaunchCadence.REDUCED_MOTION) {
            val enterT = LinearOutSlowInEasing.transform(interval(p, 0.0f, 0.25f))
            val exitT = LinearOutSlowInEasing.transform(interval(p, 0.72f, 1.0f))
            return LaunchVisualMetrics(
                stage = stageForProgress(p, cadence),
                backgroundGlowAlpha = enterT,
                studioDialAlpha = enterT * 0.55f,
                emblemAlpha = enterT,
                emblemScale = 1f,
                emblemArcSweep = 1f,
                sheenProgress = -1f,
                ringExpansion = 1f,
                ringAlpha = 0.35f * enterT,
                orbitalAngleDeg = 0f,
                waveReveal = 1f,
                eyebrowAlpha = enterT,
                wordmarkAlpha = enterT,
                wordmarkTranslationDp = 0f,
                wordmarkLetterSpacingSp = -0.5f,
                dividerSweep = 1f,
                taglineAlpha = enterT,
                taglineTranslationDp = 0f,
                exitAlpha = 1f - exitT,
                exitScale = 1f
            )
        }

        // Stage 1 (0.00 -> 0.18): Studio atmosphere & aperture dial fade-in
        val glowT = LinearOutSlowInEasing.transform(interval(p, 0.0f, 0.20f))
        val dialT = FastOutSlowInEasing.transform(interval(p, 0.04f, 0.32f))

        // Stage 2 (0.10 -> 0.56): Emblem medallion lift, triple arc draw, orbital rings, harmonic wave
        val emblemAlphaT = FastOutSlowInEasing.transform(interval(p, 0.10f, 0.34f))
        val emblemScaleT = StudioSpringEasing.transform(interval(p, 0.10f, 0.44f))
        val arcSweepT = SmoothSilkEasing.transform(interval(p, 0.16f, 0.48f))
        val sheenT = SmoothSilkEasing.transform(interval(p, 0.30f, 0.62f))
        val ringT = SmoothSilkEasing.transform(interval(p, 0.12f, 0.56f))
        val orbitalDeg = 54f * SmoothSilkEasing.transform(interval(p, 0.12f, 0.88f))
        val waveT = SmoothSilkEasing.transform(interval(p, 0.22f, 0.58f))

        // Stage 3 (0.42 -> 0.80): Staggered eyebrow, weighted wordmark tracking settle, rule & tagline
        val eyebrowT = LinearOutSlowInEasing.transform(interval(p, 0.42f, 0.60f))
        val wordmarkT = SmoothSilkEasing.transform(interval(p, 0.46f, 0.72f))
        val dividerT = SmoothSilkEasing.transform(interval(p, 0.54f, 0.76f))
        val taglineT = LinearOutSlowInEasing.transform(interval(p, 0.60f, 0.80f))

        // Stage 4 (0.80 -> 1.00): Editorial hold (0.80..0.88) followed by calm iris dissolve (0.88..1.00)
        val exitT = FastOutSlowInEasing.transform(interval(p, 0.88f, 1.0f))

        val ringAlpha = when {
            ringT <= 0f -> 0f
            ringT < 0.42f -> (ringT / 0.42f) * 0.72f
            else -> 0.72f - ((ringT - 0.42f) / 0.58f) * 0.34f
        }

        val sheenPosition = if (p in 0.30f..0.64f) sheenT else -1f

        return LaunchVisualMetrics(
            stage = stageForProgress(p, cadence),
            backgroundGlowAlpha = glowT,
            studioDialAlpha = dialT,
            emblemAlpha = emblemAlphaT,
            emblemScale = 0.78f + 0.22f * emblemScaleT,
            emblemArcSweep = arcSweepT,
            sheenProgress = sheenPosition,
            ringExpansion = 0.68f + 0.36f * ringT,
            ringAlpha = ringAlpha.coerceIn(0f, 1f),
            orbitalAngleDeg = orbitalDeg,
            waveReveal = waveT,
            eyebrowAlpha = eyebrowT,
            wordmarkAlpha = wordmarkT,
            wordmarkTranslationDp = 18f * (1f - wordmarkT),
            wordmarkLetterSpacingSp = 2.4f - (2.9f * wordmarkT),
            dividerSweep = dividerT,
            taglineAlpha = taglineT,
            taglineTranslationDp = 12f * (1f - taglineT),
            exitAlpha = 1f - exitT,
            exitScale = 1f + 0.04f * exitT
        )
    }
}

/**
 * Activity-scoped ViewModel retaining launch animation completion across configuration changes
 * (screen rotation, multi-window resize, theme changes) and background-to-foreground resumes.
 */
class LaunchExperienceViewModel : ViewModel() {
    private val _isOverlayVisible = MutableStateFlow(true)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    private val _latchedCadence = MutableStateFlow<LaunchCadence?>(null)
    val latchedCadence: StateFlow<LaunchCadence?> = _latchedCadence.asStateFlow()

    private val _currentStage = MutableStateFlow(LaunchStage.INITIAL_APPEARANCE)
    val currentStage: StateFlow<LaunchStage> = _currentStage.asStateFlow()

    fun latchCadenceIfNeeded(isFirstLaunch: Boolean, reducedMotion: Boolean) {
        if (!_isOverlayVisible.value || _latchedCadence.value != null) return
        _latchedCadence.value = when {
            reducedMotion -> LaunchCadence.REDUCED_MOTION
            isFirstLaunch -> LaunchCadence.FIRST_LAUNCH
            else -> LaunchCadence.RETURNING_LAUNCH
        }
    }

    fun updateStage(stage: LaunchStage) {
        if (_currentStage.value != stage) {
            _currentStage.value = stage
        }
    }

    fun markCompleted() {
        _currentStage.value = LaunchStage.COMPLETED
        _isOverlayVisible.value = false
    }
}

@Composable
fun SpeakCraftLaunchOverlay(
    visible: Boolean,
    cadence: LaunchCadence,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotionEnabled(),
    forceVectorFallback: Boolean = false,
    onStageChanged: (LaunchStage) -> Unit = {}
) {
    if (!visible) return

    val effectiveCadence = if (reducedMotion) LaunchCadence.REDUCED_MOTION else cadence
    val progress = remember(effectiveCadence) { Animatable(0f) }

    LaunchedEffect(visible, effectiveCadence) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = effectiveCadence.durationMs,
                easing = LinearEasing
            )
        )
        onStageChanged(LaunchStage.COMPLETED)
        onFinished()
    }

    val metrics = LaunchTimelineSpec.evaluate(progress.value, effectiveCadence)
    LaunchedEffect(metrics.stage) {
        onStageChanged(metrics.stage)
    }

    val ext = MaterialTheme.speakCraftColors
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseBackground = MaterialTheme.colorScheme.background
    val primaryAccent = MaterialTheme.colorScheme.primary
    val secondaryAccent = MaterialTheme.colorScheme.secondary
    val glowPrimary = if (darkTheme) {
        primaryAccent.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    }
    val glowSecondary = if (darkTheme) {
        secondaryAccent.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
    }
    val dialStrokeColor = MaterialTheme.colorScheme.outline.copy(alpha = if (darkTheme) 0.28f else 0.45f)
    val onBackground = MaterialTheme.colorScheme.onBackground
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    val ambientPhase = if (effectiveCadence == LaunchCadence.REDUCED_MOTION) {
        0.5f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "launch_studio_ambience")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "launch_harmonic_phase"
        )
        phase
    }
    val breathScale = if (effectiveCadence == LaunchCadence.REDUCED_MOTION) {
        1f
    } else {
        1f + 0.055f * sin(ambientPhase)
    }

    val density = LocalDensity.current
    val logoDescription = stringResource(R.string.launch_logo_content_description)
    val studioEyebrow = stringResource(R.string.launch_studio_eyebrow)
    val appTitle = stringResource(R.string.app_name)
    val tagline = stringResource(R.string.launch_tagline)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = metrics.exitAlpha
            }
            .background(baseBackground)
            .semantics { contentDescription = logoDescription }
            .testTag("launch_animation_overlay"),
        contentAlignment = Alignment.Center
    ) {
        val minDimension = minOf(maxWidth, maxHeight)
        val emblemSize = when {
            minDimension < 360.dp -> 104.dp
            minDimension >= 600.dp -> 148.dp
            else -> 124.dp
        }
        val ringCanvasSize = emblemSize * 1.92f
        val waveWidth = (maxWidth * 0.56f).coerceIn(176.dp, 280.dp)
        val isCompactVertical = maxHeight < 520.dp

        // Stage 1: Editorial Studio volumetric lighting + precision acoustic compass backdrop
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = metrics.backgroundGlowAlpha
                }
        ) {
            val topCenter = Offset(size.width * 0.5f, size.height * 0.34f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowPrimary, Color.Transparent),
                    center = topCenter,
                    radius = size.maxDimension * 0.58f * breathScale
                ),
                center = topCenter,
                radius = size.maxDimension * 0.58f * breathScale
            )

            val bottomAccent = Offset(size.width * 0.66f, size.height * 0.72f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowSecondary, Color.Transparent),
                    center = bottomAccent,
                    radius = size.maxDimension * 0.46f
                ),
                center = bottomAccent,
                radius = size.maxDimension * 0.46f
            )

            // Subtle architectural dial ticks anchored near center-top composition
            if (metrics.studioDialAlpha > 0f) {
                val dialCenter = Offset(size.width * 0.5f, size.height * 0.42f)
                val dialRadius = size.minDimension * 0.42f
                val tickCount = 36
                val outerTick = dialRadius + 6.dp.toPx()
                for (i in 0 until tickCount) {
                    val angleRad = (i * (360f / tickCount) + metrics.orbitalAngleDeg * 0.25f) * (PI / 180.0)
                    val isCardinal = i % 3 == 0
                    val innerR = if (isCardinal) dialRadius - 5.dp.toPx() else dialRadius
                    val start = Offset(
                        x = dialCenter.x + (innerR * cos(angleRad)).toFloat(),
                        y = dialCenter.y + (innerR * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        x = dialCenter.x + (outerTick * cos(angleRad)).toFloat(),
                        y = dialCenter.y + (outerTick * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = dialStrokeColor.copy(alpha = metrics.studioDialAlpha * if (isCardinal) 0.55f else 0.25f),
                        start = start,
                        end = end,
                        strokeWidth = if (isCardinal) 1.5.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = SpeakCraftSpacing.xl, vertical = SpeakCraftSpacing.lg)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = metrics.exitScale
                    scaleY = metrics.exitScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stage 2: Native Acoustic Aperture + Sculpted Compose Crest
            Box(
                modifier = Modifier.size(ringCanvasSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseR = (emblemSize.toPx() / 2f) * 1.16f
                    val outerR = baseR * 1.46f * metrics.ringExpansion
                    val innerR = baseR * 1.22f * (0.86f + 0.14f * metrics.ringExpansion)

                    if (metrics.ringAlpha > 0f) {
                        // Outer counter-rotating segmented orbital arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    secondaryAccent.copy(alpha = metrics.ringAlpha * 0.85f),
                                    primaryAccent.copy(alpha = metrics.ringAlpha * 0.45f),
                                    Color.Transparent
                                ),
                                center = center
                            ),
                            startAngle = -125f + metrics.orbitalAngleDeg,
                            sweepAngle = 225f,
                            useCenter = false,
                            topLeft = Offset(center.x - outerR, center.y - outerR),
                            size = Size(outerR * 2f, outerR * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Inner opposing orbital arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = metrics.ringAlpha * 0.95f),
                                    secondaryAccent.copy(alpha = metrics.ringAlpha * 0.55f),
                                    Color.Transparent
                                ),
                                center = center
                            ),
                            startAngle = 35f - metrics.orbitalAngleDeg * 1.35f,
                            sweepAngle = 245f,
                            useCenter = false,
                            topLeft = Offset(center.x - innerR, center.y - innerR),
                            size = Size(innerR * 2f, innerR * 2f),
                            style = Stroke(width = 2.25.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Luminous orbital satellite bead on outer track
                        val nodeAngleRad = (-125f + metrics.orbitalAngleDeg + 225f) * (PI / 180.0)
                        val nodePos = Offset(
                            x = center.x + (outerR * cos(nodeAngleRad)).toFloat(),
                            y = center.y + (outerR * sin(nodeAngleRad)).toFloat()
                        )
                        drawCircle(
                            color = primaryAccent.copy(alpha = metrics.ringAlpha),
                            radius = 3.5.dp.toPx(),
                            center = nodePos
                        )
                    }
                }

                // Pure native Jetpack Compose medallion centerpiece
                Surface(
                    modifier = Modifier
                        .size(emblemSize)
                        .graphicsLayer {
                            alpha = metrics.emblemAlpha
                            scaleX = metrics.emblemScale
                            scaleY = metrics.emblemScale
                        }
                        .testTag("launch_brand_emblem"),
                    shape = CircleShape,
                    color = ext.heroOverlayEnd,
                    shadowElevation = if (darkTheme) 14.dp else 10.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SpeakCraftNativeStudioCrest(
                            primaryColor = primaryAccent,
                            secondaryColor = secondaryAccent,
                            highlightColor = ext.onHeroTitle,
                            arcSweepFraction = metrics.emblemArcSweep,
                            waveReveal = metrics.waveReveal,
                            sheenProgress = metrics.sheenProgress,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (forceVectorFallback) {
                                        Modifier.testTag("launch_vector_fallback_crest")
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (isCompactVertical) SpeakCraftSpacing.sm else SpeakCraftSpacing.md))

            // Stage 2B: Living Harmonic Voice Equalizer Ribbon
            Canvas(
                modifier = Modifier
                    .size(width = waveWidth, height = 34.dp)
                    .graphicsLayer { alpha = metrics.emblemAlpha }
                    .testTag("launch_sound_wave")
            ) {
                val barCount = 15
                val barWidth = 4.5.dp.toPx()
                val gap = (size.width - barCount * barWidth) / (barCount - 1).coerceAtLeast(1)
                val centerIndex = (barCount - 1) / 2f
                val envelopeWeights = floatArrayOf(
                    0.18f, 0.28f, 0.42f, 0.62f, 0.78f, 0.92f, 0.98f,
                    1.0f,
                    0.98f, 0.92f, 0.78f, 0.62f, 0.42f, 0.28f, 0.18f
                )

                for (i in 0 until barCount) {
                    val normIndex = i / (barCount - 1).toFloat()
                    val distFromCenter = kotlin.math.abs(i - centerIndex) / centerIndex
                    val barReveal = ((metrics.waveReveal - distFromCenter * 0.42f) / 0.58f).coerceIn(0f, 1f)
                    val harmonic = if (effectiveCadence == LaunchCadence.REDUCED_MOTION) {
                        1f
                    } else {
                        val primaryWave = sin((ambientPhase * 1.4f + i * 0.55f).toDouble()).toFloat()
                        val overtone = cos((ambientPhase * 0.9f - i * 0.35f).toDouble()).toFloat()
                        0.76f + 0.16f * primaryWave + 0.08f * overtone
                    }
                    val targetHeight = (size.height * envelopeWeights[i] * barReveal * harmonic)
                        .coerceAtLeast(3.dp.toPx())
                    val x = i * (barWidth + gap)
                    val top = (size.height - targetHeight) / 2f
                    val barColor = lerp(primaryAccent, secondaryAccent, normIndex)
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.92f),
                        topLeft = Offset(x, top),
                        size = Size(barWidth, targetHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(Modifier.height(if (isCompactVertical) SpeakCraftSpacing.md else SpeakCraftSpacing.lg))

            // Stage 3A: Studio Eyebrow
            Text(
                text = studioEyebrow,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.2.sp
                ),
                color = primaryAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = metrics.eyebrowAlpha
                }
            )

            Spacer(Modifier.height(SpeakCraftSpacing.xs))

            // Stage 3B: Weighted Wordmark Reveal with letter-spacing settle
            Text(
                text = appTitle,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = metrics.wordmarkLetterSpacingSp.sp
                ),
                color = onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = metrics.wordmarkAlpha
                        translationY = with(density) { metrics.wordmarkTranslationDp.dp.toPx() }
                    }
                    .testTag("launch_brand_wordmark")
            )

            Spacer(Modifier.height(SpeakCraftSpacing.sm))

            // Stage 3C: Precision Studio Horizon Rule with Center Jewel Node
            Canvas(
                modifier = Modifier
                    .size(width = 112.dp, height = 8.dp)
                    .graphicsLayer { alpha = metrics.dividerSweep }
            ) {
                val sweptWidth = size.width * metrics.dividerSweep
                val startX = (size.width - sweptWidth) / 2f
                val midY = size.height / 2f
                if (sweptWidth > 2f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = 0.25f),
                                primaryAccent,
                                secondaryAccent,
                                secondaryAccent.copy(alpha = 0.25f)
                            )
                        ),
                        topLeft = Offset(startX, midY - 1.25.dp.toPx()),
                        size = Size(sweptWidth, 2.5.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawCircle(
                        color = primaryAccent,
                        radius = 3.2.dp.toPx() * metrics.dividerSweep,
                        center = Offset(size.width / 2f, midY)
                    )
                }
            }

            Spacer(Modifier.height(SpeakCraftSpacing.sm))

            // Stage 3D: Brand Tagline
            Text(
                text = tagline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.25.sp
                ),
                color = subtitleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = metrics.taglineAlpha
                        translationY = with(density) { metrics.taglineTranslationDp.dp.toPx() }
                    }
                    .testTag("launch_brand_tagline")
            )
        }
    }
}

@Composable
private fun SpeakCraftNativeStudioCrest(
    primaryColor: Color,
    secondaryColor: Color,
    highlightColor: Color,
    arcSweepFraction: Float,
    waveReveal: Float,
    sheenProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        // Subtle inner radial stage depth inside medallion
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.16f),
                    secondaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.92f
            ),
            radius = radius,
            center = center
        )

        // Outer sweep-gradient bezel
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    primaryColor,
                    secondaryColor,
                    primaryColor.copy(alpha = 0.85f),
                    primaryColor
                ),
                center = center
            ),
            radius = radius - 1.5.dp.toPx(),
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Concentric animated voice propagation arcs (left-to-right directional acoustic motif)
        val arcSpecs = listOf(
            Triple(0.70f, primaryColor.copy(alpha = 0.92f), 3.4.dp),
            Triple(0.50f, highlightColor.copy(alpha = 0.88f), 3.0.dp),
            Triple(0.32f, secondaryColor.copy(alpha = 0.95f), 2.8.dp)
        )
        arcSpecs.forEachIndexed { idx, (scaleFrac, arcColor, strokeDp) ->
            val localProgress = ((arcSweepFraction - idx * 0.12f) / (1f - idx * 0.12f)).coerceIn(0f, 1f)
            if (localProgress > 0f) {
                val r = radius * scaleFrac
                val targetSweep = 112f * localProgress
                val startAngle = 180f - targetSweep / 2f
                drawArc(
                    color = arcColor,
                    startAngle = startAngle,
                    sweepAngle = targetSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 0.85f - radius * 0.10f, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeDp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Precision 5-bar central acoustic crest shifted slightly right of wave origin
        val barWidth = (size.minDimension * 0.042f).coerceAtLeast(3.dp.toPx())
        val gap = barWidth * 1.15f
        val barHeights = floatArrayOf(0.22f, 0.42f, 0.62f, 0.38f, 0.24f)
        val startX = center.x - barWidth * 0.8f
        barHeights.forEachIndexed { idx, frac ->
            val localBar = ((waveReveal - idx * 0.07f) / 0.65f).coerceIn(0f, 1f)
            val h = (size.height * frac * localBar).coerceAtLeast(barWidth)
            val x = startX + idx * (barWidth + gap)
            val color = when (idx) {
                2 -> primaryColor
                1, 3 -> highlightColor.copy(alpha = 0.94f)
                else -> secondaryColor
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, center.y - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        // Diagonal specular glass sheen sweep once emblem settles
        if (sheenProgress in 0f..1f) {
            val offset = -radius * 2.2f + sheenProgress * radius * 4.4f
            val band = radius * 0.48f
            val path = Path().apply {
                moveTo(center.x + offset - band, 0f)
                lineTo(center.x + offset + band, 0f)
                lineTo(center.x + offset - band + size.height * 0.55f, size.height)
                lineTo(center.x + offset - band * 2f + size.height * 0.55f, size.height)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        highlightColor.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )
        }
    }
}
