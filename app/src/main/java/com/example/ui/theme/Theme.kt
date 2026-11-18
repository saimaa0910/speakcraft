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
    val heroOverlayEnd: Color,
    val onHeroTitle: Color,
    val onHeroSubtitle: Color,
    val onHeroBadge: Color,
    val cameraBackdrop: Color,
    val cameraScrim: Color,
    val recordingPulse: Color,
    val sparklinePointCenter: Color
)

private val LightExtendedColors = SpeakCraftExtendedColors(
    success = EmeraldSuccess,
    onSuccess = CrispPaper,
    successContainer = EmeraldContainerLight,
    warning = AmberSpotlight,
    onWarning = CrispPaper,
    warningContainer = AmberContainerLight,
    info = AcousticTealAccessible,
    infoContainer = TealContainerLight,
    indigo = IndigoAccent,
    indigoContainer = IndigoSoft,
    cardBorderSubtle = SubtleBorderLight,
    studioConsoleSurface = MidnightNavy,
    onStudioConsole = WarmIvory,
    heroOverlayStart = HeroOverlayStart,
    heroOverlayEnd = HeroOverlayEnd,
    onHeroTitle = HeroTitleWhite,
    onHeroSubtitle = HeroSubtitleSlate,
    onHeroBadge = HeroBadgeIvory,
    cameraBackdrop = CameraBackdrop,
    cameraScrim = CameraScrim,
    recordingPulse = CrimsonSoft,
    sparklinePointCenter = CrispPaper
)

private val DarkExtendedColors = SpeakCraftExtendedColors(
    success = EmeraldBright,
    onSuccess = MidnightNavy,
    successContainer = EmeraldContainerDark,
    warning = AmberGoldBright,
    onWarning = MidnightNavy,
    warningContainer = AmberContainerDark,
    info = AcousticTealBright,
    infoContainer = TealContainerDark,
    indigo = IndigoSoft,
    indigoContainer = SlateSurface,
    cardBorderSubtle = SubtleBorderDark,
    studioConsoleSurface = SlateSurface,
    onStudioConsole = WarmIvory,
    heroOverlayStart = HeroOverlayStart,
    heroOverlayEnd = HeroOverlayEnd,
    onHeroTitle = HeroTitleWhite,
    onHeroSubtitle = HeroSubtitleSlate,
    onHeroBadge = HeroBadgeIvory,
    cameraBackdrop = CameraBackdrop,
    cameraScrim = CameraScrim,
    recordingPulse = CrimsonSoft,
    sparklinePointCenter = DeepSlate
)

val LocalSpeakCraftColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.speakCraftColors: SpeakCraftExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSpeakCraftColors.current

private val DarkColorScheme = darkColorScheme(
    primary = AmberGoldBright,
    onPrimary = MidnightNavy,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = AmberContainerLight,
    secondary = AcousticTealBright,
    onSecondary = MidnightNavy,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = TealContainerLight,
    tertiary = IndigoSoft,
    onTertiary = MidnightNavy,
    tertiaryContainer = SlateSurface,
    onTertiaryContainer = IndigoSoft,
    background = MidnightNavy,
    onBackground = WarmIvory,
    surface = DeepSlate,
    onSurface = WarmIvory,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = SlateMutedLight,
    outline = SubtleBorderDark,
    error = CrimsonSoft,
    onError = MidnightNavy,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = CrimsonContainerLight
)

private val LightColorScheme = lightColorScheme(
    primary = AmberSpotlight,
    onPrimary = CrispPaper,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = AmberDarkInk,
    secondary = AcousticTealAccessible,
    onSecondary = CrispPaper,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = TealDarkInk,
    tertiary = IndigoAccent,
    onTertiary = CrispPaper,
    tertiaryContainer = IndigoSoft,
    onTertiaryContainer = MidnightNavy,
    background = WarmIvory,
    onBackground = MidnightNavy,
    surface = CrispPaper,
    onSurface = MidnightNavy,
    surfaceVariant = SoftStone,
    onSurfaceVariant = MutedSlateOnLight,
    outline = SubtleBorderLight,
    error = CrimsonError,
    onError = CrispPaper,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = CrimsonDarkInk
)

val SpeakCraftShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
