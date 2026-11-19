package com.birddex.app.ui.auth

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

data class VerifyEmailUiState(
    val email: String,
    val sending: Boolean = false,
    val resent: Boolean = false,
    val error: AuthError? = null,
)

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // VerifyEmailRoute.email
    private val _state = MutableStateFlow(VerifyEmailUiState(email = savedStateHandle.get<String>("email").orEmpty()))
    val state: StateFlow<VerifyEmailUiState> = _state.asStateFlow()

    fun resend() {
        val current = _state.value
        if (current.sending || current.email.isBlank()) return
        _state.update { it.copy(sending = true, resent = false, error = null) }
        viewModelScope.launch {
            val result = auth.resendVerificationEmail(current.email)
            _state.update {
                when (result) {
                    is AuthResult.Success -> it.copy(sending = false, resent = true)
                    is AuthResult.Failure -> it.copy(sending = false, error = result.error)
                }
            }
        }
    }
}
