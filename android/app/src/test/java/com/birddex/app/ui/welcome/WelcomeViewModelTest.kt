package com.birddex.app.ui.welcome

import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.onboarding.WelcomeStore
import com.birddex.app.ui.auth.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    private class FakeWelcomeStore : WelcomeStore {
        /** Null until "read from disk". */
        val stored = MutableStateFlow<Boolean?>(false)
        var markSeenCalls = 0
        override val seen: Flow<Boolean> = stored.filterNotNull()
        override fun markSeen() {
            markSeenCalls++
            stored.value = true
        }
    }

    private val auth = FakeAuthRepository().apply { state.value = SessionState.SignedIn(user) }
    private val store = FakeWelcomeStore()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun open() = WelcomeViewModel(auth, store)

    @Test fun opensByItselfTheFirstTime() {
        val vm = open()

        assertTrue(vm.open.value)
        assertEquals("Jane Doe", vm.userName.value)
    }

    @Test fun notAgainOnceSeen() {
        store.stored.value = true

        assertFalse(open().open.value)
    }

    @Test fun waitsForTheFlagToBeRead() {
        store.stored.value = null
        val vm = open()
        assertFalse(vm.open.value)

        store.stored.value = false
        assertTrue(vm.open.value)
    }

    @Test fun waitsForTheServerToConfirmTheSession() {
        // Launch with a saved session: the tabs show before the server answers.
        auth.state.value = SessionState.Unknown
        val vm = open()
        assertFalse(vm.open.value)
        assertNull(vm.userName.value)

        auth.state.value = SessionState.SignedIn(auth.user)
        assertTrue(vm.open.value)
        assertEquals("Jane Doe", vm.userName.value)
    }

    @Test fun closingItRecordsItAndItStaysClosed() {
        val vm = open()

        vm.dismiss()

        assertFalse(vm.open.value)
        assertEquals(1, store.markSeenCalls)
        assertEquals(true, store.stored.value)
    }

    @Test fun howItWorksShowsItAgain() {
        store.stored.value = true
        val vm = open()

        vm.show()
        assertTrue(vm.open.value)

        vm.dismiss()
        assertFalse(vm.open.value)
    }

    @Test fun greetsByEmailWhenThereIsNoName() {
        auth.state.value = SessionState.SignedIn(auth.user.copy(name = " "))

        assertEquals("jane@example.com", open().userName.value)
    }
}
