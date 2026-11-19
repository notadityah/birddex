package com.birddex.app.ui.auth

import androidx.lifecycle.SavedStateHandle
import com.birddex.app.R
import com.birddex.app.data.auth.AuthError
import com.birddex.app.data.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {

    private val repo = FakeAuthRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // --- Login ---

    private fun login(email: String? = null) =
        LoginViewModel(repo, SavedStateHandle(if (email != null) mapOf("email" to email) else emptyMap()))

    @Test fun loginInvalidInputNeverReachesServer() {
        val vm = login()
        vm.onEmailChange("not-an-email")
        vm.submit()

        assertEquals(R.string.error_email_invalid, vm.state.value.emailError)
        assertEquals(R.string.error_password_required, vm.state.value.passwordError)
        assertTrue(repo.signInCalls.isEmpty())
    }

    @Test fun loginSuccessClearsPassword() {
        val vm = login()
        vm.onEmailChange("jane@example.com")
        vm.onPasswordChange("secret123")
        vm.submit()

        assertEquals(listOf("jane@example.com" to "secret123"), repo.signInCalls)
        assertEquals(LoginOutcome.SignedIn, vm.state.value.outcome)
        assertEquals("", vm.state.value.password)
        assertTrue(!vm.state.value.loading)
    }

    @Test fun loginUnverifiedEmailGoesToCheckInbox() {
        repo.signInResult = AuthResult.Failure(AuthError.EmailNotVerified)
        val vm = login()
        vm.onEmailChange("  jane@example.com ")
        vm.onPasswordChange("secret123")
        vm.submit()

        assertEquals(LoginOutcome.NeedsVerification("jane@example.com"), vm.state.value.outcome)
        assertNull(vm.state.value.error)
    }

    @Test fun loginWrongPasswordShowsError() {
        repo.signInResult = AuthResult.Failure(AuthError.InvalidCredentials)
        val vm = login()
        vm.onEmailChange("jane@example.com")
        vm.onPasswordChange("wrong")
        vm.submit()

        assertEquals(AuthError.InvalidCredentials, vm.state.value.error)
        assertNull(vm.state.value.outcome)

        vm.onPasswordChange("wrong2") // editing clears the banner
        assertNull(vm.state.value.error)
    }

    @Test fun loginEmailPrefilledFromRoute() {
        assertEquals("jane@example.com", login(email = "jane@example.com").state.value.email)
    }

    // --- Register ---

    private fun filledRegister() = RegisterViewModel(repo).apply {
        onNameChange("Jane Doe")
        onEmailChange("jane@example.com ")
        onPasswordChange("secret123")
        onConfirmPasswordChange("secret123")
    }

    @Test fun registerChecksEveryFieldBeforeCallingServer() {
        val vm = RegisterViewModel(repo)
        vm.onPasswordChange("short")
        vm.onConfirmPasswordChange("different")
        vm.submit()

        with(vm.state.value) {
            assertEquals(R.string.error_name_required, nameError)
            assertEquals(R.string.error_email_required, emailError)
            assertEquals(R.string.error_password_too_short, passwordError)
            assertEquals(R.string.error_passwords_dont_match, confirmPasswordError)
        }
        assertTrue(repo.signUpCalls.isEmpty())
    }

    @Test fun registerSuccessGoesToCheckInboxAndClearsPasswords() {
        val vm = filledRegister()
        vm.submit()

        assertEquals(1, repo.signUpCalls.size)
        with(vm.state.value) {
            assertEquals("jane@example.com", registeredEmail)
            assertEquals("", password)
            assertEquals("", confirmPassword)
        }
    }

    @Test fun registerExistingEmailShowsError() {
        repo.signUpResult = AuthResult.Failure(AuthError.EmailAlreadyInUse)
        val vm = filledRegister()
        vm.submit()

        assertEquals(AuthError.EmailAlreadyInUse, vm.state.value.error)
        assertNull(vm.state.value.registeredEmail)
    }

    // --- Verify email ---

    @Test fun resendVerification() {
        val vm = VerifyEmailViewModel(repo, SavedStateHandle(mapOf("email" to "jane@example.com")))
        vm.resend()

        assertEquals(listOf("jane@example.com"), repo.resendCalls)
        assertTrue(vm.state.value.resent)
    }

    @Test fun resendRateLimited() {
        repo.resendResult = AuthResult.Failure(AuthError.TooManyAttempts)
        val vm = VerifyEmailViewModel(repo, SavedStateHandle(mapOf("email" to "jane@example.com")))
        vm.resend()

        assertEquals(AuthError.TooManyAttempts, vm.state.value.error)
        assertTrue(!vm.state.value.resent)
    }
}
