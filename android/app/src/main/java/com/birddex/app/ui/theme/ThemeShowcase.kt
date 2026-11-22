package com.birddex.app.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Sample of the theme's colors, type and components, for the light/dark previews below. */
@Composable
fun ThemeShowcase(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("BirdDex", style = MaterialTheme.typography.headlineLarge, color = colors.secondary)
        Text(
            "Identify Australian birds, log your sightings, complete your dex.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = {}) { Text("Detect bird") }
            FilledTonalButton(onClick = {}) { Text("Save") }
            OutlinedButton(onClick = {}) { Text("Gallery") }
            TextButton(onClick = {}) { Text("Skip") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Rainbow Lorikeet",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    RarityPill("Rare")
                }
                Text(
                    "Trichoglossus moluccanus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Text("Confidence 94.2%", style = MaterialTheme.typography.labelLarge, color = colors.primary)
            }
        }

        var shareInGallery by remember { mutableStateOf(true) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Share in gallery", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = shareInGallery, onCheckedChange = { shareInGallery = it })
        }

        Text("Couldn't reach BirdDex. Check your connection.", color = colors.error)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Swatch("primary", colors.primary, colors.onPrimary)
            Swatch("primaryC", colors.primaryContainer, colors.onPrimaryContainer)
            Swatch("secondary", colors.secondary, colors.onSecondary)
            Swatch("tertiaryC", colors.tertiaryContainer, colors.onTertiaryContainer)
            Swatch("error", colors.error, colors.onError)
            Swatch("surfaceV", colors.surfaceVariant, colors.onSurfaceVariant)
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun RarityPill(label: String) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.extraLarge) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Swatch(name: String, fill: Color, onFill: Color) {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 48.dp)
            .background(fill, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = onFill)
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ThemeShowcaseLightPreview() {
    BirdDexTheme(darkTheme = false) { Surface { ThemeShowcase() } }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeShowcaseDarkPreview() {
    BirdDexTheme(darkTheme = true) { Surface { ThemeShowcase() } }
}
