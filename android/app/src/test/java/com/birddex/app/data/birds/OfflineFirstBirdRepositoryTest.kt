package com.birddex.app.data.birds

import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CookieJar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.create

/** In-memory stand-in for Room's BirdDao (the real one is tested on a device in androidTest). */
class FakeBirdDao : BirdDao {
    val rows = MutableStateFlow<List<BirdEntity>>(emptyList())
    override fun observeAll() = rows.map { list -> list.sortedBy { it.name } }
    override suspend fun deleteAll() { rows.value = emptyList() }
    override suspend fun insertAll(birds: List<BirdEntity>) { rows.value = rows.value + birds }
}

/** GET /api/birds through the real HTTP stack into the offline copy, using the live response. */
class OfflineFirstBirdRepositoryTest {

    private val server = MockWebServer()
    private val dao = FakeBirdDao()
    private lateinit var repo: OfflineFirstBirdRepository

    /** Copy of https://birddex.fun/api/birds saved on 2026-11-19 (public data). */
    private val liveResponse: String =
        javaClass.classLoader!!.getResource("fixtures/birds.json")!!.readText(Charsets.UTF_8)

    @Before fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        val client = ApiClients.okHttpClient(baseUrl, CookieJar.NO_COOKIES, userAgent = "test", debugLogging = false)
        repo = OfflineFirstBirdRepository(ApiClients.retrofit(baseUrl, client).create<BirdDexApi>(), dao, ApiClients.json)
    }

    @After fun tearDown() = server.close()

    private fun respond(code: Int, body: String, contentType: String = "application/json; charset=utf-8") =
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", contentType).body(body).build())

    private suspend fun saved(): List<Bird> = repo.observeBirds().first()

    private suspend fun refreshWithLiveData(): List<Bird> {
        respond(200, liveResponse)
        assertEquals(ApiResult.Success(Unit), repo.refresh())
        return saved()
    }

    // --- parsing the real response ---

    @Test fun savesEveryBirdInTheLiveResponse() = runBlocking {
        assertTrue(saved().isEmpty()) // nothing saved before the first refresh
        val birds = refreshWithLiveData()

        assertEquals(36, birds.size)
        assertEquals(36, birds.map { it.slug }.toSet().size)
        assertTrue(birds.all { it.name.isNotBlank() && it.scientificName.isNotBlank() })
        // Every stat is filled in today's data, so a renamed field would show up as nulls here.
        assertTrue(birds.all { b ->
            listOf(b.wingspan, b.length, b.weight, b.conservationStatus, b.habitat, b.diet,
                b.callDescription, b.funFact, b.rarityValue, b.appearance).all { !it.isNullOrBlank() }
        })
        assertEquals("/api/birds", server.takeRequest().url.encodedPath)
    }

    @Test fun knownBirdMatchesTheWebsite() = runBlocking {
        val parrot = refreshWithLiveData().first { it.slug == "australian_king_parrot" }

        assertEquals(1, parrot.id)
        assertEquals("Australian King Parrot", parrot.name)
        assertEquals("Alisterus scapularis", parrot.scientificName)
        assertEquals("55–60 cm", parrot.wingspan) // en dash survives UTF-8 decoding
        assertEquals("Least Concern", parrot.conservationStatus)
        assertEquals(Rarity.Uncommon, parrot.rarityLevel)
    }

    @Test fun rarityCountsMatchTheDatabase() = runBlocking {
        val counts = refreshWithLiveData().groupingBy { it.rarityLevel }.eachCount()
        assertEquals(mapOf(Rarity.Common to 26, Rarity.Uncommon to 7, Rarity.Rare to 3), counts)
    }

    @Test fun missingStatsAndUnknownRarityAreTolerated() = runBlocking {
        respond(200, """[{"id":99,"name":"New Bird","scientific_name":"Avis nova","slug":"new_bird","rarity":"legendary","extra":1}]""")
        repo.refresh()
        val bird = saved().single()

        assertEquals(null, bird.wingspan)
        assertEquals(Rarity.Common, bird.rarityLevel)
    }

    // --- offline behaviour ---

    @Test fun failedRefreshKeepsSavedBirds() = runBlocking {
        refreshWithLiveData()

        respond(500, """{"error":"Database unavailable"}""")
        assertEquals(ApiResult.Failure(ApiError.Http(500, "Database unavailable")), repo.refresh())
        assertEquals(36, saved().size)

        server.close() // airplane mode
        assertEquals(ApiResult.Failure(ApiError.Network), repo.refresh())
        assertEquals(36, saved().size)
    }

    @Test fun emptyServerListNeverWipesSavedBirds() = runBlocking {
        refreshWithLiveData()
        respond(200, "[]")

        assertTrue(repo.refresh() is ApiResult.Failure)
        assertEquals(36, saved().size)
    }

    @Test fun refreshDropsBirdsRemovedOnTheServer() = runBlocking {
        refreshWithLiveData()
        respond(200, """[{"id":1,"name":"Australian King Parrot","scientific_name":"Alisterus scapularis","slug":"australian_king_parrot"}]""")
        repo.refresh()

        assertEquals(listOf("australian_king_parrot"), saved().map { it.slug })
    }

    @Test fun unreadableResponsesAreErrors() = runBlocking {
        respond(200, "<!DOCTYPE html><html></html>", contentType = "text/html")
        val html = repo.refresh()
        assertTrue(html.toString(), html is ApiResult.Failure && html.error is ApiError.Unexpected)

        respond(200, """{"not":"a list"}""")
        val wrongShape = repo.refresh()
        assertTrue(wrongShape.toString(), wrongShape is ApiResult.Failure && wrongShape.error is ApiError.Unexpected)
    }

    @Test fun entityRoundTripKeepsEveryField() = runBlocking {
        val birds = refreshWithLiveData()
        assertEquals(birds, birds.map { it.toEntity().toBird() })
    }

    @Test fun rarityFrom() {
        assertEquals(Rarity.Rare, Rarity.from(" RARE "))
        assertEquals(Rarity.Uncommon, Rarity.from("uncommon"))
        assertEquals(Rarity.Common, Rarity.from(null))
    }
}
