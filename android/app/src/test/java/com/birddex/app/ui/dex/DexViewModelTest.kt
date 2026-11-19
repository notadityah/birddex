package com.birddex.app.ui.dex

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
        override suspend fun refresh(): ApiResult<Unit> {
            refreshCalls++
            return refreshResult
        }
    }

    private val repo = FakeBirdRepository()
    private val parrot = Bird(id = 1, name = "Australian King Parrot", scientificName = "Alisterus scapularis", slug = "australian_king_parrot")

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun refreshesOnOpenAndShowsSavedBirds() = runTest {
        repo.saved.value = listOf(parrot)
        val vm = DexViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(1, repo.refreshCalls)
        with(vm.state.value) {
            assertTrue(cacheLoaded)
            assertEquals(listOf(parrot), birds)
            assertNull(refreshError)
        }
    }

    @Test fun offlineKeepsSavedBirdsAndReportsTheError() = runTest {
        repo.saved.value = listOf(parrot)
        repo.refreshResult = ApiResult.Failure(ApiError.Network)
        val vm = DexViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(listOf(parrot), vm.state.value.birds)
        assertEquals(ApiError.Network, vm.state.value.refreshError)
    }

    @Test fun retryClearsTheErrorWhenItWorks() = runTest {
        repo.refreshResult = ApiResult.Failure(ApiError.Network)
        val vm = DexViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(ApiError.Network, vm.state.value.refreshError)

        repo.refreshResult = ApiResult.Success(Unit)
        vm.refresh()

        assertEquals(2, repo.refreshCalls)
        assertNull(vm.state.value.refreshError)
    }
}
