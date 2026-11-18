package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    background = MidnightNavy,
    onBackground = WarmIvory,
    surface = DeepSlate,
    onSurface = WarmIvory,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = SubtleBorderDark,
    error = Color(0xFFF87171),
    onError = MidnightNavy,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = CrimsonContainerLight
)

private val LightColorScheme = lightColorScheme(
    primary = AmberSpotlight,
    onPrimary = Color.White,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = AcousticTeal,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = WarmIvory,
    onBackground = MidnightNavy,
    surface = CrispPaper,
    onSurface = MidnightNavy,
    surfaceVariant = SoftStone,
    onSurfaceVariant = Color(0xFF475569),
    outline = SubtleBorderLight,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = Color(0xFF991B1B)
)

val SpeakCraftShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SpeakCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpeakCraftTypography,
        shapes = SpeakCraftShapes,
        content = content
    )
}
