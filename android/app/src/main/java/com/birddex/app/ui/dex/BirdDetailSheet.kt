package com.birddex.app.ui.dex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.birddex.app.R
import com.birddex.app.data.sightings.Sighting
import com.birddex.app.ui.components.BirdStatsCard
import com.birddex.app.ui.components.SightingPhoto
import com.birddex.app.ui.components.formatLongDate

/** What the user can do with one of their sightings in the sheet. */
class SightingActions(
    val busyIds: Set<String>,
    val onToggleShared: (Sighting) -> Unit,
    val onSavePhoto: (Sighting) -> Unit,
    val onDelete: (Sighting) -> Unit,
)

/**
 * Bird detail (website: BirdDetailModal.vue). Stats unlock once the bird is found, Pokédex
 * style; until then it points to Detect. Each sighting can be shared/unshared, saved to the
 * phone, or deleted (after confirmation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdDetailSheet(entry: DexEntry, actions: SightingActions, onDismiss: () -> Unit, onFindBird: () -> Unit) {
    var confirmDelete by remember { mutableStateOf<Sighting?>(null) }

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
            if (entry.found) {
                Found(entry, actions, onAskDelete = { confirmDelete = it })
            } else {
                NotFoundYet(onFindBird)
            }
        }
    }

    confirmDelete?.let { sighting ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    actions.onDelete(sighting)
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
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
private fun Found(entry: DexEntry, actions: SightingActions, onAskDelete: (Sighting) -> Unit) {
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
    entry.sightings.forEach { sighting ->
        SightingRow(
            sighting = sighting,
            busy = sighting.id in actions.busyIds,
            onToggleShared = { actions.onToggleShared(sighting) },
            onSavePhoto = { actions.onSavePhoto(sighting) },
            onDelete = { onAskDelete(sighting) },
        )
    }
}

@Composable
private fun SightingRow(
    sighting: Sighting,
    busy: Boolean,
    onToggleShared: () -> Unit,
    onSavePhoto: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        sighting.seenAt?.let { formatLongDate(it) } ?: stringResource(R.string.detail_unknown_date),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(if (sighting.isPublic) R.string.detail_shared else R.string.detail_private),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sighting.isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Globe like the website: filled when shared.
                IconButton(onClick = onToggleShared, enabled = !busy) {
                    Icon(
                        painterResource(if (sighting.isPublic) R.drawable.ic_public else R.drawable.ic_public_off),
                        contentDescription = stringResource(if (sighting.isPublic) R.string.unshare_from_gallery else R.string.share_to_gallery),
                        tint = if (sighting.isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (sighting.imageKey != null) {
                    IconButton(onClick = onSavePhoto) {
                        Icon(painterResource(R.drawable.ic_download), contentDescription = stringResource(R.string.save_photo))
                    }
                }
                IconButton(onClick = onDelete, enabled = !busy) {
                    Icon(
                        painterResource(R.drawable.ic_delete_outlined),
                        contentDescription = stringResource(R.string.delete_sighting),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            sighting.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp, end = 8.dp),
                )
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
