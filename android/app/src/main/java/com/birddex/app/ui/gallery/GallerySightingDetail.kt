package com.birddex.app.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.birddex.app.R
import com.birddex.app.data.gallery.GallerySighting
import com.birddex.app.ui.components.SightingPhoto
import com.birddex.app.ui.components.formatLongDate

/**
 * One shared sighting, full-screen (website: GallerySightingModal.vue): the bird, the whole
 * photo, who spotted it, when, and their notes. Back or the close button dismisses it.
 */
@Composable
fun GallerySightingDetail(sighting: GallerySighting, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f).padding(top = 4.dp)) {
                        Text(
                            sighting.birdName,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        sighting.scientificName?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close))
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sighting.imageUrl?.let { url ->
                        SightingPhoto(
                            imageKey = sighting.photoCacheKey,
                            imageUrl = url,
                            contentDescription = sighting.birdName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    Detail(stringResource(R.string.gallery_spotted_by), sighting.userName ?: stringResource(R.string.gallery_anonymous))
                    sighting.seenAt?.let { Detail(stringResource(R.string.gallery_date), formatLongDate(it)) }
                    sighting.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "**Spotted by** Jane Doe", as the website lays it out. */
@Composable
private fun Detail(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(label) }
            append("  ")
            append(value)
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}
