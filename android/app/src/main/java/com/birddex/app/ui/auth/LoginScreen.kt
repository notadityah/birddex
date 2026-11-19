package com.birddex.app.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.R

/** Email/password or Google sign-in. */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onNeedsVerification: (email: String) -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.outcome) {
        when (val outcome = state.outcome) {
            LoginOutcome.SignedIn -> onSignedIn()
            is LoginOutcome.NeedsVerification -> onNeedsVerification(outcome.email)
            null -> return@LaunchedEffect
        }
        viewModel.onOutcomeHandled()
    }

    val submit = {
        keyboard?.hide()
        viewModel.submit()
    }

    AuthScaffold(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
        onBack = onBack,
    ) {
        AuthErrorBanner(state.error)
        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = R.string.field_email,
            placeholder = R.string.field_email_placeholder,
            error = state.emailError,
            contentType = ContentType.Username,
            keyboardType = KeyboardType.Email,
        )
        PasswordTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = R.string.field_password,
            error = state.passwordError,
            contentType = ContentType.Password,
            onDone = submit,
        )
        // Like the website: right-aligned link between the password field and the button.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { onForgotPassword(state.email.trim()) }) {
                Text(stringResource(R.string.forgot_password))
            }
        }
        LoadingButton(
            text = stringResource(R.string.sign_in),
            loadingText = stringResource(R.string.signing_in),
            loading = state.loading,
            onClick = submit,
        )
        GoogleSignInSection(onSignedIn = onSignedIn)
        AuthLinkRow(
            prompt = stringResource(R.string.login_no_account),
            action = stringResource(R.string.sign_up),
            onClick = onCreateAccount,
        )
    }
}
