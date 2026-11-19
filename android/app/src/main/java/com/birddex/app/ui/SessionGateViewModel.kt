package com.birddex.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the app shows at the top level. */
enum class Gate {
    /** The few milliseconds while the saved session is looked up on the device. */
    Loading,
    /** Sign-in flow, no tabs. */
    SignedOut,
    /** The tabs. Includes "session saved but not confirmed yet", so launch never waits on the network. */
    Main,
}

/**
 * Session gate (A2.5). Every BirdDex feature beyond the bird list needs an account, like the
 * website's route guard, so the tabs only show to a signed-in user. Reacts to sign-in,
 * sign-out and expired sessions (401s) through [AuthRepository.sessionState].
 */
@HiltViewModel
class SessionGateViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val restored = MutableStateFlow(false)

    val gate: StateFlow<Gate> = combine(restored, auth.sessionState) { restored, session ->
        when {
            !restored -> Gate.Loading
            session == SessionState.SignedOut -> Gate.SignedOut
            else -> Gate.Main
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Gate.Loading)

    init {
        viewModelScope.launch {
            val hasSavedSession = auth.restoreSession()
            restored.value = true
            // Confirm with the server in the background; signs out if the session is gone.
            if (hasSavedSession) auth.refreshSession()
        }
    }
}
