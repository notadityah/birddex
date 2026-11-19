package com.birddex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * App-wide theme. Brand colors only: Material You dynamic color is deliberately off so
 * the app always looks like BirdDex. Shapes use the Material 3 defaults, which already
 * match the web's Tailwind radii (rounded-lg = 8dp small, rounded-xl = 12dp medium,
 * rounded-2xl = 16dp large).
 */
@Composable
fun BirdDexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BirdDexTypography,
        content = content,
    )
}
