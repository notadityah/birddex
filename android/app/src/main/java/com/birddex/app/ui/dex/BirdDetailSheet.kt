package com.birddex.app.ui.dex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.birddex.app.R
import com.birddex.app.data.sightings.Sighting
import com.birddex.app.ui.components.BirdStatsCard
import com.birddex.app.ui.components.SightingPhoto
import com.birddex.app.ui.components.formatLongDate

/**
 * Bird detail (website: BirdDetailModal.vue). Stats unlock once the bird is found, Pokédex
 * style; until then it points to Detect. Sharing, deleting and saving photos come in A3.6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdDetailSheet(entry: DexEntry, onDismiss: () -> Unit, onFindBird: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.bird.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    entry.bird.scientificName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CountPill(entry)
            if (entry.found) Found(entry) else NotFoundYet(onFindBird)
        }
    }
}

@Composable
private fun CountPill(entry: DexEntry) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (entry.found) colors.primaryContainer else colors.surfaceVariant,
        contentColor = if (entry.found) colors.onPrimaryContainer else colors.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            if (entry.found) {
                pluralStringResource(R.plurals.detail_sighting_count, entry.sightingCount, entry.sightingCount)
            } else {
                stringResource(R.string.detail_not_found)
            },
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Found(entry: DexEntry) {
    // The user's own newest photo, uncropped like on the website.
    entry.photo?.let { photo ->
        SightingPhoto(
            imageKey = photo.imageKey!!,
            imageUrl = photo.imageUrl,
            contentDescription = stringResource(R.string.detail_photo_description, entry.bird.name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .clip(MaterialTheme.shapes.medium),
        )
    }
    BirdStatsCard(entry.bird, showName = false)
    Text(stringResource(R.string.detail_your_sightings), style = MaterialTheme.typography.titleMedium)
    entry.sightings.forEach { SightingRow(it) }
}

@Composable
private fun SightingRow(sighting: Sighting) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                sighting.seenAt?.let { formatLongDate(it) } ?: stringResource(R.string.detail_unknown_date),
                style = MaterialTheme.typography.titleSmall,
            )
            if (sighting.isPublic) {
                Text(
                    stringResource(R.string.detail_shared),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            sighting.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NotFoundYet(onFindBird: () -> Unit) {
    Text(
        stringResource(R.string.detail_no_sightings),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onFindBird, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.detail_go_detect))
    }
}
