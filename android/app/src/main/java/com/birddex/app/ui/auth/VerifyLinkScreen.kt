package com.birddex.app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.R
import com.birddex.app.data.auth.AuthError

/**
 * A tapped email-verification link. The website has no page for this (the link redirects to
 * its sign-in page), so the app says what happened: verified, or the link is no good.
 */
@Composable
fun VerifyLinkScreen(
    onBack: (() -> Unit)?,
    onSignIn: (email: String?) -> Unit,
    viewModel: VerifyLinkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val invalid = state.error == AuthError.InvalidLink

    AuthScaffold(
        title = stringResource(
            when {
                state.verified -> R.string.verify_link_done_title
                invalid -> R.string.verify_link_invalid_title
                else -> R.string.verify_link_title
            },
        ),
        subtitle = stringResource(
            when {
                state.verified -> R.string.verify_link_done_subtitle
                invalid -> R.string.verify_link_invalid_subtitle
                else -> R.string.verify_link_subtitle
            },
        ),
        onBack = onBack,
    ) {
        when {
            state.verifying -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.verified -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_check_circle_outlined),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp),
                    )
                }
                Button(onClick = { onSignIn(viewModel.email) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sign_in))
                }
            }
            invalid -> {
                // Signing in with an unverified email leads to "Verify your email" with Resend.
                Text(
                    stringResource(R.string.verify_link_invalid_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { onSignIn(viewModel.email) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sign_in))
                }
            }
            else -> {
                AuthErrorBanner(state.error)
                Button(onClick = viewModel::verify, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}
