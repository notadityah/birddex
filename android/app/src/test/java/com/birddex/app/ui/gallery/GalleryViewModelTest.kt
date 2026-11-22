package com.birddex.app.ui.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.gallery.GalleryRepository
import com.birddex.app.data.gallery.GallerySighting
import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private class FakeGalleryRepository : GalleryRepository {
        val requested = mutableListOf<Int?>()
        override fun feed(birdId: Int?) = flowOf(PagingData.empty<GallerySighting>()).also { requested += birdId }
    }

    private class FakeBirdRepository : BirdRepository {
        val saved = MutableStateFlow<List<Bird>>(emptyList())
        var refreshCalls = 0
        override fun observeBirds() = saved
        override suspend fun refresh(): ApiResult<Unit> = ApiResult.Success(Unit).also { refreshCalls++ }
    }

    private val gallery = FakeGalleryRepository()
    private val birds = FakeBirdRepository()
    private val savedState = SavedStateHandle()
    private val magpie = Bird(id = 2, name = "Australian Magpie", scientificName = "Gymnorhina tibicen", slug = "australian_magpie")
    private val sighting = GallerySighting(
        id = "3c1d2e4f",
        birdId = 14,
        birdName = "House Sparrow",
        scientificName = "Passer domesticus",
        slug = "house_sparrow",
        detectedAt = "2026-08-06T01:00:00Z",
        notes = "clayton station",
        createdAt = "2026-08-06T01:00:00Z",
        userName = "Anonymous",
        imageUrl = "https://s3.example/images/x.jpg?X-Amz-Signature=sig",
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.open(): GalleryViewModel =
        GalleryViewModel(gallery, birds, ApiClients.json, savedState).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.feed.collect {} }
        }

    @Test fun startsWithAllBirds() = runTest {
        val vm = open()

        assertNull(vm.birdFilter.value)
        assertEquals(listOf<Int?>(null), gallery.requested)
    }

    @Test fun aFilterStartsANewFeedForThatBird() = runTest {
        val vm = open()

        vm.setBirdFilter(2)
        vm.setBirdFilter(null)

        assertEquals(listOf(null, 2, null), gallery.requested)
    }

    @Test fun filterSurvivesTheAppBeingKilled() = runTest {
        open().setBirdFilter(2)

        // Same SavedStateHandle, as after process death.
        assertEquals(2, open().birdFilter.value)
        assertEquals(2, gallery.requested.last())
    }

    @Test fun openSightingSurvivesTheAppBeingKilled() = runTest {
        val vm = open()
        vm.open(sighting)
        assertEquals(sighting, vm.openSighting.value)

        val restored = open()
        assertEquals(sighting, restored.openSighting.value)

        restored.close()
        assertNull(restored.openSighting.value)
    }

    @Test fun birdsAreFetchedForTheMenuOnlyIfNoneAreSaved() = runTest {
        open()
        assertEquals(1, birds.refreshCalls)

        birds.saved.value = listOf(magpie)
        val vm = open()
        assertEquals(1, birds.refreshCalls)
        assertEquals(listOf(magpie), vm.birdOptions.value)
    }
}
