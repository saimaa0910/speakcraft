package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Bundled Google Fonts installed via font-util are single-weight (400) static TTF files.
 * Registering only FontWeight.Normal lets Android/Compose synthesize crisp SemiBold/Bold
 * strokes instead of clamping headings to weight 400.
 */
val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val DmSansFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

val SpeakCraftTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.15).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.35.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
)
