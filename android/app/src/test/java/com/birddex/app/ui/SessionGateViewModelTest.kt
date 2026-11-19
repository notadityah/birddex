package com.birddex.app.ui

import com.birddex.app.data.auth.AuthError
import com.birddex.app.data.auth.AuthResult
import com.birddex.app.data.auth.SessionState
import com.birddex.app.ui.auth.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionGateViewModelTest {

    private val repo = FakeAuthRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun noSavedSessionGoesStraightToSignIn() {
        repo.hasSavedSession = false
        val vm = SessionGateViewModel(repo)

        assertEquals(Gate.SignedOut, vm.gate.value)
        assertEquals(0, repo.refreshCalls) // no network needed to know we're signed out
    }

    @Test fun savedSessionShowsTabsAndConfirmsInBackground() {
        repo.hasSavedSession = true
        repo.refreshResult = AuthResult.Success(repo.user)
        val vm = SessionGateViewModel(repo)

        assertEquals(Gate.Main, vm.gate.value)
        assertEquals(1, repo.refreshCalls)
    }

    @Test fun savedSessionRejectedByServerGoesToSignIn() {
        repo.hasSavedSession = true
        repo.refreshResult = AuthResult.Success(null)
        val vm = SessionGateViewModel(repo)

        assertEquals(Gate.SignedOut, vm.gate.value)
    }

    @Test fun offlineWithSavedSessionStaysInTheApp() {
        repo.hasSavedSession = true
        repo.refreshResult = AuthResult.Failure(AuthError.Network)
        val vm = SessionGateViewModel(repo)

        assertEquals(Gate.Main, vm.gate.value)
    }

    @Test fun reactsToSignInAndSessionExpiry() {
        repo.hasSavedSession = false
        val vm = SessionGateViewModel(repo)
        assertEquals(Gate.SignedOut, vm.gate.value)

        repo.state.value = SessionState.SignedIn(repo.user) // signed in
        assertEquals(Gate.Main, vm.gate.value)

        repo.state.value = SessionState.SignedOut // signed out, or a 401 expired the session
        assertEquals(Gate.SignedOut, vm.gate.value)
    }
}
