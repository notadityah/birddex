package com.birddex.app.data.detect

import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CookieJar
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.create

class NetworkDetectRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repo: NetworkDetectRepository

    /** Shaped exactly like the detect Lambda's answer: snake_case bird rows, nullable stats. */
    private val fixture: String =
        javaClass.classLoader!!.getResource("fixtures/detect.json")!!.readText(Charsets.UTF_8)

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())

    @Before fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        val client = ApiClients.okHttpClient(baseUrl, CookieJar.NO_COOKIES, userAgent = "test", debugLogging = false)
        repo = NetworkDetectRepository(ApiClients.retrofit(baseUrl, client).create<BirdDexApi>(), ApiClients.json)
    }

    @After fun tearDown() = server.close()

    private fun respond(code: Int, body: String, contentType: String = "application/json") = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", contentType).body(body).build(),
    )

    @Test fun sendsThePhotoAsBase64AndAsksForFiveMatches() = runBlocking {
        respond(200, fixture)

        repo.detect(jpeg)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/detect", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))
        val body = ApiClients.json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals(setOf("imageBase64", "topN"), body.keys)
        // Plain base64 without a data: prefix or line breaks, which the server decodes directly.
        val base64 = body["imageBase64"]!!.jsonPrimitive.content
        assertEquals("/9gBAgP/2Q==", base64)
        assertArrayEquals(jpeg, Base64.getDecoder().decode(base64))
        assertEquals(5, body["topN"]!!.jsonPrimitive.int)
    }

    @Test fun readsMatchesBestFirstWithEveryBird() = runBlocking {
        respond(200, fixture)

        val detection = (repo.detect(jpeg) as ApiResult.Success).value

        assertEquals(listOf("house_sparrow", "spotted_turtle_dove", "removed_bird"), detection.predictions.map { it.label })
        assertEquals(listOf(91.37, 4.2, 0.5), detection.predictions.map { it.confidence })
        assertEquals("House Sparrow", detection.bestMatch!!.name)
        assertEquals("Passer domesticus", detection.bestMatch!!.scientificName)
        assertEquals(setOf("house_sparrow", "spotted_turtle_dove"), detection.birds.keys)
        // A bird row without stats still reads.
        with(detection.birds.getValue("spotted_turtle_dove")) {
            assertEquals("Spotted Turtle-dove", name)
            assertNull(wingspan)
        }
    }

    @Test fun noBirdFoundIsAnAnswerNotAnError() = runBlocking {
        respond(200, """{"predictions":[],"bird":null,"birds":{}}""")

        assertEquals(ApiResult.Success(Detection()), repo.detect(jpeg))
    }

    @Test fun failuresAreReported() = runBlocking {
        respond(500, """{"error":"Internal server error"}""")
        assertEquals(ApiResult.Failure(ApiError.Http(500, "Internal server error")), repo.detect(jpeg))

        respond(401, """{"error":"Unauthorized"}""")
        assertEquals(ApiResult.Failure(ApiError.Unauthorized), repo.detect(jpeg))

        // A gateway or CloudFront error page instead of JSON.
        respond(200, "<html><body>Service Unavailable</body></html>", contentType = "text/html")
        assertTrue((repo.detect(jpeg) as ApiResult.Failure).error is ApiError.Unexpected)

        server.close() // offline
        assertEquals(ApiResult.Failure(ApiError.Network), repo.detect(jpeg))
    }
}
