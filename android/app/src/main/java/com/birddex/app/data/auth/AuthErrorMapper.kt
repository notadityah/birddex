package com.birddex.app.data.auth

import kotlinx.serialization.json.Json

/**
 * Maps a failed better-auth response to an [AuthError]. Codes are the ones the website
 * handles in `frontend/src/stores/auth.js`, plus better-auth's validation codes.
 *
 * @param emailLink true for the verify-email and reset-password calls made from an email
 *   link, where a token code means the link is no good. Elsewhere the same codes mean
 *   something else (Google sign-in answers INVALID_TOKEN for a rejected ID token).
 */
internal fun mapAuthError(httpCode: Int, errorBody: String?, json: Json, emailLink: Boolean = false): AuthError {
    val body = errorBody
        ?.let { runCatching { json.decodeFromString<BetterAuthErrorBody>(it) }.getOrNull() }
    return when {
        body?.code == "INVALID_EMAIL_OR_PASSWORD" -> AuthError.InvalidCredentials
        body?.code == "EMAIL_NOT_VERIFIED" -> AuthError.EmailNotVerified
        // better-auth has used both USER_ALREADY_EXISTS and USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL.
        body?.code?.startsWith("USER_ALREADY_EXISTS") == true -> AuthError.EmailAlreadyInUse
        body?.code == "INVALID_EMAIL" -> AuthError.InvalidEmail
        body?.code == "PASSWORD_TOO_SHORT" -> AuthError.PasswordTooShort
        body?.code == "PASSWORD_TOO_LONG" -> AuthError.PasswordTooLong
        body?.code == "TOO_MANY_REQUESTS" || httpCode == 429 -> AuthError.TooManyAttempts
        body?.code == "RESET_PASSWORD_DISABLED" -> AuthError.PasswordResetUnavailable
        // A bad, used or expired link token, or one for an account that's gone.
        emailLink && body?.code in INVALID_LINK_CODES -> AuthError.InvalidLink
        else -> AuthError.Unexpected(httpCode, body?.message)
    }
}

private val INVALID_LINK_CODES = setOf("INVALID_TOKEN", "TOKEN_EXPIRED", "USER_NOT_FOUND")
