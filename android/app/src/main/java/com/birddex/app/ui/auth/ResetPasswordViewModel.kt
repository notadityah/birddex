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

data class ResetPasswordUiState(
    // Held in memory only (never in SavedStateHandle).
    val password: String = "",
    val confirmPassword: String = "",
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    val loading: Boolean = false,
    val error: AuthError? = null,
    /** The password was changed; the screen switches to "Sign in". */
    val done: Boolean = false,
)

/** "Set new password" from a tapped reset link (website: ResetPasswordPage.vue). */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // ResetPasswordRoute.token
    private val token = savedStateHandle.get<String>("token").orEmpty()

    private val _state = MutableStateFlow(ResetPasswordUiState())
    val state: StateFlow<ResetPasswordUiState> = _state.asStateFlow()

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }
    fun onConfirmPasswordChange(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = null, error = null) }

    fun submit() {
        val s = _state.value
        if (s.loading || s.done) return
        // Same rules as creating an account (the website leaves the length to the server).
        val checked = s.copy(
            passwordError = AuthValidation.newPassword(s.password),
            confirmPasswordError = AuthValidation.confirmPassword(s.password, s.confirmPassword),
        )
        if (checked.passwordError != null || checked.confirmPasswordError != null) {
            _state.value = checked
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.resetPassword(token, s.password)
            _state.update {
                when (result) {
                    is AuthResult.Success -> ResetPasswordUiState(done = true)
                    is AuthResult.Failure -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }
}
