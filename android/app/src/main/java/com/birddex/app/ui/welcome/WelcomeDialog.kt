package com.birddex.app.ui.welcome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.birddex.app.R

/** One of the three steps (website: constants/onboardingSteps.js). */
private data class WelcomeStep(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val body: Int)

private val steps = listOf(
    WelcomeStep(R.drawable.ic_photo_camera_outlined, R.string.welcome_step_photo, R.string.welcome_step_photo_body),
    WelcomeStep(R.drawable.ic_lightbulb_outlined, R.string.welcome_step_id, R.string.welcome_step_id_body),
    WelcomeStep(R.drawable.ic_grid_view_outlined, R.string.welcome_step_collect, R.string.welcome_step_collect_body),
)

/**
 * Website: WelcomeModal.vue. Back and tapping outside close it like "Explore MyDex" (the
 * website's close button does the same, so there's no separate one here).
 */
@Composable
fun WelcomeDialog(
    /** Null shows the greeting without a name. */
    name: String?,
    onExplore: () -> Unit,
    onTryDetect: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (name != null) stringResource(R.string.welcome_title, name) else stringResource(R.string.welcome_title_no_name),
                )
                Text(
                    stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                steps.forEach { StepRow(it) }
                HorizontalDivider()
                Text(
                    stringResource(R.string.welcome_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onTryDetect) { Text(stringResource(R.string.welcome_try_detect)) } },
        dismissButton = { OutlinedButton(onClick = onExplore) { Text(stringResource(R.string.welcome_explore)) } },
    )
}

@Composable
private fun StepRow(step: WelcomeStep) {
    val accent = MaterialTheme.colorScheme.secondary
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent.copy(alpha = 0.1f), CircleShape)
                .border(2.dp, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative: the title next to it says the same.
            Icon(painterResource(step.icon), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // The dialog's body text is grey; the website's step titles are dark and bold.
            Text(
                stringResource(step.title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(step.body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
