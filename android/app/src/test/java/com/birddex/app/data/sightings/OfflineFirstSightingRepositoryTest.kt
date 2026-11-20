package com.birddex.app.data.sightings

import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.ui.auth.FakeAuthRepository
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CookieJar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.create

/** In-memory stand-in for Room's SightingDao (the real one is tested on a device). */
class FakeSightingDao : SightingDao {
    val rows = MutableStateFlow<List<SightingEntity>>(emptyList())
    override fun observeAll() = rows.map { list -> list.sortedByDescending { it.createdAt } }
    override suspend fun deleteAll() { rows.value = emptyList() }
    override suspend fun insertAll(sightings: List<SightingEntity>) { rows.value = rows.value + sightings }
    override suspend fun setPublic(id: String, isPublic: Boolean) {
        rows.value = rows.value.map { if (it.id == id) it.copy(isPublic = isPublic) else it }
    }
    override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
}

class OfflineFirstSightingRepositoryTest {

    private val server = MockWebServer()
    private val dao = FakeSightingDao()
    private val auth = FakeAuthRepository().apply { state.value = SessionState.SignedIn(user) }
    private lateinit var repo: OfflineFirstSightingRepository

    /** Rows shaped exactly like GET /api/sightings (postgres.js timestamps, nullable columns). */
    private val fixture: String =
        javaClass.classLoader!!.getResource("fixtures/sightings.json")!!.readText(Charsets.UTF_8)

    @Before fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        val client = ApiClients.okHttpClient(baseUrl, CookieJar.NO_COOKIES, userAgent = "test", debugLogging = false)
        repo = OfflineFirstSightingRepository(ApiClients.retrofit(baseUrl, client).create<BirdDexApi>(), dao, auth, ApiClients.json)
    }

    @After fun tearDown() = server.close()

    private fun respond(code: Int, body: String) = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
    )

    private suspend fun saved() = repo.observeSightings().first()

    private suspend fun refreshWithFixture(): List<Sighting> {
        respond(200, fixture)
        assertEquals(ApiResult.Success(Unit), repo.refresh())
        return saved()
    }

    @Test fun savesSightingsNewestFirst() = runBlocking {
        val sightings = refreshWithFixture()

        assertEquals(3, sightings.size)
        assertEquals(listOf("2026-11-18", "2026-11-10", "2026-11-01"), sightings.map { it.createdAt.take(10) })
        assertEquals("/api/sightings", server.takeRequest().url.encodedPath)
    }

    @Test fun fullSightingKeepsEveryField() = runBlocking {
        val s = refreshWithFixture().first()

        assertEquals("6f1c2a3e-8b4d-4c5e-9f60-1a2b3c4d5e6f", s.id)
        assertEquals(1, s.birdId)
        assertEquals("australian_king_parrot", s.slug)
        assertEquals("Australian King Parrot", s.birdName)
        assertEquals("images/u_test/0b7e9c1d-2f3a-4b5c-8d9e-0f1a2b3c4d5e.jpg", s.imageKey)
        assertTrue(s.imageUrl!!.contains("X-Amz-Signature"))
        assertEquals("Two males at the feeder", s.notes)
        assertTrue(s.isPublic)
        assertEquals(Instant.parse("2026-11-18T03:12:45Z"), s.seenAt)
    }

    @Test fun minimalSightingWithNulls() = runBlocking {
        val s = refreshWithFixture().first { it.slug == "australian_magpie" }

        assertNull(s.imageKey)
        assertNull(s.imageUrl)
        assertNull(s.notes)
        assertFalse(s.isPublic) // "public": null counts as not shared
        assertEquals(Instant.parse("2026-11-10T22:01:00Z"), s.seenAt) // falls back to created_at
    }

    @Test fun entityRoundTripKeepsEveryField() = runBlocking {
        val sightings = refreshWithFixture()
        assertEquals(sightings.map { it.copy(publicValue = it.isPublic) }, sightings.map { it.toEntity().toSighting() })
    }

    @Test fun noSightingsYetIsValidAndClearsTheCopy() = runBlocking {
        refreshWithFixture()
        respond(200, "[]")

        assertEquals(ApiResult.Success(Unit), repo.refresh())
        assertTrue(saved().isEmpty())
    }

    @Test fun failedRefreshKeepsSavedSightings() = runBlocking {
        refreshWithFixture()

        respond(500, """{"error":"Database unavailable"}""")
        assertEquals(ApiResult.Failure(ApiError.Http(500, "Database unavailable")), repo.refresh())

        server.close() // airplane mode
        assertEquals(ApiResult.Failure(ApiError.Network), repo.refresh())
        assertEquals(3, saved().size)
    }

    @Test fun unauthorizedIsReported() = runBlocking {
        respond(401, """{"error":"Unauthorized"}""")
        assertEquals(ApiResult.Failure(ApiError.Unauthorized), repo.refresh())
    }

    // --- share toggle (A3.6) ---

    private val parrotId = "6f1c2a3e-8b4d-4c5e-9f60-1a2b3c4d5e6f" // public in the fixture
    private suspend fun savedById(id: String) = saved().firstOrNull { it.id == id }

    @Test fun unshareSendsPatchAndKeepsTheChange() = runBlocking {
        refreshWithFixture()
        server.takeRequest()
        respond(200, """{"id":"$parrotId","public":false}""")

        assertEquals(ApiResult.Success(Unit), repo.setPublic(parrotId, false))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/sightings/$parrotId", request.url.encodedPath)
        assertEquals("""{"public":false}""", request.body?.utf8())
        assertFalse(savedById(parrotId)!!.isPublic)
    }

    @Test fun shareFailureRollsBack() = runBlocking {
        refreshWithFixture()
        respond(500, """{"error":"boom"}""")

        assertTrue(repo.setPublic(parrotId, false) is ApiResult.Failure)
        assertTrue(savedById(parrotId)!!.isPublic) // put back
    }

    @Test fun sharingASightingDeletedElsewhereRemovesIt() = runBlocking {
        refreshWithFixture()
        respond(404, """{"error":"Not found"}""")

        repo.setPublic(parrotId, false)

        assertNull(savedById(parrotId))
    }

    // --- delete (A3.6) ---

    @Test fun deleteSendsDeleteThenRemovesLocally() = runBlocking {
        refreshWithFixture()
        server.takeRequest()
        respond(200, """{"ok":true}""")

        assertEquals(ApiResult.Success(Unit), repo.delete(parrotId))

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/sightings/$parrotId", request.url.encodedPath)
        assertNull(savedById(parrotId))
        assertEquals(2, saved().size)
    }

    @Test fun failedDeleteKeepsTheSighting() = runBlocking {
        refreshWithFixture()
        server.close() // offline: nothing may disappear without the server's OK

        assertEquals(ApiResult.Failure(ApiError.Network), repo.delete(parrotId))
        assertEquals(3, saved().size)
    }

    @Test fun deletingSomethingAlreadyGoneCountsAsDone() = runBlocking {
        refreshWithFixture()
        respond(404, """{"error":"Not found"}""")

        assertEquals(ApiResult.Success(Unit), repo.delete(parrotId))
        assertNull(savedById(parrotId))
    }

    @Test fun signedOutDuringDownloadDoesNotSaveTheOldAccountsSightings() = runBlocking {
        auth.state.value = SessionState.SignedOut
        respond(200, fixture)

        repo.refresh()

        assertTrue(saved().isEmpty())
    }

    // --- saving a new sighting (A4.5) ---

    private val photo = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 42, 0xFF.toByte(), 0xD9.toByte())
    private val newId = "9d7e6f5a-4b3c-4d2e-8f1a-0b9c8d7e6f5a"

    /** The 201 answer, with an upload link that points at the fake server like a presigned S3 URL. */
    private fun respondCreated() = respond(
        201,
        """{"id":"$newId","imageKey":"images/u_test/abc.jpg","uploadUrl":"${server.url("/images/u_test/abc.jpg?X-Amz-Signature=sig")}"}""",
    )

    @Test fun createRecordsTheSightingUploadsThePhotoAndRefreshes() = runBlocking {
        respondCreated()
        respond(200, "") // S3 answers an empty 200
        respond(200, fixture)

        assertEquals(ApiResult.Success(Unit), repo.create(birdId = 14, notes = "At the feeder", isPublic = true, photoJpeg = photo))

        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/api/sightings", create.url.encodedPath)
        assertEquals("""{"birdId":14,"imageExt":"jpg","notes":"At the feeder","public":true}""", create.body?.utf8())

        val upload = server.takeRequest()
        assertEquals("PUT", upload.method)
        assertEquals("/images/u_test/abc.jpg", upload.url.encodedPath)
        assertEquals("sig", upload.url.queryParameter("X-Amz-Signature"))
        assertEquals("image/jpeg", upload.headers["Content-Type"])
        assertEquals(photo.toList(), upload.body!!.toByteArray().toList())

        assertEquals("/api/sightings", server.takeRequest().url.encodedPath) // the refresh
        assertEquals(3, saved().size)
    }

    @Test fun noNotesAndNotSharedAreSentLikeTheWebsite() = runBlocking {
        respondCreated()
        respond(200, "")
        respond(200, fixture)

        repo.create(birdId = 14, notes = null, isPublic = false, photoJpeg = photo)

        assertEquals("""{"birdId":14,"imageExt":"jpg","public":false}""", server.takeRequest().body?.utf8())
    }

    @Test fun failedUploadDeletesTheNewSightingAgain() = runBlocking {
        respondCreated()
        respond(403, "<Error><Code>AccessDenied</Code></Error>") // e.g. the 5-minute link expired
        respond(200, """{"ok":true}""")

        val result = repo.create(birdId = 14, notes = null, isPublic = false, photoJpeg = photo)

        assertTrue(result is ApiResult.Failure)
        assertEquals("POST", server.takeRequest().method)
        assertEquals("PUT", server.takeRequest().method)
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/sightings/$newId", delete.url.encodedPath)
        assertEquals(3, server.requestCount) // no refresh: nothing was saved
    }

    @Test fun failedCreateStopsThere() = runBlocking {
        respond(400, """{"error":"birdId must be a positive number"}""")

        assertEquals(
            ApiResult.Failure(ApiError.Http(400, "birdId must be a positive number")),
            repo.create(birdId = 0, notes = null, isPublic = false, photoJpeg = photo),
        )
        assertEquals(1, server.requestCount)
    }

    // --- reset collection (A5.3) ---

    @Test fun deleteAllClearsTheServerThenThePhone() = runBlocking {
        refreshWithFixture()
        server.takeRequest()
        respond(200, """{"ok":true,"deleted":3}""")

        assertEquals(ApiResult.Success(3), repo.deleteAll())

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/sightings", request.url.encodedPath)
        assertTrue(saved().isEmpty())
    }

    @Test fun failedDeleteAllKeepsEverything() = runBlocking {
        refreshWithFixture()
        server.close() // offline: nothing may disappear without the server's OK

        assertEquals(ApiResult.Failure(ApiError.Network), repo.deleteAll())
        assertEquals(3, saved().size)
    }

    @Test fun savedEvenIfTheRefreshAfterwardsFails() = runBlocking {
        respondCreated()
        respond(200, "")
        respond(500, """{"error":"Database unavailable"}""")

        assertEquals(ApiResult.Success(Unit), repo.create(birdId = 14, notes = null, isPublic = false, photoJpeg = photo))
    }
}
