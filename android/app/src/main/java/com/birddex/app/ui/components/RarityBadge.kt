package com.birddex.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.birddex.app.R
import com.birddex.app.data.birds.Rarity

/** Same colours as the website's rarity badges: grey common, green uncommon, amber rare. */
@Composable
fun RarityBadge(rarity: Rarity, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content, label) = when (rarity) {
        Rarity.Common -> Triple(colors.surfaceVariant, colors.onSurfaceVariant, R.string.rarity_common)
        Rarity.Uncommon -> Triple(colors.secondaryContainer, colors.onSecondaryContainer, R.string.rarity_uncommon)
        Rarity.Rare -> Triple(colors.tertiaryContainer, colors.onTertiaryContainer, R.string.rarity_rare)
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.extraLarge, modifier = modifier) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
