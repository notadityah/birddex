package com.birddex.app.ui.dex

import androidx.lifecycle.SavedStateHandle
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.sightings.Sighting
import com.birddex.app.data.sightings.SightingRepository
import com.birddex.app.ui.auth.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DexViewModelTest {

    private class FakeBirdRepository : BirdRepository {
        val saved = MutableStateFlow<List<Bird>>(emptyList())
        var refreshResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var refreshCalls = 0
        override fun observeBirds() = saved
        override suspend fun refresh(): ApiResult<Unit> = refreshResult.also { refreshCalls++ }
    }

    private class FakeSightingRepository : SightingRepository {
        val saved = MutableStateFlow<List<Sighting>>(emptyList())
        var refreshResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var refreshCalls = 0
        override fun observeSightings() = saved
        override suspend fun refresh(): ApiResult<Unit> = refreshResult.also { refreshCalls++ }
    }

    private val birds = FakeBirdRepository()
    private val sightings = FakeSightingRepository()
    private val auth = FakeAuthRepository()
    private val savedState = SavedStateHandle()
    private val parrot = Bird(id = 1, name = "Australian King Parrot", scientificName = "Alisterus scapularis", slug = "australian_king_parrot")
    private val magpie = Bird(id = 2, name = "Australian Magpie", scientificName = "Gymnorhina tibicen", slug = "australian_magpie")

    private fun sighting(id: String, birdId: Int) =
        Sighting(id = id, birdId = birdId, slug = "x", birdName = "x", createdAt = "2026-11-18T00:00:00Z")

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.open(): DexViewModel = DexViewModel(birds, sightings, auth, savedState).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    }

    @Test fun refreshesBothOnOpenAndShowsProgress() = runTest {
        birds.saved.value = listOf(parrot, magpie)
        sightings.saved.value = listOf(sighting("a", 1), sighting("b", 1))
        val vm = open()

        assertEquals(1, birds.refreshCalls)
        assertEquals(1, sightings.refreshCalls)
        with(vm.state.value) {
            assertTrue(cacheLoaded)
            assertEquals(2, totalCount)
            assertEquals(1, foundCount) // two parrot sightings = one species
            assertEquals(50, percent)
            assertNull(refreshError)
        }
    }

    @Test fun greetsTheSignedInUserOnly() = runTest {
        val vm = open()
        assertNull(vm.state.value.userName) // session not confirmed yet

        auth.state.value = SessionState.SignedIn(auth.user)
        assertEquals("Jane Doe", vm.state.value.userName)
    }

    @Test fun filterIsAppliedAndSurvivesRecreation() = runTest {
        birds.saved.value = listOf(parrot, magpie)
        sightings.saved.value = listOf(sighting("a", 1))
        val vm = open()

        vm.setFilter(DexFilter.NotFound)
        assertEquals(listOf(magpie), vm.state.value.visibleEntries.map { it.bird })

        // Same SavedStateHandle, e.g. after rotation or process death.
        assertEquals(DexFilter.NotFound, open().state.value.filter)
    }

    @Test fun offlineKeepsSavedBirdsAndReportsTheError() = runTest {
        birds.saved.value = listOf(parrot)
        birds.refreshResult = ApiResult.Failure(ApiError.Network)
        sightings.refreshResult = ApiResult.Failure(ApiError.Network)
        val vm = open()

        assertEquals(listOf(parrot), vm.state.value.entries.map { it.bird })
        assertEquals(ApiError.Network, vm.state.value.refreshError)
    }

    @Test fun retryClearsTheErrorWhenItWorks() = runTest {
        sightings.refreshResult = ApiResult.Failure(ApiError.Http(500, "boom"))
        val vm = open()
        assertEquals(ApiError.Http(500, "boom"), vm.state.value.refreshError)

        sightings.refreshResult = ApiResult.Success(Unit)
        vm.refresh()

        assertEquals(2, sightings.refreshCalls)
        assertNull(vm.state.value.refreshError)
    }
}
