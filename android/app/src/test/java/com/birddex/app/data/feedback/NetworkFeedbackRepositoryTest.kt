package com.birddex.app.data.feedback

import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CookieJar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.create

class NetworkFeedbackRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repo: NetworkFeedbackRepository
    private val photo = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 7, 0xFF.toByte(), 0xD9.toByte())

    @Before fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        val client = ApiClients.okHttpClient(baseUrl, CookieJar.NO_COOKIES, userAgent = "test", debugLogging = false)
        repo = NetworkFeedbackRepository(ApiClients.retrofit(baseUrl, client).create<BirdDexApi>(), ApiClients.json)
    }

    @After fun tearDown() = server.close()

    private fun respond(code: Int, body: String) = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
    )

    private fun respondCreated() = respond(
        201,
        """{"id":"fb-1","uploadUrl":"${server.url("/images/feedback/u_test/abc.jpg?X-Amz-Signature=sig")}"}""",
    )

    private suspend fun sendDetection(message: String = "Wrong match", withPhoto: Boolean = true) =
        repo.send(FeedbackCategory.Detection, message, FeedbackRepository.appPage("/detect"), photo.takeIf { withPhoto })

    @Test fun sendsTheReportThenItsPhoto() = runBlocking {
        respondCreated()
        respond(200, "")

        assertEquals(ApiResult.Success(FeedbackSent(photoUploaded = true)), sendDetection())

        val report = server.takeRequest()
        assertEquals("POST", report.method)
        assertEquals("/api/feedback", report.url.encodedPath)
        assertEquals(
            """{"category":"detection","message":"Wrong match","pageUrl":"android:/detect","imageExt":"jpg"}""",
            report.body?.utf8(),
        )
        val upload = server.takeRequest()
        assertEquals("PUT", upload.method)
        assertEquals("/images/feedback/u_test/abc.jpg", upload.url.encodedPath)
        assertEquals("image/jpeg", upload.headers["Content-Type"])
        assertEquals(photo.toList(), upload.body!!.toByteArray().toList())
    }

    @Test fun withoutAPhotoNothingIsUploaded() = runBlocking {
        respond(201, """{"id":"fb-1"}""")

        assertEquals(ApiResult.Success(FeedbackSent(photoUploaded = true)), sendDetection(withPhoto = false))

        assertEquals(
            """{"category":"detection","message":"Wrong match","pageUrl":"android:/detect"}""",
            server.takeRequest().body?.utf8(),
        )
        assertEquals(1, server.requestCount)
    }

    @Test fun failedPhotoUploadStillCountsAsSent() = runBlocking {
        respondCreated()
        respond(403, "<Error><Code>AccessDenied</Code></Error>")

        // Users can't delete feedback, and sending again would file the report twice.
        assertEquals(ApiResult.Success(FeedbackSent(photoUploaded = false)), sendDetection())
        assertEquals(2, server.requestCount)
    }

    @Test fun longMessagesAreCutToTheApisLimit() = runBlocking {
        respond(201, """{"id":"fb-1"}""")

        sendDetection(message = "x".repeat(2500), withPhoto = false)

        val sent = ApiClients.json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals(2000, sent["message"]!!.jsonPrimitive.content.length)
    }

    @Test fun failuresAreReported() = runBlocking {
        respond(400, """{"error":"message is required and must be at most 2000 characters"}""")
        assertEquals(
            ApiResult.Failure(ApiError.Http(400, "message is required and must be at most 2000 characters")),
            sendDetection(),
        )

        server.close()
        assertEquals(ApiResult.Failure(ApiError.Network), sendDetection())
    }
}
