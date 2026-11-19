package com.birddex.app.data.auth

import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.AuthApi
import com.birddex.app.data.network.PersistentCookieJar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.create

/** Runs the real HTTP stack (interceptors, Retrofit, cookie jar) against a fake server. */
class NetworkAuthRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private val server = MockWebServer()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var baseUrl: String
    private lateinit var cookieJar: PersistentCookieJar
    private lateinit var client: OkHttpClient
    private lateinit var repo: NetworkAuthRepository

    private val userJson =
        """{"id":"u1","name":"Test","email":"t@example.com","emailVerified":true,"createdAt":"2026-11-19T00:00:00Z"}"""

    @Before fun setUp() {
        server.start()
        baseUrl = server.url("/").toString()
        val storeFile = tmp.newFolder().resolve("cookies.preferences_pb")
        cookieJar = PersistentCookieJar(PersistentCookieJar.createStore(scope) { storeFile })
        client = ApiClients.okHttpClient(baseUrl, cookieJar, userAgent = "test", debugLogging = false)
        val api = ApiClients.retrofit(baseUrl, client).create<AuthApi>()
        repo = NetworkAuthRepository(api, cookieJar, ApiClients.json, verifyCallbackUrl = "https://birddex.fun/login")
    }

    @After fun tearDown() {
        server.close()
        scope.cancel()
    }

    private fun respond(code: Int, body: String, contentType: String = "application/json", vararg headers: Pair<String, String>) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", contentType)
                .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
                .body(body)
                .build(),
        )
    }

    private val sessionCookie = "Set-Cookie" to "better-auth.session_token=abc.sig; Max-Age=604800; Path=/; HttpOnly"
    private fun storedCookieNames() = cookieJar.loadForRequest(server.url("/api/sightings")).map { it.name }
    private fun RecordedRequest.bodyText() = body?.utf8().orEmpty()

    // --- sign in ---

    @Test fun signInSuccessStoresSessionAndSendsOrigin() = runBlocking {
        respond(200, """{"redirect":false,"token":"abc","user":$userJson}""", headers = arrayOf(sessionCookie))

        val result = repo.signIn("  t@example.com ", "pw")

        assertEquals("u1", (result as AuthResult.Success).value.id)
        assertEquals(SessionState.SignedIn(result.value), repo.sessionState.value)
        assertEquals(listOf("better-auth.session_token"), storedCookieNames())

        val request = server.takeRequest()
        assertEquals("/api/auth/sign-in/email", request.url.encodedPath)
        assertEquals(ApiClients.originOf(baseUrl), request.headers["Origin"])
        val body = request.bodyText()
        assertTrue(body, body.contains(""""email":"t@example.com"""") && body.contains(""""rememberMe":true"""))
    }

    @Test fun wrongPasswordIsInvalidCredentialsAndStaysSignedOut() = runBlocking {
        respond(401, """{"code":"INVALID_EMAIL_OR_PASSWORD","message":"Invalid email or password"}""")

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), repo.signIn("t@example.com", "nope"))
        assertTrue(repo.sessionState.value !is SessionState.SignedIn)
        assertTrue(storedCookieNames().isEmpty())
    }

    @Test fun unverifiedEmail() = runBlocking {
        respond(403, """{"code":"EMAIL_NOT_VERIFIED","message":"Email not verified"}""")
        assertEquals(AuthResult.Failure(AuthError.EmailNotVerified), repo.signIn("t@example.com", "pw"))
    }

    @Test fun rateLimited() = runBlocking {
        respond(429, "Too many requests. Please try again later.", contentType = "text/plain")
        assertEquals(AuthResult.Failure(AuthError.TooManyAttempts), repo.signIn("t@example.com", "pw"))
    }

    @Test fun htmlPageIsAnErrorNotSuccess() = runBlocking {
        // Uses a call whose body isn't parsed as JSON, so only HtmlErrorPageInterceptor can catch it.
        respond(200, "<!DOCTYPE html><html></html>", contentType = "text/html")
        val result = repo.resendVerificationEmail("t@example.com")
        assertTrue(result.toString(), result is AuthResult.Failure && result.error is AuthError.Unexpected)
    }

    @Test fun serverUnreachableIsNetworkError() = runBlocking {
        server.close()
        assertEquals(AuthResult.Failure(AuthError.Network), repo.signIn("t@example.com", "pw"))
        assertEquals(SessionState.Unknown, repo.sessionState.value)
    }

    // --- sign up ---

    @Test fun signUpSendsCallbackAndDoesNotSignIn() = runBlocking {
        respond(200, """{"token":null,"user":$userJson}""")

        assertEquals(AuthResult.Success(Unit), repo.signUp(" Test ", "t@example.com", "longpassword"))
        assertEquals(SessionState.Unknown, repo.sessionState.value)

        val request = server.takeRequest()
        assertEquals("/api/auth/sign-up/email", request.url.encodedPath)
        val body = request.bodyText()
        assertTrue(body, body.contains(""""name":"Test"""") && body.contains(""""callbackURL":"https://birddex.fun/login""""))
    }

    @Test fun signUpWithExistingEmail() = runBlocking {
        respond(422, """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL","message":"User already exists"}""")
        assertEquals(AuthResult.Failure(AuthError.EmailAlreadyInUse), repo.signUp("Test", "t@example.com", "longpassword"))
    }

    @Test fun resendVerificationEmail() = runBlocking {
        respond(200, """{"status":true}""")
        assertEquals(AuthResult.Success(Unit), repo.resendVerificationEmail("t@example.com"))
        assertEquals("/api/auth/send-verification-email", server.takeRequest().url.encodedPath)
    }

    // --- session ---

    @Test fun refreshSessionSignedOut() = runBlocking {
        respond(200, "null")
        assertEquals(AuthResult.Success(null), repo.refreshSession())
        assertEquals(SessionState.SignedOut, repo.sessionState.value)
    }

    @Test fun refreshSessionSignedIn() = runBlocking {
        respond(200, """{"session":{"id":"s1","expiresAt":"2026-11-26T00:00:00Z"},"user":$userJson}""")
        val result = repo.refreshSession() as AuthResult.Success
        assertEquals("u1", result.value?.id)
        assertEquals(SessionState.SignedIn(result.value!!), repo.sessionState.value)
    }

    @Test fun refreshSessionOfflineKeepsState() = runBlocking {
        respond(200, "null")
        repo.refreshSession()
        server.close()

        assertEquals(AuthResult.Failure(AuthError.Network), repo.refreshSession())
        assertEquals(SessionState.SignedOut, repo.sessionState.value)
    }

    // --- sign out ---

    @Test fun signOutSendsOriginAndClearsCookie() = runBlocking {
        respond(200, """{"redirect":false,"token":"abc","user":$userJson}""", headers = arrayOf(sessionCookie))
        repo.signIn("t@example.com", "pw")
        server.takeRequest()
        respond(200, """{"success":true}""")

        repo.signOut()

        val request = server.takeRequest()
        assertEquals("/api/auth/sign-out", request.url.encodedPath)
        assertEquals(ApiClients.originOf(baseUrl), request.headers["Origin"])
        assertTrue(request.headers["Cookie"].orEmpty().contains("better-auth.session_token=abc.sig"))
        assertEquals(SessionState.SignedOut, repo.sessionState.value)
        assertTrue(storedCookieNames().isEmpty())
    }

    @Test fun signOutClearsLocallyEvenIfServerFails() = runBlocking {
        respond(200, """{"redirect":false,"token":"abc","user":$userJson}""", headers = arrayOf(sessionCookie))
        repo.signIn("t@example.com", "pw")
        respond(500, """{"message":"boom"}""")

        repo.signOut()

        assertEquals(SessionState.SignedOut, repo.sessionState.value)
        assertTrue(storedCookieNames().isEmpty())
    }

    // --- interceptors ---

    @Test fun originIsOnlyAddedToAuthCalls() {
        respond(200, "[]")
        client.newCall(Request.Builder().url(server.url("/api/birds")).build()).execute().close()
        assertNull(server.takeRequest().headers["Origin"])
    }

    @Test fun originOfBaseUrl() {
        assertEquals("https://birddex.fun", ApiClients.originOf("https://birddex.fun"))
        assertEquals("https://birddex.fun", ApiClients.originOf("https://birddex.fun/"))
        assertEquals("http://localhost:8080", ApiClients.originOf("http://localhost:8080/api/"))
    }
}
