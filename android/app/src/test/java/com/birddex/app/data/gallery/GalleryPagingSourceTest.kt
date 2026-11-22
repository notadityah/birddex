package com.birddex.app.data.gallery

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.BirdDexApi
import java.time.Instant
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CookieJar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.create

class GalleryPagingSourceTest {

    private val server = MockWebServer()
    private lateinit var api: BirdDexApi

    /** Shaped exactly like GET /api/gallery: snake_case, nullable columns, "Anonymous". */
    private val fixture: String =
        javaClass.classLoader!!.getResource("fixtures/gallery.json")!!.readText(Charsets.UTF_8)

    @Before fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        val client = ApiClients.okHttpClient(baseUrl, CookieJar.NO_COOKIES, userAgent = "test", debugLogging = false)
        api = ApiClients.retrofit(baseUrl, client).create()
    }

    @After fun tearDown() = server.close()

    private fun respond(code: Int, body: String) = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
    )

    private fun page(ids: List<String>, total: Int) =
        """{"sightings":[${ids.joinToString { """{"id":"$it","bird_id":1,"bird_name":"Galah","slug":"galah","created_at":"2026-11-19T00:00:00Z"}""" }}],"total":$total}"""

    private fun source(birdId: Int? = null) = GalleryPagingSource(api, ApiClients.json, birdId)

    private suspend fun GalleryPagingSource.first() = load(LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
    private suspend fun GalleryPagingSource.after(offset: Int) = load(LoadParams.Append(key = offset, loadSize = 20, placeholdersEnabled = false))

    @Test fun firstPageIsTheNewestTwenty() = runBlocking {
        respond(200, fixture)

        val result = source().first() as LoadResult.Page

        val request = server.takeRequest()
        assertEquals("/api/gallery", request.url.encodedPath)
        assertEquals("20", request.url.queryParameter("limit"))
        assertEquals("0", request.url.queryParameter("offset"))
        assertNull(request.url.queryParameter("birdId"))
        assertEquals(2, result.data.size)
        assertNull(result.prevKey)
        assertEquals(2, result.nextKey) // 2 of 45 so far
    }

    @Test fun everyFieldIsRead() = runBlocking {
        respond(200, fixture)

        val (shared, anonymous) = (source().first() as LoadResult.Page).data

        assertEquals("House Sparrow", shared.birdName)
        assertEquals(14, shared.birdId)
        assertEquals("house_sparrow", shared.slug)
        assertEquals("Jane Doe", shared.userName)
        assertEquals("clayton station", shared.notes)
        assertTrue(shared.imageUrl!!.contains("X-Amz-Signature"))
        assertEquals(Instant.parse("2026-11-19T22:41:07.512Z"), shared.sharedAt)
        assertEquals("gallery/3c1d2e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f", shared.photoCacheKey)

        assertEquals("Anonymous", anonymous.userName)
        assertNull(anonymous.imageUrl)
        assertNull(anonymous.notes)
        assertNull(anonymous.detectedAt)
    }

    @Test fun nextPagesContinueFromTheOffset() = runBlocking {
        respond(200, page(listOf("c", "d"), total = 45))

        val result = source().after(20) as LoadResult.Page

        assertEquals("20", server.takeRequest().url.queryParameter("offset"))
        assertEquals(22, result.nextKey)
    }

    @Test fun stopsAtTheEnd() = runBlocking {
        respond(200, page(listOf("x", "y"), total = 22))
        assertNull((source().after(20) as LoadResult.Page).nextKey) // 22 of 22

        respond(200, page(emptyList(), total = 22)) // someone unshared meanwhile
        assertNull((source().after(22) as LoadResult.Page).nextKey)
    }

    @Test fun speciesFilterIsSent() = runBlocking {
        respond(200, fixture)

        source(birdId = 14).first()

        assertEquals("14", server.takeRequest().url.queryParameter("birdId"))
    }

    @Test fun repeatsFromAShiftedFeedAreDropped() = runBlocking {
        val source = source()
        respond(200, page(listOf("a", "b"), total = 10))
        respond(200, page(listOf("b", "c"), total = 11)) // a new share pushed "b" onto the next page

        source.first()
        val next = source.after(2) as LoadResult.Page

        assertEquals(listOf("c"), next.data.map { it.id })
        assertEquals(4, next.nextKey) // still counts what the server sent
    }

    @Test fun failuresBecomeLoadErrors() = runBlocking {
        respond(401, """{"error":"Unauthorized"}""")
        assertEquals(ApiError.Unauthorized, ((source().first() as LoadResult.Error).throwable as GalleryLoadException).error)

        respond(500, """{"error":"Internal server error"}""")
        assertEquals(
            ApiError.Http(500, "Internal server error"),
            ((source().first() as LoadResult.Error).throwable as GalleryLoadException).error,
        )

        server.close()
        assertEquals(ApiError.Network, ((source().first() as LoadResult.Error).throwable as GalleryLoadException).error)
    }
}
