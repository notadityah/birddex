package com.birddex.app.ui.dex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.birds.Bird
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
    val birds: List<Bird> = emptyList(),
    /** Ids of the species the user has at least one sighting of. */
    val seenBirdIds: Set<Int> = emptySet(),
    /** False until the saved copy has been read once (a few milliseconds at most). */
    val cacheLoaded: Boolean = false,
    val refreshing: Boolean = false,
    /** Last refresh failure; with saved birds on screen it just means "showing saved copy". */
    val refreshError: ApiError? = null,
) {
    /** Seen species that are still in the bird list. */
    val seenCount: Int get() = birds.count { it.id in seenBirdIds }
}

@HiltViewModel
class DexViewModel @Inject constructor(
    private val birds: BirdRepository,
    private val sightings: SightingRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<ApiError?>(null)

    val state: StateFlow<DexUiState> = combine(
        birds.observeBirds(),
        sightings.observeSightings(),
        refreshing,
        refreshError,
    ) { birdList, sightingList, isRefreshing, error ->
        DexUiState(
            birds = birdList,
            seenBirdIds = sightingList.mapTo(mutableSetOf()) { it.birdId },
            cacheLoaded = true,
            refreshing = isRefreshing,
            refreshError = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DexUiState())

    init {
        refresh()
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
}
