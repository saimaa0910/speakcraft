package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SpeakCraftExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val indigo: Color,
    val indigoContainer: Color,
    val cardBorderSubtle: Color,
    val studioConsoleSurface: Color,
    val onStudioConsole: Color,
    val heroOverlayStart: Color,
    val heroOverlayMid: Color,
    val heroOverlayEnd: Color,
    val onHeroTitle: Color,
    val onHeroSubtitle: Color,
    val onHeroBadge: Color,
    val cameraBackdrop: Color,
    val cameraScrim: Color,
    val recordingPulse: Color,
    val sparklinePointCenter: Color,
    // Midnight Indigo Sidebar & Luminous Violet Gradients
    val sidebarTop: Color,
    val sidebarBottom: Color,
    val sidebarActiveStart: Color,
    val sidebarActiveEnd: Color,
    val sidebarTextIdle: Color,
    val sidebarWaveAccent: Color,
    val streakSurface: Color,
    // 8 Pastel Quick-Access & Metric Swatch Pairs
    val pastelLavenderBg: Color,
    val pastelLavenderInk: Color,
    val pastelSkyBg: Color,
    val pastelSkyInk: Color,
    val pastelMintBg: Color,
    val pastelMintInk: Color,
    val pastelPeachBg: Color,
    val pastelPeachInk: Color,
    val pastelRoseBg: Color,
    val pastelRoseInk: Color,
    val pastelGoldBg: Color,
    val pastelGoldInk: Color,
    val pastelPeriwinkleBg: Color,
    val pastelPeriwinkleInk: Color,
    val pastelCoralBg: Color,
    val pastelCoralInk: Color
)

private val LightExtendedColors = SpeakCraftExtendedColors(
    success = MintSuccess,
    onSuccess = CrispWhite,
    successContainer = MintContainerLight,
    warning = WarmAmber,
    onWarning = CrispWhite,
    warningContainer = WarmAmberContainerLight,
    info = SkyAccent,
    infoContainer = SkyContainerLight,
    indigo = RoyalViolet,
    indigoContainer = VioletContainerLight,
    cardBorderSubtle = SubtleBorderLight,
    studioConsoleSurface = SidebarIndigoTop,
    onStudioConsole = CrispWhite,
    heroOverlayStart = HeroGradientStart,
    heroOverlayMid = HeroGradientMid,
    heroOverlayEnd = HeroGradientEnd,
    onHeroTitle = HeroTitleWhite,
    onHeroSubtitle = HeroSubtitleLight,
    onHeroBadge = HeroBadgeGlow,
    cameraBackdrop = CameraBackdrop,
    cameraScrim = CameraScrim,
    recordingPulse = RoseSoft,
    sparklinePointCenter = CrispWhite,
    sidebarTop = SidebarIndigoTop,
    sidebarBottom = SidebarIndigoBottom,
    sidebarActiveStart = SidebarActiveStart,
    sidebarActiveEnd = SidebarActiveEnd,
    sidebarTextIdle = SidebarTextIdle,
    sidebarWaveAccent = SidebarWaveAccent,
    streakSurface = StreakSurfaceLight,
    pastelLavenderBg = PastelLavenderBgLight,
    pastelLavenderInk = PastelLavenderInkLight,
    pastelSkyBg = PastelSkyBgLight,
    pastelSkyInk = PastelSkyInkLight,
    pastelMintBg = PastelMintBgLight,
    pastelMintInk = PastelMintInkLight,
    pastelPeachBg = PastelPeachBgLight,
    pastelPeachInk = PastelPeachInkLight,
    pastelRoseBg = PastelRoseBgLight,
    pastelRoseInk = PastelRoseInkLight,
    pastelGoldBg = PastelGoldBgLight,
    pastelGoldInk = PastelGoldInkLight,
    pastelPeriwinkleBg = PastelPeriwinkleBgLight,
    pastelPeriwinkleInk = PastelPeriwinkleInkLight,
    pastelCoralBg = PastelCoralBgLight,
    pastelCoralInk = PastelCoralInkLight
)

private val DarkExtendedColors = SpeakCraftExtendedColors(
    success = MintBright,
    onSuccess = MidnightIndigoBg,
    successContainer = MintContainerDark,
    warning = WarmAmberBright,
    onWarning = MidnightIndigoBg,
    warningContainer = WarmAmberContainerDark,
    info = SkyBright,
    infoContainer = SkyContainerDark,
    indigo = LuminousViolet,
    indigoContainer = VioletContainerDark,
    cardBorderSubtle = SubtleBorderDark,
    studioConsoleSurface = ElevatedIndigoSurface,
    onStudioConsole = SoftWhiteOnDark,
    heroOverlayStart = HeroGradientStart,
    heroOverlayMid = HeroGradientMid,
    heroOverlayEnd = HeroGradientEnd,
    onHeroTitle = HeroTitleWhite,
    onHeroSubtitle = HeroSubtitleLight,
    onHeroBadge = HeroBadgeGlow,
    cameraBackdrop = CameraBackdrop,
    cameraScrim = CameraScrim,
    recordingPulse = RoseSoft,
    sparklinePointCenter = ElevatedIndigoSurface,
    sidebarTop = SidebarIndigoTop,
    sidebarBottom = SidebarIndigoBottom,
    sidebarActiveStart = SidebarActiveStart,
    sidebarActiveEnd = SidebarActiveEnd,
    sidebarTextIdle = SidebarTextIdle,
    sidebarWaveAccent = SidebarWaveAccent,
    streakSurface = StreakSurfaceDark,
    pastelLavenderBg = PastelLavenderBgDark,
    pastelLavenderInk = PastelLavenderInkDark,
    pastelSkyBg = PastelSkyBgDark,
    pastelSkyInk = PastelSkyInkDark,
    pastelMintBg = PastelMintBgDark,
    pastelMintInk = PastelMintInkDark,
    pastelPeachBg = PastelPeachBgDark,
    pastelPeachInk = PastelPeachInkDark,
    pastelRoseBg = PastelRoseBgDark,
    pastelRoseInk = PastelRoseInkDark,
    pastelGoldBg = PastelGoldBgDark,
    pastelGoldInk = PastelGoldInkDark,
    pastelPeriwinkleBg = PastelPeriwinkleBgDark,
    pastelPeriwinkleInk = PastelPeriwinkleInkDark,
    pastelCoralBg = PastelCoralBgDark,
    pastelCoralInk = PastelCoralInkDark
)

val LocalSpeakCraftColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.speakCraftColors: SpeakCraftExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSpeakCraftColors.current

private val DarkColorScheme = darkColorScheme(
    primary = LuminousViolet,
    onPrimary = MidnightIndigoBg,
    primaryContainer = VioletContainerDark,
    onPrimaryContainer = VioletContainerLight,
    secondary = SkyBright,
    onSecondary = MidnightIndigoBg,
    secondaryContainer = SkyContainerDark,
    onSecondaryContainer = SkyContainerLight,
    tertiary = MintBright,
    onTertiary = MidnightIndigoBg,
    tertiaryContainer = MintContainerDark,
    onTertiaryContainer = MintContainerLight,
    background = MidnightIndigoBg,
    onBackground = SoftWhiteOnDark,
    surface = ElevatedIndigoSurface,
    onSurface = SoftWhiteOnDark,
    surfaceVariant = IndigoSurfaceVariant,
    onSurfaceVariant = MutedLavenderOnDark,
    outline = SubtleBorderDark,
    error = RoseSoft,
    onError = MidnightIndigoBg,
    errorContainer = RoseContainerDark,
    onErrorContainer = RoseContainerLight
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalViolet,
    onPrimary = CrispWhite,
    primaryContainer = VioletContainerLight,
    onPrimaryContainer = VioletInkDark,
    secondary = SkyAccent,
    onSecondary = CrispWhite,
    secondaryContainer = SkyContainerLight,
    onSecondaryContainer = SkyDarkInk,
    tertiary = MintSuccess,
    onTertiary = CrispWhite,
    tertiaryContainer = MintContainerLight,
    onTertiaryContainer = DeepIndigoInk,
    background = StudioCanvasLight,
    onBackground = DeepIndigoInk,
    surface = CrispWhite,
    onSurface = DeepIndigoInk,
    surfaceVariant = SoftLavenderSurface,
    onSurfaceVariant = CoolSlateOnLight,
    outline = SubtleBorderLight,
    error = RoseCrimson,
    onError = CrispWhite,
    errorContainer = RoseContainerLight,
    onErrorContainer = RoseDarkInk
)

val SpeakCraftShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SpeakCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalSpeakCraftColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpeakCraftTypography,
            shapes = SpeakCraftShapes,
            content = content
        )
    }
}
