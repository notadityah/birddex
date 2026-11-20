package com.birddex.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.account.AccountRepository
import com.birddex.app.data.auth.AuthError
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.AuthResult
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.sightings.SightingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "Appear as Anonymous in gallery", as last read from or saved to the server. */
data class AnonymitySetting(
    /** Null until loaded. */
    val anonymous: Boolean? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val loadFailed: Boolean = false,
)

/** The profile card's numbers, from the saved birds and sightings (website: AccountPage.vue). */
data class AccountStats(val found: Int = 0, val total: Int = 0, val shared: Int = 0, val sightings: Int = 0)

/** One-off results, shown as toasts. */
enum class AccountMessage { SettingFailed, ResetDone, ResetFailed }

/**
 * Account tab (website: AccountPage.vue): who is signed in, gallery anonymity, "Reset
 * Collection" (delete all sightings) and signing out.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val account: AccountRepository,
    private val sightings: SightingRepository,
    birds: BirdRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = auth.sessionState

    private val _offline = MutableStateFlow(false)
    /** True when the session couldn't be checked because the server was unreachable. */
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val _signingOut = MutableStateFlow(false)
    val signingOut: StateFlow<Boolean> = _signingOut.asStateFlow()

    private val _anonymity = MutableStateFlow(AnonymitySetting())
    val anonymity: StateFlow<AnonymitySetting> = _anonymity.asStateFlow()

    private val _resetting = MutableStateFlow(false)
    val resetting: StateFlow<Boolean> = _resetting.asStateFlow()

    val stats: StateFlow<AccountStats> =
        combine(birds.observeBirds(), sightings.observeSightings()) { allBirds, mine ->
            val ids = allBirds.mapTo(HashSet()) { it.id }
            AccountStats(
                found = mine.mapTo(HashSet()) { it.birdId }.count { it in ids },
                total = allBirds.size,
                shared = mine.count { it.isPublic },
                sightings = mine.size,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AccountStats())

    private val _messages = Channel<AccountMessage>(Channel.BUFFERED)
    val messages: Flow<AccountMessage> = _messages.receiveAsFlow()

    init {
        // The session gate already checks at launch; this covers opening the tab while that
        // check failed (offline), so the Retry message appears instead of a spinner.
        if (auth.sessionState.value == SessionState.Unknown) refresh()
        loadSettings()
    }

    fun refresh() {
        viewModelScope.launch {
            _offline.value = false
            val result = auth.refreshSession()
            _offline.value = result is AuthResult.Failure && result.error == AuthError.Network
            if (_anonymity.value.loadFailed) loadSettings()
        }
    }

    fun loadSettings() {
        if (_anonymity.value.loading) return
        _anonymity.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            val result = account.getSettings()
            _anonymity.update {
                when (result) {
                    is ApiResult.Success -> it.copy(anonymous = result.value.galleryAnonymous, loading = false)
                    is ApiResult.Failure -> it.copy(loading = false, loadFailed = true)
                }
            }
        }
    }

    /** Flips the setting once the server has saved it, like the website (not optimistic). */
    fun toggleAnonymous() {
        val current = _anonymity.value
        val anonymous = current.anonymous ?: return
        if (current.saving) return
        _anonymity.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = account.setGalleryAnonymous(!anonymous)
            _anonymity.update {
                when (result) {
                    is ApiResult.Success -> it.copy(anonymous = result.value.galleryAnonymous, saving = false)
                    is ApiResult.Failure -> it.copy(saving = false)
                }
            }
            if (result is ApiResult.Failure) _messages.send(AccountMessage.SettingFailed)
        }
    }

    /** "Reset Collection", after the user confirmed: deletes every sighting and photo. */
    fun resetCollection() {
        if (_resetting.value) return
        _resetting.value = true
        viewModelScope.launch {
            val result = sightings.deleteAll()
            _resetting.value = false
            _messages.send(if (result is ApiResult.Success) AccountMessage.ResetDone else AccountMessage.ResetFailed)
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
