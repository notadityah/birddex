package com.birddex.app.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.birddex.app.data.gallery.GalleryRepository
import com.birddex.app.data.gallery.GallerySighting
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The Community Gallery tab (website: GalleryPage.vue + stores/gallery.js). */
@HiltViewModel
class GalleryViewModel @Inject constructor(gallery: GalleryRepository) : ViewModel() {

    /** Kept while the tab's back stack entry lives, so switching tabs keeps the scroll and pages. */
    val feed: Flow<PagingData<GallerySighting>> = gallery.feed().cachedIn(viewModelScope)
}
