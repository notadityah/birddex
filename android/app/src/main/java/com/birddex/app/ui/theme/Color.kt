package com.birddex.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand colors, from frontend/src/assets/main.css.
val BirdGreen = Color(0xFF00D084) // --primary-green
val ForestGreen = Color(0xFF1A3C34) // --forest-green
val Ink = Color(0xFF171717) // --foreground
val LightGray = Color(0xFFF3F4F6) // --light-gray

// Same hue as BirdGreen at lower lightness. BirdGreen on white is only ~2:1 contrast,
// too low for text, so text and filled buttons use BirdGreen40 (~6.5:1 with white).
val BirdGreen40 = Color(0xFF006C43)
val BirdGreen20 = Color(0xFF003820)

// Tailwind palette entries the web app uses (gray, red, amber).
private val Gray50 = Color(0xFFF9FAFB)
private val Gray200 = Color(0xFFE5E7EB)
private val Gray300 = Color(0xFFD1D5DB)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)
private val Gray600 = Color(0xFF4B5563)
private val Gray700 = Color(0xFF374151)
private val Red50 = Color(0xFFFEF2F2)
private val Red100 = Color(0xFFFEE2E2)
private val Red400 = Color(0xFFF87171)
private val Red700 = Color(0xFFB91C1C)
private val Red900 = Color(0xFF7F1D1D)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber300 = Color(0xFFFCD34D)
private val Amber800 = Color(0xFF92400E)
private val Amber900 = Color(0xFF78350F)

internal val LightColors = lightColorScheme(
    primary = BirdGreen40,
    onPrimary = Color.White,
    primaryContainer = BirdGreen,
    onPrimaryContainer = ForestGreen,
    inversePrimary = BirdGreen,
    secondary = ForestGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5E6),
    onSecondaryContainer = ForestGreen,
    tertiary = Amber800,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber800,
    error = Red700,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red900,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = LightGray,
    onSurfaceVariant = Gray600,
    surfaceTint = BirdGreen40,
    inverseSurface = Color(0xFF262626),
    inverseOnSurface = Color(0xFFF5F5F5),
    outline = Gray300,
    outlineVariant = Gray200,
    surfaceDim = Color(0xFFE2E4E7),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Gray50,
    surfaceContainer = LightGray,
    surfaceContainerHigh = Color(0xFFECEEF1),
    surfaceContainerHighest = Gray200,
)

// The web app has no dark mode; this keeps the same brand greens on a dark, slightly
// green-tinted neutral background.
internal val DarkColors = darkColorScheme(
    primary = BirdGreen,
    onPrimary = BirdGreen20,
    primaryContainer = ForestGreen,
    onPrimaryContainer = Color(0xFFA7F3D0),
    inversePrimary = BirdGreen40,
    secondary = Color(0xFFA7C9BC),
    onSecondary = Color(0xFF0F2A23),
    secondaryContainer = Color(0xFF24453C),
    onSecondaryContainer = Color(0xFFC8E8DB),
    tertiary = Amber300,
    onTertiary = Color(0xFF3D2600),
    tertiaryContainer = Amber900,
    onTertiaryContainer = Amber100,
    error = Red400,
    onError = Color(0xFF450A0A),
    errorContainer = Red900,
    onErrorContainer = Red100,
    background = Color(0xFF101412),
    onBackground = Gray200,
    surface = Color(0xFF101412),
    onSurface = Gray200,
    surfaceVariant = Color(0xFF262B29),
    onSurfaceVariant = Gray400,
    surfaceTint = BirdGreen,
    inverseSurface = Gray200,
    inverseOnSurface = Color(0xFF1F2421),
    outline = Gray500,
    outlineVariant = Gray700,
    surfaceDim = Color(0xFF101412),
    surfaceBright = Color(0xFF333A37),
    surfaceContainerLowest = Color(0xFF0B0F0D),
    surfaceContainerLow = Color(0xFF161B19),
    surfaceContainer = Color(0xFF1A201D),
    surfaceContainerHigh = Color(0xFF222926),
    surfaceContainerHighest = Color(0xFF2C3330),
)
