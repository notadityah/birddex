package com.birddex.app.ui.auth

import androidx.annotation.StringRes
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

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    // Held in memory only (never in SavedStateHandle).
    val password: String = "",
    val confirmPassword: String = "",
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    val loading: Boolean = false,
    val error: AuthError? = null,
    /** Set when the account exists and a verification email went out to this address. */
    val registeredEmail: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null, error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }
    fun onConfirmPasswordChange(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = null, error = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val checked = s.copy(
            nameError = AuthValidation.name(s.name),
            emailError = AuthValidation.email(s.email),
            passwordError = AuthValidation.newPassword(s.password),
            confirmPasswordError = AuthValidation.confirmPassword(s.password, s.confirmPassword),
        )
        if (listOfNotNull(checked.nameError, checked.emailError, checked.passwordError, checked.confirmPasswordError).isNotEmpty()) {
            _state.value = checked
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.signUp(s.name, s.email, s.password)
            _state.update {
                when (result) {
                    is AuthResult.Success -> it.copy(
                        loading = false,
                        password = "",
                        confirmPassword = "",
                        registeredEmail = s.email.trim(),
                    )
                    is AuthResult.Failure -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }

    fun onRegisteredHandled() = _state.update { it.copy(registeredEmail = null) }
}
