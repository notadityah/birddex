package com.birddex.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.network.BirdDexApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Until sign-in lands (Phase 2), the Account tab just shows whether the API is reachable. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val api: BirdDexApi,
) : ViewModel() {

    sealed interface ApiStatus {
        data object Checking : ApiStatus
        data class Reachable(val birdCount: Int) : ApiStatus
        data class Unreachable(val reason: String) : ApiStatus
    }

    private val _apiStatus = MutableStateFlow<ApiStatus>(ApiStatus.Checking)
    val apiStatus: StateFlow<ApiStatus> = _apiStatus.asStateFlow()

    init {
        checkApi()
    }

    fun checkApi() {
        _apiStatus.value = ApiStatus.Checking
        viewModelScope.launch {
            _apiStatus.value = try {
                ApiStatus.Reachable(birdCount = api.getBirds().size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApiStatus.Unreachable(e.message ?: e::class.simpleName.orEmpty())
            }
        }
    }
}
