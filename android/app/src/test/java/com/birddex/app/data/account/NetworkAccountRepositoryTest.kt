package com.birddex.app.data.account

import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CookieJar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.create

class NetworkAccountRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repo: NetworkAccountRepository

    @Before fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        val client = ApiClients.okHttpClient(baseUrl, CookieJar.NO_COOKIES, userAgent = "test", debugLogging = false)
        repo = NetworkAccountRepository(ApiClients.retrofit(baseUrl, client).create<BirdDexApi>(), ApiClients.json)
    }

    @After fun tearDown() = server.close()

    private fun respond(code: Int, body: String) = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
    )

    @Test fun readsTheSettings() = runBlocking {
        respond(200, """{"galleryAnonymous":true}""")

        assertEquals(ApiResult.Success(AccountSettings(galleryAnonymous = true)), repo.getSettings())
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/account/settings", request.url.encodedPath)
    }

    @Test fun turningAnonymityOffSendsFalse() = runBlocking {
        respond(200, """{"galleryAnonymous":false}""")

        assertEquals(ApiResult.Success(AccountSettings(galleryAnonymous = false)), repo.setGalleryAnonymous(false))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/account/settings", request.url.encodedPath)
        // Not "{}": the server answers 400 unless the flag is a boolean.
        assertEquals("""{"galleryAnonymous":false}""", request.body?.utf8())
    }

    @Test fun turningAnonymityOnSendsTrue() = runBlocking {
        respond(200, """{"galleryAnonymous":true}""")

        repo.setGalleryAnonymous(true)

        assertEquals("""{"galleryAnonymous":true}""", server.takeRequest().body?.utf8())
    }

    @Test fun failuresAreReported() = runBlocking {
        respond(400, """{"error":"galleryAnonymous must be a boolean"}""")
        assertEquals(
            ApiResult.Failure(ApiError.Http(400, "galleryAnonymous must be a boolean")),
            repo.setGalleryAnonymous(true),
        )

        respond(401, """{"error":"Unauthorized"}""")
        assertEquals(ApiResult.Failure(ApiError.Unauthorized), repo.getSettings())

        server.close()
        assertEquals(ApiResult.Failure(ApiError.Network), repo.getSettings())
    }
}
