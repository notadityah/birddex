package com.birddex.app.ui.dex

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.sightings.Sighting
import com.birddex.app.data.sightings.SightingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DexUiState(
    val entries: List<DexEntry> = emptyList(),
    val filter: DexFilter = DexFilter.All,
    /** For "Welcome back, …"; null until the session is confirmed (e.g. offline launch). */
    val userName: String? = null,
    /** False until the saved copy has been read once (a few milliseconds at most). */
    val cacheLoaded: Boolean = false,
    val refreshing: Boolean = false,
    /** Last refresh failure; with saved birds on screen it just means "showing saved copy". */
    val refreshError: ApiError? = null,
    /** Sightings with a share/delete request in flight (their buttons are disabled). */
    val busySightingIds: Set<String> = emptySet(),
) {
    val totalCount: Int get() = entries.size
    val foundCount: Int get() = entries.count { it.found }
    val percent: Int get() = completionPercent(foundCount, totalCount)
    val visibleEntries: List<DexEntry> get() = entries.filteredBy(filter)
}

/** One-off feedback shown as a short message. */
enum class DexMessage { ShareFailed, Deleted, DeleteFailed }

/** MyDex: every species, found or not, with the user's progress and sightings (A3.4–A3.6). */
@HiltViewModel
class DexViewModel @Inject constructor(
    private val birds: BirdRepository,
    private val sightings: SightingRepository,
    auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<ApiError?>(null)
    private val busyIds = MutableStateFlow<Set<String>>(emptySet())
    private val filter = savedState.getStateFlow(KEY_FILTER, DexFilter.All)
    private val _messages = Channel<DexMessage>(Channel.BUFFERED)
    val messages: Flow<DexMessage> = _messages.receiveAsFlow()

    private val entries = combine(birds.observeBirds(), sightings.observeSightings(), ::buildDexEntries)
    private val status = combine(refreshing, refreshError, ::Pair)

    val state: StateFlow<DexUiState> =
        combine(entries, filter, auth.sessionState, status, busyIds) { list, f, session, (isRefreshing, error), busy ->
            DexUiState(
                entries = list,
                filter = f,
                userName = (session as? SessionState.SignedIn)?.user?.name?.takeIf { it.isNotBlank() },
                cacheLoaded = true,
                refreshing = isRefreshing,
                refreshError = error,
                busySightingIds = busy,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DexUiState())

    init {
        refresh()
    }

    fun setFilter(value: DexFilter) {
        savedState[KEY_FILTER] = value
    }

    /** Refreshes birds and sightings together; reports the first failure. */
    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            val birdsResult = async { birds.refresh() }
            val sightingsResult = async { sightings.refresh() }
            refreshError.value = listOf(birdsResult.await(), sightingsResult.await())
                .filterIsInstance<ApiResult.Failure>()
                .firstOrNull()?.error
            refreshing.value = false
        }
    }

    /** Shares or unshares in the gallery (the website's globe button). */
    fun toggleShared(sighting: Sighting) = withBusy(sighting.id) {
        if (sightings.setPublic(sighting.id, !sighting.isPublic) is ApiResult.Failure) {
            _messages.send(DexMessage.ShareFailed)
        }
    }

    /** Permanently deletes a sighting and its photo; the UI confirms first. */
    fun delete(sighting: Sighting) = withBusy(sighting.id) {
        _messages.send(
            if (sightings.delete(sighting.id) is ApiResult.Success) DexMessage.Deleted else DexMessage.DeleteFailed,
        )
    }

    private fun withBusy(id: String, block: suspend () -> Unit) {
        if (id in busyIds.value) return
        busyIds.update { it + id }
        viewModelScope.launch {
            try {
                block()
            } finally {
                busyIds.update { it - id }
            }
        }
    }

    private companion object {
        const val KEY_FILTER = "dex_filter"
    }
}
