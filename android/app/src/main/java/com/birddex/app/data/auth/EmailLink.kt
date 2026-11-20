package com.birddex.app.data.auth

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A link from a BirdDex email that the app opens (App Links, A6.2). better-auth builds them
 * on the site's own domain (backend/lambda/auth/index.ts):
 * - `https://birddex.fun/api/auth/verify-email?token=<jwt>&callbackURL=…`
 * - `https://birddex.fun/api/auth/reset-password/<token>?callbackURL=…`
 *
 * In a browser they go through the API to the website's pages; in the app they open
 * the matching sign-in screen, which calls the same API.
 */
sealed interface EmailLink {

    data class VerifyEmail(val token: String) : EmailLink {
        /** The address being verified, read from the token, to fill in on sign-in. */
        val email: String? get() = emailInVerificationToken(token)
    }

    data class ResetPassword(val token: String) : EmailLink

    companion object {
        const val VERIFY_PATH = "/api/auth/verify-email"
        const val RESET_PATH_PREFIX = "/api/auth/reset-password/"

        /** The link in [url], or null if it isn't a BirdDex email link for [siteHost]. */
        fun parse(url: String?, siteHost: String): EmailLink? {
            val uri = try {
                URI(url ?: return null)
            } catch (_: URISyntaxException) {
                return null
            }
            if (uri.scheme != "https" || !uri.host.equals(siteHost, ignoreCase = true)) return null
            val path = uri.path ?: return null
            return when {
                path == VERIFY_PATH ->
                    queryParam(uri.rawQuery, "token")?.takeIf { it.isNotBlank() }?.let(::VerifyEmail)
                path.startsWith(RESET_PATH_PREFIX) ->
                    path.removePrefix(RESET_PATH_PREFIX).takeIf { it.isNotBlank() && '/' !in it }?.let(::ResetPassword)
                else -> null
            }
        }

        private fun queryParam(rawQuery: String?, name: String): String? =
            rawQuery?.split('&')?.firstNotNullOfOrNull { pair ->
                val key = pair.substringBefore('=')
                // The Charset overload is API 33+; minSdk is 26.
                if (key == name) URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8") else null
            }

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * The verification token is a JWT whose payload carries the email. Only read for
         * convenience (the server checks the signature); null if it can't be read.
         */
        internal fun emailInVerificationToken(token: String): String? = try {
            val payload = token.split('.').getOrNull(1) ?: return null
            val text = Base64.getUrlDecoder().decode(payload).decodeToString()
            json.parseToJsonElement(text).let { it as? JsonObject }?.get("email")?.jsonPrimitive?.contentOrNull
        } catch (_: IllegalArgumentException) {
            null // not base64, not JSON, or email isn't a string
        }
    }
}
