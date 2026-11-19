package com.birddex.app.ui.detect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.birddex.app.R
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.detect.Prediction
import com.birddex.app.data.feedback.FeedbackRepository
import com.birddex.app.data.network.ApiError
import java.io.File
import kotlinx.coroutines.delay

/** Longest note the website's form allows. */
const val REPORT_NOTE_MAX = 1000

/**
 * The website's auto-captured "Detection details" (ReportMatchModal.vue): the match on
 * screen and every prediction. Shown in the form and sent as part of the message.
 */
fun detectionReportDetails(shown: Bird?, predictions: List<Prediction>): String = buildList {
    shown?.let { add("Best match shown: ${it.name} (${it.slug})") }
    if (predictions.isNotEmpty()) {
        add("Predictions:")
        predictions.forEach { add("  - ${labelToName(it.label)} — ${jsNumber(it.confidence)}%") }
    }
}.joinToString("\n")

/** The message the website sends, cut to the API's limit. */
fun detectionReportMessage(details: String, note: String): String {
    var message = "Wrong / missing match reported from the detect screen.\n\n$details"
    note.trim().takeIf { it.isNotEmpty() }?.let { message += "\n\nUser note: $it" }
    return message.take(FeedbackRepository.MAX_MESSAGE)
}

/** How JavaScript prints a number: 78.23, 56.9, but 100 rather than Kotlin's "100.0". */
private fun jsNumber(value: Double): String =
    if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1e15) value.toLong().toString() else value.toString()

/**
 * "Report this bird" (website: ReportMatchModal.vue), for a wrong match or a bird the model
 * doesn't know. Sends the details, an optional note and the photo to the admins; closes
 * itself two seconds after "Thanks for the report!", like the website.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMatchSheet(
    photo: File?,
    details: String,
    report: ReportUiState,
    onSend: (note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val sent = report.sent
        if (sent != null) {
            LaunchedEffect(Unit) {
                delay(2000)
                onDismiss()
            }
            ReportSent(photoUploaded = sent.photoUploaded)
            return@ModalBottomSheet
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.report_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.report_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            photo?.let {
                AsyncImage(
                    model = it,
                    contentDescription = stringResource(R.string.report_photo_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Text(stringResource(R.string.report_details), style = MaterialTheme.typography.labelLarge)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    details.ifEmpty { stringResource(R.string.report_no_details) },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(REPORT_NOTE_MAX) },
                label = { Text(stringResource(R.string.report_note_label)) },
                placeholder = { Text(stringResource(R.string.report_note_placeholder)) },
                supportingText = {
                    Text(
                        stringResource(R.string.report_note_count, note.length, REPORT_NOTE_MAX),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                minLines = 3,
                maxLines = 6,
                enabled = !report.sending,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            report.error?.let {
                Text(
                    stringResource(if (it == ApiError.Network) R.string.detect_offline else R.string.report_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = { onSend(note) }, enabled = !report.sending) {
                    Text(stringResource(if (report.sending) R.string.report_sending else R.string.report_submit))
                }
            }
        }
    }
}

@Composable
private fun ReportSent(photoUploaded: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp, top = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_check_circle_outlined),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(stringResource(R.string.report_sent_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.report_sent_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!photoUploaded) {
            Text(
                stringResource(R.string.report_photo_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
