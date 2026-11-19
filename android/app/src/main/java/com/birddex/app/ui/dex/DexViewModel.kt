package com.birddex.app.ui.dex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DexUiState(
    val birds: List<Bird> = emptyList(),
    /** False until the saved copy has been read once (a few milliseconds at most). */
    val cacheLoaded: Boolean = false,
    val refreshing: Boolean = false,
    /** Last refresh failure; with saved birds on screen it just means "showing saved copy". */
    val refreshError: ApiError? = null,
)

@HiltViewModel
class DexViewModel @Inject constructor(
    private val birds: BirdRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<ApiError?>(null)

    val state: StateFlow<DexUiState> =
        combine(birds.observeBirds(), refreshing, refreshError) { list, isRefreshing, error ->
            DexUiState(birds = list, cacheLoaded = true, refreshing = isRefreshing, refreshError = error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DexUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            refreshError.value = (birds.refresh() as? ApiResult.Failure)?.error
            refreshing.value = false
        }
    }
}
