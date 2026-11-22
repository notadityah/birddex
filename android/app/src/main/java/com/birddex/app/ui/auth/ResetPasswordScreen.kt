package com.birddex.app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.R
import com.birddex.app.data.auth.AuthError

/** A tapped password-reset link: choose a new password (website: ResetPasswordPage.vue). */
@Composable
fun ResetPasswordScreen(
    onBack: (() -> Unit)?,
    onSignIn: () -> Unit,
    /** "Request a new link" when this one is no good. */
    onRequestNewLink: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        keyboard?.hide()
        viewModel.submit()
    }

    AuthScaffold(
        title = stringResource(R.string.reset_title),
        subtitle = stringResource(R.string.reset_subtitle),
        onBack = onBack,
    ) {
        if (state.done) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_check_circle_outlined),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
            }
            Text(stringResource(R.string.reset_done), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sign_in))
            }
        } else {
            AuthErrorBanner(state.error)
            if (state.error == AuthError.InvalidLink) {
                TextButton(onClick = onRequestNewLink) { Text(stringResource(R.string.reset_request_new_link)) }
            }
            PasswordTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = R.string.field_new_password,
                error = state.passwordError,
                contentType = ContentType.NewPassword,
                imeAction = ImeAction.Next,
            )
            PasswordTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = R.string.field_confirm_password,
                error = state.confirmPasswordError,
                contentType = ContentType.NewPassword,
                onDone = submit,
            )
            LoadingButton(
                text = stringResource(R.string.reset_submit),
                loadingText = stringResource(R.string.reset_submitting),
                loading = state.loading,
                onClick = submit,
            )
        }
    }
}
