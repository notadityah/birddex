package com.birddex.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.BuildConfig
import com.birddex.app.R
import kotlinx.coroutines.launch

/** "or continue with" divider + Google button, as on the website's login and register pages. */
@Composable
fun GoogleSignInSection(
    onSignedIn: () -> Unit,
    viewModel: GoogleSignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            onSignedIn()
            viewModel.onSignedInHandled()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                stringResource(R.string.or_continue_with),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = {
                viewModel.onStarted()
                scope.launch {
                    viewModel.onIdTokenResult(requestGoogleIdToken(context, BuildConfig.GOOGLE_WEB_CLIENT_ID))
                }
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Image(painterResource(R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.google))
        }
        state.error?.let { error ->
            Text(
                when (error) {
                    GoogleSignInError.NoGoogleAccount -> stringResource(R.string.google_no_account)
                    GoogleSignInError.DeviceFailure -> stringResource(R.string.google_failed)
                    is GoogleSignInError.Server -> authErrorMessage(error.error)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
