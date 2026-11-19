package com.birddex.app.data.network

import com.birddex.app.data.auth.RequestPasswordResetRequest
import com.birddex.app.data.auth.SendVerificationEmailRequest
import com.birddex.app.data.auth.SignInEmailRequest
import com.birddex.app.data.auth.SignUpEmailRequest
import com.birddex.app.data.auth.SocialIdTokenSignInRequest
import com.birddex.app.data.auth.UserResponse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * better-auth endpoints (`backend/lambda/auth/index.ts`, mounted at /api/auth). Use them
 * through AuthRepository. The session cookie they set lives in PersistentCookieJar, and
 * AuthOriginInterceptor adds the `Origin` header better-auth requires.
 */
interface AuthApi {

    @POST("api/auth/sign-in/email")
    suspend fun signInEmail(@Body body: SignInEmailRequest): Response<UserResponse>

    /** Google sign-in: better-auth verifies the ID token with Google and creates the account if new. */
    @POST("api/auth/sign-in/social")
    suspend fun signInSocial(@Body body: SocialIdTokenSignInRequest): Response<UserResponse>

    @POST("api/auth/sign-up/email")
    suspend fun signUpEmail(@Body body: SignUpEmailRequest): Response<JsonElement>

    /** `{ user, session }`, or JSON `null` when signed out. */
    @GET("api/auth/get-session")
    suspend fun getSession(): Response<JsonElement>

    @POST("api/auth/sign-out")
    suspend fun signOut(@Body body: JsonObject = JsonObject(emptyMap())): Response<ResponseBody>

    @POST("api/auth/send-verification-email")
    suspend fun sendVerificationEmail(@Body body: SendVerificationEmailRequest): Response<ResponseBody>

    /** Succeeds whether or not the email has an account, so it never reveals who is registered. */
    @POST("api/auth/request-password-reset")
    suspend fun requestPasswordReset(@Body body: RequestPasswordResetRequest): Response<ResponseBody>
}
