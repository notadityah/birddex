package com.birddex.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.R

/**
 * Create an account with email and password (ends on the "check your inbox" screen), or
 * continue with Google, which signs straight in (Google has already verified the email).
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: (email: String) -> Unit,
    onSignedIn: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.registeredEmail) {
        val email = state.registeredEmail ?: return@LaunchedEffect
        onRegistered(email)
        viewModel.onRegisteredHandled()
    }

    val submit = {
        keyboard?.hide()
        viewModel.submit()
    }

    AuthScaffold(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        onBack = onBack,
    ) {
        AuthErrorBanner(state.error)
        AuthTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = R.string.field_name,
            placeholder = R.string.field_name_placeholder,
            error = state.nameError,
            contentType = ContentType.PersonFullName,
            keyboardType = KeyboardType.Text,
        )
        AuthTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = R.string.field_email,
            placeholder = R.string.field_email_placeholder,
            error = state.emailError,
            contentType = ContentType.EmailAddress,
            keyboardType = KeyboardType.Email,
        )
        PasswordTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = R.string.field_password,
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
            text = stringResource(R.string.create_account),
            loadingText = stringResource(R.string.creating_account),
            loading = state.loading,
            onClick = submit,
        )
        GoogleSignInSection(onSignedIn = onSignedIn)
        AuthLinkRow(
            prompt = stringResource(R.string.register_have_account),
            action = stringResource(R.string.sign_in),
            onClick = onSignIn,
        )
    }
}
