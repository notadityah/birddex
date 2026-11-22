package com.birddex.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/** Email/password and Google auth against better-auth. */
interface AuthRepository {

    /**
     * Current sign-in state. [SessionState.Unknown] means "a session is saved on the device
     * but the server hasn't confirmed it yet" (or couldn't be reached). Goes to
     * [SessionState.SignedOut] by itself when any API call answers 401.
     */
    val sessionState: StateFlow<SessionState>

    /**
     * Launch check, local only: returns true if a session cookie is saved on the device.
     * If not, [sessionState] becomes [SessionState.SignedOut] right away. Follow a `true`
     * with [refreshSession] to confirm the session with the server.
     */
    suspend fun restoreSession(): Boolean

    /**
     * Asks the server who is signed in and updates [sessionState]. Returns the user, or
     * null when signed out. On a network error the state is left as it was.
     */
    suspend fun refreshSession(): AuthResult<AuthUser?>

    suspend fun signIn(email: String, password: String): AuthResult<AuthUser>

    /**
     * Signs in (or signs up, for a new Google user) with a Google ID token from Credential
     * Manager. The token's audience must be the backend's Google web client ID.
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult<AuthUser>

    /**
     * Creates an account. The server sends a verification email and does not sign the user
     * in (email verification is required), so this never changes [sessionState].
     */
    suspend fun signUp(name: String, email: String, password: String): AuthResult<Unit>

    suspend fun resendVerificationEmail(email: String): AuthResult<Unit>

    /**
     * Emails a password-reset link. Succeeds even when no account uses [email] (the server
     * never reveals that). The link opens the app where it's installed ([resetPassword]),
     * and the website's reset page elsewhere.
     */
    suspend fun requestPasswordReset(email: String): AuthResult<Unit>

    /**
     * Verifies an email address with the token from its verification link. Doesn't sign
     * in (the server doesn't): the user signs in afterwards. Fine to repeat once verified.
     */
    suspend fun verifyEmail(token: String): AuthResult<Unit>

    /** Sets a new password with the token from a reset link. Doesn't sign in. */
    suspend fun resetPassword(token: String, newPassword: String): AuthResult<Unit>

    /**
     * Signs out on the server and always forgets the session locally, even if the server
     * can't be reached. Ends in [SessionState.SignedOut].
     */
    suspend fun signOut()
}
