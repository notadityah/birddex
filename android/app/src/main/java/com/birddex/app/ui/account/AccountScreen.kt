package com.birddex.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.BuildConfig
import com.birddex.app.R
import com.birddex.app.data.auth.AuthUser
import com.birddex.app.data.auth.SessionState

@Composable
fun AccountScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val signingOut by viewModel.signingOut.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val s = session) {
            SessionState.Unknown ->
                if (offline) Offline(onRetry = viewModel::refresh) else CircularProgressIndicator()
            SessionState.SignedOut -> SignedOut(onSignIn, onCreateAccount)
            is SessionState.SignedIn -> SignedIn(s.user, signingOut, onSignOut = viewModel::signOut)
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
private fun SignedOut(onSignIn: () -> Unit, onCreateAccount: () -> Unit) {
    Icon(
        painterResource(R.drawable.ic_account_circle_outlined),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp),
    )
    Text(stringResource(R.string.tab_account), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
    Text(
        stringResource(R.string.account_signed_out_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_in)) }
    OutlinedButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.create_account))
    }
}

@Composable
private fun SignedIn(user: AuthUser, signingOut: Boolean, onSignOut: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            user.name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(user.name, style = MaterialTheme.typography.titleLarge)
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    OutlinedButton(onClick = onSignOut, enabled = !signingOut, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(if (signingOut) R.string.signing_out else R.string.sign_out))
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
