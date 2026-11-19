package com.birddex.app.data

import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.sightings.FakeSightingDao
import com.birddex.app.data.sightings.SightingEntity
import com.birddex.app.ui.auth.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserDataCleanerTest {

    private val auth = FakeAuthRepository()
    private val dao = FakeSightingDao()
    private val row = SightingEntity("s1", 1, "australian_king_parrot", "Australian King Parrot", null, null, null, null, "2026-11-18T00:00:00Z", false)

    @Test fun wipesSightingsWhenSignedOut() = runTest {
        auth.state.value = SessionState.SignedIn(auth.user)
        dao.rows.value = listOf(row)
        val cleaner = UserDataCleaner(auth, dao, backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { cleaner.run() }

        assertEquals(1, dao.rows.value.size) // signed in: kept

        auth.state.value = SessionState.SignedOut // sign-out or expired session
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test fun launchWithoutSessionWipesLeftoverData() = runTest {
        dao.rows.value = listOf(row) // e.g. left from an account whose session expired offline
        auth.state.value = SessionState.SignedOut
        val cleaner = UserDataCleaner(auth, dao, backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { cleaner.run() }

        assertTrue(dao.rows.value.isEmpty())
    }
}
