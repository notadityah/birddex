package com.birddex.app.data.gallery

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.apiCall
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/** A failed gallery page, carried through Paging's load state so the screen can show why. */
class GalleryLoadException(val error: ApiError) : Exception(error.toString())

interface GalleryRepository {
    /**
     * The Community Gallery, newest first, loading [PAGE_SIZE] more as the user scrolls.
     * Not saved for offline use (the website doesn't keep it either).
     */
    fun feed(birdId: Int? = null): Flow<PagingData<GallerySighting>>

    companion object {
        /** The website's page size (`stores/gallery.js`). */
        const val PAGE_SIZE = 20
    }
}

class NetworkGalleryRepository @Inject constructor(
    private val api: BirdDexApi,
    private val json: Json,
) : GalleryRepository {

    override fun feed(birdId: Int?): Flow<PagingData<GallerySighting>> = Pager(
        config = PagingConfig(
            pageSize = GalleryRepository.PAGE_SIZE,
            initialLoadSize = GalleryRepository.PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { GalleryPagingSource(api, json, birdId) },
    ).flow
}

/**
 * `GET /api/gallery?limit&offset[&birdId]`, keyed by offset. The feed is newest first, so a
 * sighting shared while the user scrolls pushes everything down one place and the next page
 * would repeat the last one; ids already shown are dropped, since repeats would also crash
 * the grid (its items are keyed by id).
 */
class GalleryPagingSource(
    private val api: BirdDexApi,
    private val json: Json,
    private val birdId: Int?,
) : PagingSource<Int, GallerySighting>() {

    private val seenIds = mutableSetOf<String>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GallerySighting> {
        val offset = params.key ?: 0
        val result = apiCall(json) { api.getGallery(limit = params.loadSize, offset = offset, birdId = birdId) }
        return when (result) {
            is ApiResult.Failure -> LoadResult.Error(GalleryLoadException(result.error))
            is ApiResult.Success -> {
                val page = result.value
                val next = offset + page.sightings.size
                LoadResult.Page(
                    data = synchronized(seenIds) { page.sightings.filter { seenIds.add(it.id) } },
                    prevKey = null,
                    nextKey = if (page.sightings.isEmpty() || next >= page.total) null else next,
                )
            }
        }
    }

    /** Always start again from the newest: positions shift as people share and unshare. */
    override fun getRefreshKey(state: PagingState<Int, GallerySighting>): Int? = null
}
