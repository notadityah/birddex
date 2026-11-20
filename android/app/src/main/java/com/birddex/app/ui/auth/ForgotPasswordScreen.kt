package com.birddex.app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.R

/**
 * Requests a password-reset email. The link in it opens [ResetPasswordScreen] (App Links),
 * or the website's reset page on a device without the app.
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onSignIn: (email: String) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        keyboard?.hide()
        viewModel.submit()
    }

    AuthScaffold(
        title = stringResource(R.string.forgot_title),
        subtitle = stringResource(R.string.forgot_subtitle),
        onBack = onBack,
    ) {
        val sentTo = state.sentTo
        if (sentTo != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_check_circle_outlined),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
            }
            Text(stringResource(R.string.forgot_sent, sentTo), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.forgot_sent_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { onSignIn(sentTo) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.forgot_back_to_sign_in))
            }
        } else {
            AuthErrorBanner(state.error)
            AuthTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = R.string.field_email,
                placeholder = R.string.field_email_placeholder,
                error = state.emailError,
                contentType = ContentType.EmailAddress,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onDone = submit,
            )
            LoadingButton(
                text = stringResource(R.string.forgot_send),
                loadingText = stringResource(R.string.forgot_sending),
                loading = state.loading,
                onClick = submit,
            )
            AuthLinkRow(
                prompt = stringResource(R.string.forgot_remember),
                action = stringResource(R.string.sign_in),
                onClick = { onSignIn(state.email.trim()) },
            )
        }
    }
}
