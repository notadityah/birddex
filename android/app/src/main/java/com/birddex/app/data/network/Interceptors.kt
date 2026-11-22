package com.birddex.app.data.network

import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the website's `Origin` to better-auth calls. better-auth rejects any auth request
 * that carries a session cookie without one (403 MISSING_OR_NULL_ORIGIN),
 * which would break sign-out and every call made while signed in.
 */
class AuthOriginInterceptor(private val origin: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val needsOrigin = request.url.encodedPath.startsWith("/api/auth/") && request.header("Origin") == null
        return chain.proceed(if (needsOrigin) request.newBuilder().header("Origin", origin).build() else request)
    }
}

/**
 * Turns a "successful" HTML page from an `/api/` path into an error. CloudFront used to answer API
 * 403/404s with the website's index.html and status 200 (since fixed in production);
 * this keeps such a page from ever being read as success if it comes back.
 */
class HtmlErrorPageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val isHtml = response.header("Content-Type").orEmpty().startsWith("text/html", ignoreCase = true)
        if (request.url.encodedPath.startsWith("/api/") && response.isSuccessful && isHtml) {
            response.close()
            throw UnexpectedHtmlResponseException(request.url)
        }
        return response
    }
}

/**
 * Reports a `401` from the BirdDex API (session expired or revoked) via [onUnauthorized].
 * better-auth's own `/api/auth/` calls are skipped: there a 401 means e.g. a wrong password,
 * not that the current session is gone.
 */
class UnauthorizedInterceptor(private val onUnauthorized: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val path = chain.request().url.encodedPath
        if (response.code == 401 && path.startsWith("/api/") && !path.startsWith("/api/auth/")) {
            onUnauthorized()
        }
        return response
    }
}

class UnexpectedHtmlResponseException(url: HttpUrl) :
    IOException("Expected JSON from ${url.encodedPath} but got an HTML page")
