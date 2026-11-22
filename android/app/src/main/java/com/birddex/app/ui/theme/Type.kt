package com.birddex.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.birddex.app.R

// Inter, same as the web app (main.css --font-sans). res/font/inter.ttf is the variable
// font from Google Fonts (license: assets/licenses/inter-OFL.txt), so one file serves
// every weight via the "wght" axis.
private val InterWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

val BirdDexFontFamily = FontFamily(
    InterWeights.map { weight ->
        Font(
            resId = R.font.inter,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

private val Base = Typography()

// Letter spacing 0 like the web: Material's defaults (up to 0.5sp) are tuned for Roboto
// and make Inter look too loose.
private fun TextStyle.brand(weight: FontWeight) =
    copy(fontFamily = BirdDexFontFamily, fontWeight = weight, letterSpacing = 0.sp)

// Material 3 sizes (body 16sp / 24sp line height, same as the web's 16px / 1.5).
// Headings are bolder to match the web's font-bold / font-semibold headings.
val BirdDexTypography = Typography(
    displayLarge = Base.displayLarge.brand(FontWeight.Bold),
    displayMedium = Base.displayMedium.brand(FontWeight.Bold),
    displaySmall = Base.displaySmall.brand(FontWeight.Bold),
    headlineLarge = Base.headlineLarge.brand(FontWeight.Bold),
    headlineMedium = Base.headlineMedium.brand(FontWeight.Bold),
    headlineSmall = Base.headlineSmall.brand(FontWeight.SemiBold),
    titleLarge = Base.titleLarge.brand(FontWeight.SemiBold),
    titleMedium = Base.titleMedium.brand(FontWeight.SemiBold),
    titleSmall = Base.titleSmall.brand(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.brand(FontWeight.Normal),
    bodyMedium = Base.bodyMedium.brand(FontWeight.Normal),
    bodySmall = Base.bodySmall.brand(FontWeight.Normal),
    labelLarge = Base.labelLarge.brand(FontWeight.Medium),
    labelMedium = Base.labelMedium.brand(FontWeight.Medium),
    labelSmall = Base.labelSmall.brand(FontWeight.Medium),
)
