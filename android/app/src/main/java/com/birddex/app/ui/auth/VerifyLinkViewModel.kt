package com.birddex.app.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.auth.AuthError
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.AuthResult
import com.birddex.app.data.auth.EmailLink
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VerifyLinkUiState(
    val verifying: Boolean = true,
    val verified: Boolean = false,
    /** Why it failed: [AuthError.InvalidLink] for a bad or expired link, else e.g. offline. */
    val error: AuthError? = null,
)

/** Verifies the email from a tapped verification link as soon as the screen opens. */
@HiltViewModel
class VerifyLinkViewModel @Inject constructor(
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // VerifyLinkRoute.token
    private val link = EmailLink.VerifyEmail(savedStateHandle.get<String>("token").orEmpty())

    /** The address being verified, to fill in on sign-in; null if the token doesn't say. */
    val email: String? = link.email

    private val _state = MutableStateFlow(VerifyLinkUiState())
    val state: StateFlow<VerifyLinkUiState> = _state.asStateFlow()

    init {
        verify()
    }

    /** Also "Try again". Verifying twice is harmless (already verified counts as success). */
    fun verify() {
        if (_state.value.verified) return
        _state.value = VerifyLinkUiState(verifying = true)
        viewModelScope.launch {
            _state.value = when (val result = auth.verifyEmail(link.token)) {
                is AuthResult.Success -> VerifyLinkUiState(verifying = false, verified = true)
                is AuthResult.Failure -> VerifyLinkUiState(verifying = false, error = result.error)
            }
        }
    }
}
