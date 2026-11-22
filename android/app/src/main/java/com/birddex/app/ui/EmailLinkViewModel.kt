package com.birddex.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.BuildConfig
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.EmailLink
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * An email link the app was opened with, until a screen takes it. MainActivity hands
 * links in; the sign-in flow opens them. Like the website, whose reset and verify pages are
 * for signed-out visitors only, a signed-in user is asked to sign out first.
 */
@HiltViewModel
class EmailLinkViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    /** Kept as the URL so it survives the process being killed before it's opened. */
    val pending: StateFlow<EmailLink?> = savedState.getStateFlow<String?>(KEY, null)
        .map { EmailLink.parse(it, siteHost) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EmailLink.parse(savedState.get<String>(KEY), siteHost))

    private val _signingOut = MutableStateFlow(false)
    val signingOut: StateFlow<Boolean> = _signingOut.asStateFlow()

    /** The URL the app was opened with; anything that isn't a BirdDex email link is ignored. */
    fun open(url: String?) {
        if (EmailLink.parse(url, siteHost) != null) savedState[KEY] = url
    }

    /** The sign-in flow opened it, or the user dropped it. */
    fun handled() {
        // Set to null rather than remove(): remove() would detach [pending] from the handle.
        savedState[KEY] = null
    }

    /** "Sign out" when signed in: the link stays pending and opens once signed out. */
    fun signOutToOpen() {
        if (_signingOut.value) return
        _signingOut.value = true
        viewModelScope.launch {
            auth.signOut()
            _signingOut.value = false
        }
    }

    private companion object {
        const val KEY = "email_link"
        val siteHost: String = URI(BuildConfig.BASE_URL).host
    }
}
