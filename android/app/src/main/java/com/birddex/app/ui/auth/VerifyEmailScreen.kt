package com.birddex.app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/**
 * "Check your inbox" after sign-up, or after signing in with an unverified email. The link
 * in the email opens [VerifyLinkScreen] on top of this one (App Links).
 */
@Composable
fun VerifyEmailScreen(
    onBack: () -> Unit,
    onSignIn: (email: String) -> Unit,
    viewModel: VerifyEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AuthScaffold(
        title = stringResource(R.string.verify_title),
        subtitle = stringResource(R.string.verify_subtitle, state.email),
        onBack = onBack,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_mail_outlined),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
        }
        Text(stringResource(R.string.verify_body), style = MaterialTheme.typography.bodyLarge)
        AuthErrorBanner(state.error)
        if (state.resent) {
            Text(
                stringResource(R.string.verify_resent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        OutlinedButton(
            onClick = viewModel::resend,
            enabled = !state.sending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (state.sending) R.string.verify_sending else R.string.verify_resend))
        }
        AuthLinkRow(
            prompt = stringResource(R.string.verify_already_verified),
            action = stringResource(R.string.sign_in),
            onClick = { onSignIn(state.email) },
        )
    }
}
