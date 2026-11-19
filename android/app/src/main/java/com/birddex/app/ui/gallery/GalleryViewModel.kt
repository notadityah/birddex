package com.birddex.app.ui.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.gallery.GalleryRepository
import com.birddex.app.data.gallery.GallerySighting
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * The Community Gallery tab (website: GalleryPage.vue + stores/gallery.js): the feed, its
 * species filter, and the sighting opened full-screen. The filter and the open sighting live
 * in [SavedStateHandle], so they survive rotation and the app being killed in the background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val gallery: GalleryRepository,
    private val birds: BirdRepository,
    private val json: Json,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    /** The species filter (a bird id), or null for "All birds". */
    val birdFilter: StateFlow<Int?> = savedState.getStateFlow(KEY_FILTER, null)

    /**
     * The feed for the current filter; a new filter starts a new feed from the top. Kept while
     * the tab's back stack entry lives, so switching tabs keeps the scroll and pages.
     */
    val feed: Flow<PagingData<GallerySighting>> =
        birdFilter.flatMapLatest { gallery.feed(it) }.cachedIn(viewModelScope)

    /** Every species for the filter menu, by name as the API sorts them. */
    val birdOptions: StateFlow<List<Bird>> =
        birds.observeBirds().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The sighting shown full-screen, or null. */
    val openSighting: StateFlow<GallerySighting?> =
        savedState.getStateFlow<String?>(KEY_OPEN, null)
            .map { saved -> saved?.let { runCatching { json.decodeFromString<GallerySighting>(it) }.getOrNull() } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // The menu needs the birds even if the Dex hasn't loaded them yet (the website loads
        // them when the gallery opens).
        viewModelScope.launch {
            if (birds.observeBirds().first().isEmpty()) birds.refresh()
        }
    }

    fun setBirdFilter(birdId: Int?) {
        savedState[KEY_FILTER] = birdId
    }

    fun open(sighting: GallerySighting) {
        savedState[KEY_OPEN] = json.encodeToString(sighting)
    }

    fun close() {
        // Not remove(): that detaches the key's StateFlow, which would keep the old sighting.
        savedState[KEY_OPEN] = null
    }

    private companion object {
        const val KEY_FILTER = "gallery_bird_id"
        const val KEY_OPEN = "gallery_open_sighting"
    }
}
