package com.birddex.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.auth.AuthError
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.AuthResult
import com.birddex.app.data.auth.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Account tab: who is signed in, and signing out. Settings and feedback come in Phase 5. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = auth.sessionState

    private val _offline = MutableStateFlow(false)
    /** True when the session couldn't be checked because the server was unreachable. */
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val _signingOut = MutableStateFlow(false)
    val signingOut: StateFlow<Boolean> = _signingOut.asStateFlow()

    init {
        // The session gate already checks at launch; this covers opening the tab while that
        // check failed (offline), so the Retry message appears instead of a spinner.
        if (auth.sessionState.value == SessionState.Unknown) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _offline.value = false
            val result = auth.refreshSession()
            _offline.value = result is AuthResult.Failure && result.error == AuthError.Network
        }
    }

    fun signOut() {
        if (_signingOut.value) return
        viewModelScope.launch {
            _signingOut.value = true
            auth.signOut()
            _signingOut.value = false
        }
    }
}
