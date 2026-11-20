package com.birddex.app.ui.auth

import androidx.lifecycle.SavedStateHandle
import com.birddex.app.R
import com.birddex.app.data.auth.AuthError
import com.birddex.app.data.auth.AuthResult
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** VerifyLinkViewModel and ResetPasswordViewModel (the screens email links open). */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailLinkScreensTest {

    private val auth = FakeAuthRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val token = Base64.getUrlEncoder().withoutPadding().let {
        it.encodeToString("{}".toByteArray()) + "." + it.encodeToString("""{"email":"jane@example.com"}""".toByteArray()) + ".sig"
    }

    private fun verify() = VerifyLinkViewModel(auth, SavedStateHandle(mapOf("token" to token)))
    private fun reset() = ResetPasswordViewModel(auth, SavedStateHandle(mapOf("token" to "tok123")))

    // --- verify ---

    @Test fun verifiesAsSoonAsItOpens() {
        val vm = verify()

        assertEquals(listOf(token), auth.verifyEmailCalls)
        assertEquals(VerifyLinkUiState(verifying = false, verified = true), vm.state.value)
        assertEquals("jane@example.com", vm.email) // filled in on sign-in
    }

    @Test fun anExpiredLinkSaysSo() {
        auth.verifyEmailResult = AuthResult.Failure(AuthError.InvalidLink)

        assertEquals(VerifyLinkUiState(verifying = false, error = AuthError.InvalidLink), verify().state.value)
    }

    @Test fun offlineCanTryAgain() {
        auth.verifyEmailResult = AuthResult.Failure(AuthError.Network)
        val vm = verify()
        assertEquals(AuthError.Network, vm.state.value.error)

        auth.verifyEmailResult = AuthResult.Success(Unit)
        vm.verify()

        assertTrue(vm.state.value.verified)
        assertEquals(2, auth.verifyEmailCalls.size)
    }

    @Test fun notVerifiedTwiceOnceDone() {
        val vm = verify()
        vm.verify()
        assertEquals(1, auth.verifyEmailCalls.size)
    }

    // --- reset ---

    @Test fun setsTheNewPassword() {
        val vm = reset()
        vm.onPasswordChange("correct horse")
        vm.onConfirmPasswordChange("correct horse")

        vm.submit()

        assertEquals(listOf("tok123" to "correct horse"), auth.resetPasswordCalls)
        assertEquals(ResetPasswordUiState(done = true), vm.state.value) // passwords cleared
    }

    @Test fun checksTheFormFirst() {
        val vm = reset()

        vm.submit()
        assertEquals(R.string.error_password_required, vm.state.value.passwordError)
        assertEquals(R.string.error_confirm_password_required, vm.state.value.confirmPasswordError)

        vm.onPasswordChange("short")
        vm.onConfirmPasswordChange("short")
        vm.submit()
        assertEquals(R.string.error_password_too_short, vm.state.value.passwordError)

        vm.onPasswordChange("long enough")
        vm.onConfirmPasswordChange("long enougH")
        vm.submit()
        assertNull(vm.state.value.passwordError)
        assertEquals(R.string.error_passwords_dont_match, vm.state.value.confirmPasswordError)

        assertTrue(auth.resetPasswordCalls.isEmpty())
    }

    @Test fun aUsedLinkKeepsTheFormAndSaysWhy() {
        auth.resetPasswordResult = AuthResult.Failure(AuthError.InvalidLink)
        val vm = reset()
        vm.onPasswordChange("correct horse")
        vm.onConfirmPasswordChange("correct horse")

        vm.submit()

        val state = vm.state.value
        assertEquals(AuthError.InvalidLink, state.error)
        assertFalse(state.loading)
        assertFalse(state.done)
        assertEquals("correct horse", state.password)
    }
}
