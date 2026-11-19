package com.birddex.app.ui.auth

import android.util.Log
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

sealed interface GoogleSignInError {
    data object NoGoogleAccount : GoogleSignInError
    /** Google gave a token but the BirdDex server refused it. */
    data class Server(val error: AuthError) : GoogleSignInError
    /** Credential Manager / Google Play services failed before we had a token. */
    data object DeviceFailure : GoogleSignInError
}

data class GoogleSignInUiState(
    val loading: Boolean = false,
    val error: GoogleSignInError? = null,
    val signedIn: Boolean = false,
)

/** The "Continue with Google" button's state, shared by the sign-in and register screens. */
@HiltViewModel
class GoogleSignInViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GoogleSignInUiState())
    val state: StateFlow<GoogleSignInUiState> = _state.asStateFlow()

    /** Call when the Google sheet is about to open. */
    fun onStarted() = _state.update { it.copy(loading = true, error = null) }

    fun onIdTokenResult(result: GoogleIdTokenResult) {
        when (result) {
            is GoogleIdTokenResult.Token -> viewModelScope.launch {
                val signIn = auth.signInWithGoogle(result.idToken)
                _state.update {
                    when (signIn) {
                        is AuthResult.Success -> it.copy(loading = false, signedIn = true)
                        is AuthResult.Failure -> it.copy(loading = false, error = GoogleSignInError.Server(signIn.error))
                    }
                }
            }
            GoogleIdTokenResult.Cancelled -> _state.update { it.copy(loading = false) }
            GoogleIdTokenResult.NoGoogleAccount ->
                _state.update { it.copy(loading = false, error = GoogleSignInError.NoGoogleAccount) }
            is GoogleIdTokenResult.Failed -> {
                Log.w("GoogleSignIn", "Credential Manager failed: ${result.reason}")
                _state.update { it.copy(loading = false, error = GoogleSignInError.DeviceFailure) }
            }
        }
    }

    fun onSignedInHandled() = _state.update { it.copy(signedIn = false) }
}
