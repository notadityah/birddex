package com.birddex.app.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * better-auth endpoints (`backend/lambda/auth/index.ts`, mounted at /api/auth). The session
 * cookie they set is stored by PersistentCookieJar. Calls return [Response] so callers see
 * HTTP status and better-auth's error body instead of an exception.
 */
interface AuthApi {

    @POST("api/auth/sign-in/email")
    suspend fun signInEmail(@Body body: SignInEmailRequest): Response<JsonObject>

    /** Current session, or JSON `null` when signed out. */
    @GET("api/auth/get-session")
    suspend fun getSession(): Response<JsonElement>

    /**
     * [origin] is optional while A2.1 finds out whether better-auth requires it here. Raw
     * body because CloudFront can replace API 403/404 errors with the website's HTML page.
     */
    @POST("api/auth/sign-out")
    suspend fun signOut(
        @Header("Origin") origin: String? = null,
        @Body body: JsonObject = JsonObject(emptyMap()),
    ): Response<ResponseBody>
}

@Serializable
data class SignInEmailRequest(
    val email: String,
    val password: String,
    /** true = 7-day persistent cookie instead of a browser-session cookie. */
    val rememberMe: Boolean,
)
