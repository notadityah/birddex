package com.birddex.app.ui.dex

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.sightings.SightingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
) {
    val totalCount: Int get() = entries.size
    val foundCount: Int get() = entries.count { it.found }
    val percent: Int get() = completionPercent(foundCount, totalCount)
    val visibleEntries: List<DexEntry> get() = entries.filteredBy(filter)
}

/** MyDex: every species, found or not, with the user's progress (A3.4). */
@HiltViewModel
class DexViewModel @Inject constructor(
    private val birds: BirdRepository,
    private val sightings: SightingRepository,
    auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<ApiError?>(null)
    private val filter = savedState.getStateFlow(KEY_FILTER, DexFilter.All)

    private val entries = combine(birds.observeBirds(), sightings.observeSightings(), ::buildDexEntries)
    private val status = combine(refreshing, refreshError, ::Pair)

    val state: StateFlow<DexUiState> = combine(entries, filter, auth.sessionState, status) { list, f, session, (isRefreshing, error) ->
        DexUiState(
            entries = list,
            filter = f,
            userName = (session as? SessionState.SignedIn)?.user?.name?.takeIf { it.isNotBlank() },
            cacheLoaded = true,
            refreshing = isRefreshing,
            refreshError = error,
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

    private companion object {
        const val KEY_FILTER = "dex_filter"
    }
}
