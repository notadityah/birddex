package com.birddex.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.birddex.app.R
import com.birddex.app.data.feedback.FeedbackCategory
import com.birddex.app.data.feedback.FeedbackRepository
import kotlinx.coroutines.delay

/** The website's labels for each category (FeedbackButton.vue), in its order. */
private val FeedbackCategory.labelRes: Int
    get() = when (this) {
        FeedbackCategory.Bug -> R.string.feedback_bug
        FeedbackCategory.Detection -> R.string.feedback_detection
        FeedbackCategory.Suggestion -> R.string.feedback_suggestion
        FeedbackCategory.Other -> R.string.feedback_other
    }

/**
 * "Send Feedback" (website: FeedbackButton.vue's dialog): a category, a message of up to
 * 2000 characters, and Submit. "Thanks for your feedback!" closes itself after two seconds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    state: FeedbackUiState,
    onSend: (FeedbackCategory, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(FeedbackCategory.Bug) }
    var message by rememberSaveable { mutableStateOf("") }
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        if (state.sent) {
            LaunchedEffect(Unit) {
                delay(2000)
                onDismiss()
            }
            Sent()
            return@ModalBottomSheet
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.feedback_title), style = MaterialTheme.typography.titleLarge)
            ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it && !state.sending }) {
                OutlinedTextField(
                    value = stringResource(category.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    enabled = !state.sending,
                    label = { Text(stringResource(R.string.feedback_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    FeedbackCategory.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                category = option
                                menuOpen = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(FeedbackRepository.MAX_MESSAGE) },
                label = { Text(stringResource(R.string.feedback_message)) },
                placeholder = { Text(stringResource(R.string.feedback_placeholder)) },
                supportingText = {
                    Text(
                        stringResource(R.string.report_note_count, message.length, FeedbackRepository.MAX_MESSAGE),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                minLines = 4,
                maxLines = 8,
                enabled = !state.sending,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let {
                Text(
                    stringResource(
                        when (it) {
                            FeedbackError.EmptyMessage -> R.string.feedback_empty
                            FeedbackError.Network -> R.string.account_offline
                            FeedbackError.Failed -> R.string.feedback_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = { onSend(category, message) }, enabled = !state.sending) {
                    Text(stringResource(if (state.sending) R.string.report_sending else R.string.feedback_submit))
                }
            }
        }
    }
}

@Composable
private fun Sent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 40.dp)
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
        Text(stringResource(R.string.feedback_sent_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.feedback_sent_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
