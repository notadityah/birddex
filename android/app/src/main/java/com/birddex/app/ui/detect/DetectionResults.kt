package com.birddex.app.ui.detect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.birddex.app.R
import com.birddex.app.data.detect.Detection
import com.birddex.app.data.detect.Prediction
import com.birddex.app.ui.components.ReferencePhoto

/**
 * The matches, best first (website: DetectionResults.vue): each with its reference photo,
 * name, confidence and a bar, "Best match" on the first. Tapping one shows that bird's photo
 * and stats instead, since the model's best guess is sometimes wrong.
 */
@Composable
fun DetectionResults(
    detection: Detection,
    selectedSlug: String?,
    onSelect: (label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.results_title), style = MaterialTheme.typography.titleMedium)
            if (detection.predictions.isEmpty()) {
                Text(
                    stringResource(R.string.results_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Text(
                stringResource(R.string.results_pick_another),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                detection.predictions.forEachIndexed { index, prediction ->
                    MatchRow(
                        prediction = prediction,
                        name = detection.nameOf(prediction.label),
                        best = index == 0,
                        selected = prediction.label == selectedSlug,
                        // Only matches with a bird row can be shown (see DetectViewModel.selectMatch).
                        enabled = prediction.label in detection.birds,
                        onClick = { onSelect(prediction.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchRow(
    prediction: Prediction,
    name: String,
    best: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primary.copy(alpha = 0.08f).compositeOver(colors.surface) else colors.surface,
        border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.RadioButton }
            .then(if (enabled) Modifier else Modifier.alpha(0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReferencePhoto(
                slug = prediction.label,
                contentDescription = null, // the name is right beside it
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (best) BestMatchBadge()
                    }
                    Text(
                        formatConfidence(prediction.confidence),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                LinearProgressIndicator(
                    progress = { (prediction.confidence / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BestMatchBadge() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Text(
            stringResource(R.string.best_match),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
