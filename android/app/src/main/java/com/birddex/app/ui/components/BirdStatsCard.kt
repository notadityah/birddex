package com.birddex.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.birddex.app.R
import com.birddex.app.data.birds.Bird
import com.birddex.app.ui.components.RarityBadge

/**
 * Pokédex stats for one bird, laid out like the website's BirdStatsCard.vue: header band
 * (name, rarity, conservation status), Wingspan/Length/Weight/Diet grid, Appearance/Habitat/
 * Call rows, and a fun fact. Missing stats are skipped. The website's optional reference
 * photo (with photographer credit) is added with the Detect results in A4.4.
 */
@Composable
fun BirdStatsCard(bird: Bird, modifier: Modifier = Modifier, showName: Boolean = true) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header band
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showName) {
                    Text(bird.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        bird.scientificName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bird.rarityValue != null) RarityBadge(bird.rarityLevel)
                    bird.conservationStatus?.let { ConservationBadge(it) }
                }
            }

            // Stat grid, two per row
            val stats = listOf(
                R.string.stat_wingspan to bird.wingspan,
                R.string.stat_length to bird.length,
                R.string.stat_weight to bird.weight,
                R.string.stat_diet to bird.diet,
            ).filter { !it.second.isNullOrBlank() }
            if (stats.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            pair.forEach { (label, value) -> Stat(label, value!!, Modifier.weight(1f)) }
                            if (pair.size == 1) Column(Modifier.weight(1f)) {}
                        }
                    }
                }
            }

            // Detail rows
            listOf(
                R.string.stat_appearance to bird.appearance,
                R.string.stat_habitat to bird.habitat,
                R.string.stat_call to bird.callDescription,
            ).filter { !it.second.isNullOrBlank() }.forEach { (label, value) -> Stat(label, value!!) }

            bird.funFact?.takeIf { it.isNotBlank() }?.let { FunFact(it) }
        }
    }
}

@Composable
private fun Stat(@StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ConservationBadge(status: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(status, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun FunFact(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.stat_fun_fact).uppercase(), style = MaterialTheme.typography.labelSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
