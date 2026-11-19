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

    var signInResult: AuthResult<AuthUser> = AuthResult.Success(user)
    var signUpResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resendResult: AuthResult<Unit> = AuthResult.Success(Unit)

    val signInCalls = mutableListOf<Pair<String, String>>()
    val signUpCalls = mutableListOf<Triple<String, String, String>>()
    val resendCalls = mutableListOf<String>()

    override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedOut)

    override suspend fun refreshSession(): AuthResult<AuthUser?> = AuthResult.Success(null)

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUser> {
        signInCalls += email to password
        return signInResult
    }

    var googleResult: AuthResult<AuthUser> = AuthResult.Success(user)
    val googleCalls = mutableListOf<String>()

    override suspend fun signInWithGoogle(idToken: String): AuthResult<AuthUser> {
        googleCalls += idToken
        return googleResult
    }

    override suspend fun signUp(name: String, email: String, password: String): AuthResult<Unit> {
        signUpCalls += Triple(name, email, password)
        return signUpResult
    }

    override suspend fun resendVerificationEmail(email: String): AuthResult<Unit> {
        resendCalls += email
        return resendResult
    }

    var resetResult: AuthResult<Unit> = AuthResult.Success(Unit)
    val resetCalls = mutableListOf<String>()

    override suspend fun requestPasswordReset(email: String): AuthResult<Unit> {
        resetCalls += email
        return resetResult
    }

    override suspend fun signOut() = Unit
}
