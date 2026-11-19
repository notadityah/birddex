package com.birddex.app.data.auth

import kotlinx.serialization.Serializable

/** The signed-in user, as better-auth returns it (unknown fields are ignored). */
@Serializable
data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean = false,
    val image: String? = null,
    /** "admin" for admins (better-auth admin plugin); null or "user" otherwise. */
    val role: String? = null,
)

/** Where the app thinks the user is. Starts [Unknown] until the session is checked. */
sealed interface SessionState {
    data object Unknown : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}

/** Why an auth call failed, in terms the UI can show. */
sealed interface AuthError {
    data object InvalidCredentials : AuthError
    data object EmailNotVerified : AuthError
    data object EmailAlreadyInUse : AuthError
    data object InvalidEmail : AuthError
    data object PasswordTooShort : AuthError
    data object PasswordTooLong : AuthError
    data object TooManyAttempts : AuthError
    /** No connection, timeout, DNS failure… */
    data object Network : AuthError
    /** Anything else; [message] is better-auth's own text when there is one. */
    data class Unexpected(val httpCode: Int?, val message: String?) : AuthError
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}

// Wire formats for better-auth (backend/lambda/auth/index.ts).

@Serializable
data class SignInEmailRequest(
    val email: String,
    val password: String,
    /** true = 7-day persistent cookie instead of a browser-session cookie. */
    val rememberMe: Boolean,
)

@Serializable
data class SignUpEmailRequest(
    val name: String,
    val email: String,
    val password: String,
    /** Where the verification link sends the user afterwards. */
    val callbackURL: String,
)

@Serializable
data class SendVerificationEmailRequest(val email: String, val callbackURL: String)

/** better-auth `sign-in/social` with an ID token from the device (no browser redirect). */
@Serializable
data class SocialIdTokenSignInRequest(val provider: String, val idToken: IdToken) {
    @Serializable
    data class IdToken(val token: String)
}

@Serializable
data class UserResponse(val user: AuthUser)

@Serializable
data class SessionResponse(val user: AuthUser)

@Serializable
data class BetterAuthErrorBody(val code: String? = null, val message: String? = null)
