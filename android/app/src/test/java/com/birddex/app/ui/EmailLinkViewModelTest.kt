package com.birddex.app.ui

import androidx.lifecycle.SavedStateHandle
import com.birddex.app.data.auth.EmailLink
import com.birddex.app.data.auth.SessionState
import com.birddex.app.ui.auth.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailLinkViewModelTest {

    private val auth = FakeAuthRepository().apply { state.value = SessionState.SignedIn(user) }
    private val savedState = SavedStateHandle()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // Debug unit tests use the debug BASE_URL, production unless overridden.
    private val reset = "https://birddex.fun/api/auth/reset-password/tok123?callbackURL=x"

    @Test fun keepsALinkUntilAScreenTakesIt() {
        val vm = EmailLinkViewModel(auth, savedState)

        vm.open(reset)
        assertEquals(EmailLink.ResetPassword("tok123"), vm.pending.value)

        vm.handled()
        assertNull(vm.pending.value)
    }

    @Test fun ignoresOtherLinks() {
        val vm = EmailLinkViewModel(auth, savedState)

        vm.open(null)
        vm.open("https://birddex.fun/gallery")
        vm.open("https://example.com/api/auth/reset-password/tok123")

        assertNull(vm.pending.value)
    }

    @Test fun survivesTheProcessBeingKilled() {
        EmailLinkViewModel(auth, savedState).open(reset)

        // A new ViewModel on the restored handle, as after process death.
        assertEquals(EmailLink.ResetPassword("tok123"), EmailLinkViewModel(auth, savedState).pending.value)
    }

    @Test fun signingOutKeepsTheLinkForTheSignInFlow() {
        val vm = EmailLinkViewModel(auth, savedState)
        vm.open(reset)

        vm.signOutToOpen()

        assertEquals(1, auth.signOutCalls)
        assertEquals(SessionState.SignedOut, auth.state.value)
        assertEquals(EmailLink.ResetPassword("tok123"), vm.pending.value)
        assertFalse(vm.signingOut.value)
    }
}
