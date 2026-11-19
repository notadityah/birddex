package com.birddex.app.ui.auth

import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.AuthResult
import com.birddex.app.data.auth.AuthUser
import com.birddex.app.data.auth.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory AuthRepository for ViewModel tests: set the next results, read the calls. */
class FakeAuthRepository : AuthRepository {
    val user = AuthUser(id = "u1", name = "Jane Doe", email = "jane@example.com", emailVerified = true)

    val state = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = state

    /** Whether restoreSession() finds a saved session cookie. */
    var hasSavedSession = false
    /** What refreshSession() reports: the signed-in user, or null for signed out. */
    var refreshResult: AuthResult<AuthUser?> = AuthResult.Success(null)
    var refreshCalls = 0

    var signInResult: AuthResult<AuthUser> = AuthResult.Success(user)
    var googleResult: AuthResult<AuthUser> = AuthResult.Success(user)
    var signUpResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resendResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resetResult: AuthResult<Unit> = AuthResult.Success(Unit)

    val signInCalls = mutableListOf<Pair<String, String>>()
    val googleCalls = mutableListOf<String>()
    val signUpCalls = mutableListOf<Triple<String, String, String>>()
    val resendCalls = mutableListOf<String>()
    val resetCalls = mutableListOf<String>()

    override suspend fun restoreSession(): Boolean {
        if (!hasSavedSession) state.value = SessionState.SignedOut
        return hasSavedSession
    }

    override suspend fun refreshSession(): AuthResult<AuthUser?> {
        refreshCalls++
        val result = refreshResult
        if (result is AuthResult.Success) {
            state.value = result.value?.let(SessionState::SignedIn) ?: SessionState.SignedOut
        }
        return result
    }

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUser> {
        signInCalls += email to password
        return signInResult.also { if (it is AuthResult.Success) state.value = SessionState.SignedIn(it.value) }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult<AuthUser> {
        googleCalls += idToken
        return googleResult.also { if (it is AuthResult.Success) state.value = SessionState.SignedIn(it.value) }
    }

    override suspend fun signUp(name: String, email: String, password: String): AuthResult<Unit> {
        signUpCalls += Triple(name, email, password)
        return signUpResult
    }

    override suspend fun resendVerificationEmail(email: String): AuthResult<Unit> {
        resendCalls += email
        return resendResult
    }

    override suspend fun requestPasswordReset(email: String): AuthResult<Unit> {
        resetCalls += email
        return resetResult
    }

    override suspend fun signOut() {
        state.value = SessionState.SignedOut
    }
}
