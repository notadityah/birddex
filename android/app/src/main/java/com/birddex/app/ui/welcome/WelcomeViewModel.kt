package com.birddex.app.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.AuthUser
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.onboarding.WelcomeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The first-run welcome (website: WelcomeModal.vue and useOnboarding.js). It opens by itself
 * the first time this phone is signed in, and "How it works" on the Account tab opens it again.
 * Scoped to the tabs, not to one tab, so every tab shares it.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    auth: AuthRepository,
    private val store: WelcomeStore,
) : ViewModel() {

    /** Who to greet (website: authStore.displayName), or null if not known yet. */
    val userName: StateFlow<String?> = auth.sessionState
        .map { session -> (session as? SessionState.SignedIn)?.user?.displayName() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, (auth.sessionState.value as? SessionState.SignedIn)?.user?.displayName())

    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    init {
        // Waits for a confirmed sign-in, not just a saved session, so the greeting has the name.
        viewModelScope.launch {
            combine(store.seen, auth.sessionState) { seen, session -> !seen && session is SessionState.SignedIn }
                .first { it }
            _open.value = true
        }
    }

    /** "How it works". */
    fun show() {
        _open.value = true
    }

    /** Every way of closing it, so closing it once is enough (website: dismiss()). */
    fun dismiss() {
        _open.value = false
        store.markSeen()
    }
}

private fun AuthUser.displayName(): String = name.ifBlank { email }
