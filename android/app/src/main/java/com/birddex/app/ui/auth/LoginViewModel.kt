package com.birddex.app.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.auth.AuthError
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    // Held in memory only (never in SavedStateHandle) and cleared after sign-in.
    val password: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val loading: Boolean = false,
    val error: AuthError? = null,
    val outcome: LoginOutcome? = null,
)

sealed interface LoginOutcome {
    data object SignedIn : LoginOutcome
    /** Right password, but the email isn't verified yet (same as the website's 'unverified'). */
    data class NeedsVerification(val email: String) : LoginOutcome
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // LoginRoute.email, so "Already verified? Sign in" arrives with the email filled in.
    private val _state = MutableStateFlow(LoginUiState(email = savedStateHandle.get<String>("email").orEmpty()))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun submit() {
        val current = _state.value
        if (current.loading) return
        val emailError = AuthValidation.email(current.email)
        val passwordError = AuthValidation.currentPassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.signIn(current.email, current.password)
            _state.update {
                when {
                    result is AuthResult.Success ->
                        it.copy(loading = false, password = "", outcome = LoginOutcome.SignedIn)
                    result is AuthResult.Failure && result.error == AuthError.EmailNotVerified ->
                        it.copy(loading = false, outcome = LoginOutcome.NeedsVerification(current.email.trim()))
                    result is AuthResult.Failure -> it.copy(loading = false, error = result.error)
                    else -> it
                }
            }
        }
    }

    fun onOutcomeHandled() = _state.update { it.copy(outcome = null) }
}
