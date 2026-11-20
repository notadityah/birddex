package com.birddex.app.data.auth

import com.birddex.app.data.network.ApiClients
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMapperTest {

    private fun map(code: Int, body: String?) = mapAuthError(code, body, ApiClients.json)
    private fun mapLink(code: Int, body: String?) = mapAuthError(code, body, ApiClients.json, emailLink = true)

    @Test fun knownBetterAuthCodes() {
        assertEquals(AuthError.InvalidCredentials, map(401, """{"code":"INVALID_EMAIL_OR_PASSWORD","message":"x"}"""))
        assertEquals(AuthError.EmailNotVerified, map(403, """{"code":"EMAIL_NOT_VERIFIED"}"""))
        assertEquals(AuthError.EmailAlreadyInUse, map(422, """{"code":"USER_ALREADY_EXISTS"}"""))
        assertEquals(AuthError.EmailAlreadyInUse, map(422, """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL"}"""))
        assertEquals(AuthError.InvalidEmail, map(400, """{"code":"INVALID_EMAIL"}"""))
        assertEquals(AuthError.PasswordTooShort, map(400, """{"code":"PASSWORD_TOO_SHORT"}"""))
        assertEquals(AuthError.PasswordTooLong, map(400, """{"code":"PASSWORD_TOO_LONG"}"""))
        assertEquals(AuthError.TooManyAttempts, map(429, """{"code":"TOO_MANY_REQUESTS"}"""))
    }

    @Test fun emailLinkCodes() {
        // verify-email answers 401, reset-password 400 (better-auth 1.5).
        assertEquals(AuthError.InvalidLink, mapLink(401, """{"code":"INVALID_TOKEN","message":"Invalid token"}"""))
        assertEquals(AuthError.InvalidLink, mapLink(401, """{"code":"TOKEN_EXPIRED","message":"Token expired"}"""))
        assertEquals(AuthError.InvalidLink, mapLink(401, """{"code":"USER_NOT_FOUND","message":"User not found"}"""))
        assertEquals(AuthError.InvalidLink, mapLink(400, """{"code":"INVALID_TOKEN","message":"Invalid token"}"""))
        assertEquals(AuthError.PasswordTooShort, mapLink(400, """{"code":"PASSWORD_TOO_SHORT"}"""))
    }

    @Test fun tokenCodesOutsideEmailLinksAreNotAboutLinks() {
        // Google sign-in with an ID token Google rejected.
        assertEquals(AuthError.Unexpected(401, "Invalid token"), map(401, """{"code":"INVALID_TOKEN","message":"Invalid token"}"""))
    }

    @Test fun rateLimitWithoutJsonBody() {
        assertEquals(AuthError.TooManyAttempts, map(429, "Too many requests"))
    }

    @Test fun unknownCodeKeepsServerMessage() {
        assertEquals(
            AuthError.Unexpected(403, "Missing or null Origin"),
            map(403, """{"code":"MISSING_OR_NULL_ORIGIN","message":"Missing or null Origin"}"""),
        )
    }

    @Test fun unreadableBodyIsUnexpected() {
        assertEquals(AuthError.Unexpected(500, null), map(500, "<html>oops</html>"))
        assertEquals(AuthError.Unexpected(502, null), map(502, null))
    }
}
