package com.birddex.app.data.auth

import com.birddex.app.data.network.AuthApi
import com.birddex.app.data.network.PersistentCookieJar
import com.birddex.app.data.network.UnexpectedHtmlResponseException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import retrofit2.Response

/**
 * [AuthRepository] over better-auth's HTTP API. The session itself is the cookie in
 * [cookieJar]; this class keeps [sessionState] in step with it.
 *
 * @param verifyCallbackUrl where email-verification links land (the website's /login,
 *   like the web app; the Android app takes these links over in A6.2).
 */
class NetworkAuthRepository(
    private val api: AuthApi,
    private val cookieJar: PersistentCookieJar,
    private val json: Json,
    private val verifyCallbackUrl: String,
) : AuthRepository {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun refreshSession(): AuthResult<AuthUser?> {
        val result = call { api.getSession() }.then { body ->
            if (body is JsonNull) null else json.decodeFromJsonElement(SessionResponse.serializer(), body).user
        }
        if (result is AuthResult.Success) {
            _sessionState.value = result.value?.let(SessionState::SignedIn) ?: SessionState.SignedOut
        }
        return result
    }

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUser> {
        val result = call { api.signInEmail(SignInEmailRequest(email.trim(), password, rememberMe = true)) }
            .then { it.user }
        if (result is AuthResult.Success) _sessionState.value = SessionState.SignedIn(result.value)
        return result
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult<AuthUser> {
        val request = SocialIdTokenSignInRequest(provider = "google", idToken = SocialIdTokenSignInRequest.IdToken(idToken))
        val result = call { api.signInSocial(request) }.then { it.user }
        if (result is AuthResult.Success) _sessionState.value = SessionState.SignedIn(result.value)
        return result
    }

    override suspend fun signUp(name: String, email: String, password: String): AuthResult<Unit> =
        call { api.signUpEmail(SignUpEmailRequest(name.trim(), email.trim(), password, verifyCallbackUrl)) }
            .then { }

    override suspend fun resendVerificationEmail(email: String): AuthResult<Unit> =
        call { api.sendVerificationEmail(SendVerificationEmailRequest(email.trim(), verifyCallbackUrl)) }
            .then { it.close() }

    override suspend fun signOut() {
        // Best effort on the server (it also expires the cookie); local sign-out always happens.
        (call { api.signOut() } as? AuthResult.Success)?.value?.close()
        withContext(Dispatchers.IO) { cookieJar.clear() }
        _sessionState.value = SessionState.SignedOut
    }

    /** Runs one API call and turns every outcome into an [AuthResult]. */
    private suspend fun <T : Any> call(request: suspend () -> Response<T>): AuthResult<T> = try {
        val response = request()
        val body = response.body()
        when {
            !response.isSuccessful ->
                AuthResult.Failure(mapAuthError(response.code(), response.errorBody()?.string(), json))
            body == null -> AuthResult.Failure(AuthError.Unexpected(response.code(), "Empty response"))
            else -> AuthResult.Success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: UnexpectedHtmlResponseException) {
        AuthResult.Failure(AuthError.Unexpected(null, e.message))
    } catch (e: IOException) {
        AuthResult.Failure(AuthError.Network)
    } catch (e: SerializationException) {
        AuthResult.Failure(AuthError.Unexpected(null, "Unreadable response from server"))
    }

    /** Maps a success value; a body that doesn't parse becomes [AuthError.Unexpected]. */
    private inline fun <T, R> AuthResult<T>.then(transform: (T) -> R): AuthResult<R> = when (this) {
        is AuthResult.Failure -> this
        is AuthResult.Success -> try {
            AuthResult.Success(transform(value))
        } catch (e: SerializationException) {
            AuthResult.Failure(AuthError.Unexpected(null, "Unreadable response from server"))
        } catch (e: IllegalArgumentException) {
            AuthResult.Failure(AuthError.Unexpected(null, "Unreadable response from server"))
        }
    }
}
