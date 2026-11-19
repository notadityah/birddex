package com.birddex.app.data.auth

import kotlinx.serialization.json.Json

/**
 * Maps a failed better-auth response to an [AuthError]. Codes are the ones the website
 * handles in `frontend/src/stores/auth.js`, plus better-auth's validation codes.
 */
internal fun mapAuthError(httpCode: Int, errorBody: String?, json: Json): AuthError {
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
        else -> AuthError.Unexpected(httpCode, body?.message)
    }
}
