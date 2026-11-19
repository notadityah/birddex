package com.birddex.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/** Email/password auth against better-auth. Google sign-in is added in A2.6. */
interface AuthRepository {

    /** Current sign-in state; [SessionState.Unknown] until [refreshSession] has run once. */
    val sessionState: StateFlow<SessionState>

    /**
     * Asks the server who is signed in and updates [sessionState]. Returns the user, or
     * null when signed out. On a network error the state is left as it was.
     */
    suspend fun refreshSession(): AuthResult<AuthUser?>

    suspend fun signIn(email: String, password: String): AuthResult<AuthUser>

    /**
     * Creates an account. The server sends a verification email and does not sign the user
     * in (email verification is required), so this never changes [sessionState].
     */
    suspend fun signUp(name: String, email: String, password: String): AuthResult<Unit>

    suspend fun resendVerificationEmail(email: String): AuthResult<Unit>

    /**
     * Signs out on the server and always forgets the session locally, even if the server
     * can't be reached. Ends in [SessionState.SignedOut].
     */
    suspend fun signOut()
}
