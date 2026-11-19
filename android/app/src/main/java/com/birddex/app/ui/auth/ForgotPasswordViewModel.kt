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

data class ForgotPasswordUiState(
    val email: String = "",
    @StringRes val emailError: Int? = null,
    val loading: Boolean = false,
    val error: AuthError? = null,
    /** The address the reset link went to; non-null switches the screen to "check your inbox". */
    val sentTo: String? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // ForgotPasswordRoute.email: whatever was typed on the sign-in screen.
    private val _state = MutableStateFlow(ForgotPasswordUiState(email = savedStateHandle.get<String>("email").orEmpty()))
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun submit() {
        val current = _state.value
        if (current.loading) return
        val emailError = AuthValidation.email(current.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.requestPasswordReset(current.email)
            _state.update {
                when (result) {
                    is AuthResult.Success -> it.copy(loading = false, sentTo = current.email.trim())
                    is AuthResult.Failure -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }
}
