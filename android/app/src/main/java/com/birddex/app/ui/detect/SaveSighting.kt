package com.birddex.app.ui.detect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.birddex.app.R
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.network.ApiError

/**
 * "Save sighting" under the results (website: SightingSaver.vue): optional notes, "Share to
 * Community Gallery", and "Save to MyDex". The notes and checkbox are kept when another match
 * is picked, as on the website.
 */
@Composable
fun SightingSaver(
    bird: Bird,
    alreadyFound: Boolean,
    saving: Boolean,
    error: ApiError?,
    onSave: (notes: String, shareToGallery: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var notes by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(false) }

    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.save_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    withBold(stringResource(R.string.save_detected, bird.name), bird.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (alreadyFound) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.save_already_found),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.save_notes_label)) },
                placeholder = { Text(stringResource(R.string.save_notes_placeholder)) },
                minLines = 2,
                maxLines = 5,
                enabled = !saving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = share, enabled = !saving, role = Role.Checkbox, onValueChange = { share = it }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = share, onCheckedChange = null, enabled = !saving)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.save_share), style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { onSave(notes, share) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.saving))
                } else {
                    Text(stringResource(R.string.save_button))
                }
            }
            error?.let {
                Text(
                    stringResource(if (it == ApiError.Network) R.string.detect_offline else R.string.save_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

/** "Sighting saved!" with the Dex progress (website: DetectPage.vue's saved state). */
@Composable
fun SightingSaved(
    bird: Bird,
    dex: DexProgress,
    onOpenDex: () -> Unit,
    onDetectAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_check_circle_outlined),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(stringResource(R.string.saved_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            withBold(stringResource(R.string.saved_body, bird.name), bird.name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Guarded like the website: no birds loaded must not show 0/0.
        if (dex.total > 0) {
            Column(Modifier.widthIn(max = 320.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(
                        stringResource(R.string.saved_dex, dex.found, dex.total),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.saved_percent, dex.percent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { dex.found.toFloat() / dex.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = onOpenDex) { Text(stringResource(R.string.view_in_mydex)) }
            OutlinedButton(onClick = onDetectAnother) { Text(stringResource(R.string.detect_another)) }
        }
    }
}

/** [full] with its first [part] in bold, e.g. the bird's name in a translated sentence. */
private fun withBold(full: String, part: String): AnnotatedString = buildAnnotatedString {
    val start = full.indexOf(part)
    if (start < 0) {
        append(full)
        return@buildAnnotatedString
    }
    append(full.substring(0, start))
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) }
    append(full.substring(start + part.length))
}
