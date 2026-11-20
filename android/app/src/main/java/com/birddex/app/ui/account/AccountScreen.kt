package com.birddex.app.ui.account

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.BuildConfig
import com.birddex.app.R
import com.birddex.app.data.auth.AuthUser
import com.birddex.app.data.auth.SessionState
import com.birddex.app.ui.components.formatMonthYear
import java.time.Instant

/**
 * Account tab (website: AccountPage.vue; only reachable when signed in, the session gate
 * handles signed out): profile, gallery anonymity, "Reset Collection" and sign out. The
 * website's Change Password isn't here yet; the feedback form comes in A5.4.
 */
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val signingOut by viewModel.signingOut.collectAsStateWithLifecycle()
    val anonymity by viewModel.anonymity.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val resetting by viewModel.resetting.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                AccountMessage.SettingFailed -> R.string.account_setting_failed
                AccountMessage.ResetDone -> R.string.account_reset_done
                AccountMessage.ResetFailed -> R.string.account_reset_failed
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
        when (val s = session) {
            // Saved session not confirmed yet (launch check still running, or offline).
            SessionState.Unknown -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (offline) Offline(onRetry = viewModel::refresh) else CircularProgressIndicator()
            }
            is SessionState.SignedIn -> {
                ProfileCard(s.user, stats)
                GallerySettings(anonymity, onToggle = viewModel::toggleAnonymous, onRetry = viewModel::loadSettings)
                DangerZone(sightingCount = stats.sightings, resetting = resetting, onReset = viewModel::resetCollection)
                OutlinedButton(onClick = viewModel::signOut, enabled = !signingOut, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (signingOut) R.string.signing_out else R.string.sign_out))
                }
            }
            SessionState.SignedOut -> Unit // the session gate is already switching to sign-in
        }
        if (BuildConfig.DEBUG) {
            Text(
                stringResource(R.string.build_info, BuildConfig.BASE_URL, BuildConfig.BUILD_TYPE, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileCard(user: AuthUser, stats: AccountStats) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (user.name.ifBlank { user.email }).trim().firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (user.emailVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                painterResource(R.drawable.ic_check_circle_outlined),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(stringResource(R.string.account_verified), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(stringResource(R.string.account_unverified), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val statStyle = MaterialTheme.typography.bodyMedium
                val statColor = MaterialTheme.colorScheme.onSurfaceVariant
                user.createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() }?.let {
                    Text(stringResource(R.string.account_member_since, formatMonthYear(it)), style = statStyle, color = statColor)
                }
                Text(stringResource(R.string.account_found, stats.found, stats.total), style = statStyle, color = statColor)
                Text(stringResource(R.string.account_shared, stats.shared), style = statStyle, color = statColor)
            }
        }
    }
}

/** "Appear as Anonymous in gallery" (website: Gallery Settings). */
@Composable
internal fun GallerySettings(setting: AnonymitySetting, onToggle: () -> Unit, onRetry: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.account_gallery_settings), style = MaterialTheme.typography.titleMedium)
            val enabled = setting.anonymous != null && !setting.saving
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = setting.anonymous == true, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.account_anonymous), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (setting.loading || setting.saving) {
                    CircularProgressIndicator(Modifier.padding(end = 12.dp).size(18.dp), strokeWidth = 2.dp)
                }
                Switch(checked = setting.anonymous == true, onCheckedChange = null, enabled = enabled)
            }
            Text(
                stringResource(R.string.account_anonymous_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (setting.loadFailed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.account_settings_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }
}

/**
 * Website: Danger Zone. "Reset Collection" asks first in a dialog (the website confirms
 * inline), since it can't be undone. Deleting the account is by request only, as on the
 * website: the server doesn't allow self-service deletion.
 */
@Composable
private fun DangerZone(sightingCount: Int, resetting: Boolean, onReset: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val error = MaterialTheme.colorScheme.error

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.account_reset_confirm_title)) },
            text = { Text(pluralStringResource(R.plurals.account_reset_confirm_body, sightingCount, sightingCount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onReset()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = error),
                ) { Text(stringResource(R.string.account_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    OutlinedCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, error.copy(alpha = 0.4f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.account_danger_zone), style = MaterialTheme.typography.titleMedium, color = error)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.account_reset_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.account_reset_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { confirming = true },
                    enabled = !resetting,
                    border = BorderStroke(1.dp, error.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = error),
                ) {
                    Text(stringResource(if (resetting) R.string.account_resetting else R.string.account_reset))
                }
            }
            HorizontalDivider(color = error.copy(alpha = 0.2f))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.account_delete_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.account_delete_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Offline(onRetry: () -> Unit) {
    Text(
        stringResource(R.string.account_offline),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
}
