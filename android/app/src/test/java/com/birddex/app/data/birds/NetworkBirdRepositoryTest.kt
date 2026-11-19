package com.birddex.app.data.birds

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.create

/** GET /api/birds through the real HTTP stack, using a copy of the live response. */
class NetworkBirdRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repo: NetworkBirdRepository

    /** Copy of https://birddex.fun/api/birds saved on 2026-11-19 (public data). */
    private val liveResponse: String =
        javaClass.classLoader!!.getResource("fixtures/birds.json")!!.readText(Charsets.UTF_8)

    @Before fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        val client = ApiClients.okHttpClient(baseUrl, CookieJar.NO_COOKIES, userAgent = "test", debugLogging = false)
        repo = NetworkBirdRepository(ApiClients.retrofit(baseUrl, client).create<BirdDexApi>(), ApiClients.json)
    }

    @After fun tearDown() = server.close()

    private fun respond(code: Int, body: String, contentType: String = "application/json; charset=utf-8") =
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", contentType).body(body).build())

    private suspend fun birds(): List<Bird> = (repo.getBirds() as ApiResult.Success).value

    @Test fun parsesEveryBirdInTheLiveResponse() = runBlocking {
        respond(200, liveResponse)
        val birds = birds()

        assertEquals(36, birds.size)
        assertEquals(36, birds.map { it.slug }.toSet().size)
        assertEquals(birds.sortedBy { it.name }.map { it.name }, birds.map { it.name })
        assertTrue(birds.all { it.name.isNotBlank() && it.scientificName.isNotBlank() })
        // Every stat is filled in today's data, so a renamed field would show up as nulls here.
        assertTrue(birds.all { b ->
            listOf(b.wingspan, b.length, b.weight, b.conservationStatus, b.habitat, b.diet,
                b.callDescription, b.funFact, b.rarityValue, b.appearance).all { !it.isNullOrBlank() }
        })
        assertEquals("/api/birds", server.takeRequest().url.encodedPath)
    }

    @Test fun knownBirdMatchesTheWebsite() = runBlocking {
        respond(200, liveResponse)
        val parrot = birds().first { it.slug == "australian_king_parrot" }

        assertEquals(1, parrot.id)
        assertEquals("Australian King Parrot", parrot.name)
        assertEquals("Alisterus scapularis", parrot.scientificName)
        assertEquals("55–60 cm", parrot.wingspan) // en dash survives UTF-8 decoding
        assertEquals("Least Concern", parrot.conservationStatus)
        assertEquals(Rarity.Uncommon, parrot.rarityLevel)
    }

    @Test fun rarityCountsMatchTheDatabase() = runBlocking {
        respond(200, liveResponse)
        val counts = birds().groupingBy { it.rarityLevel }.eachCount()
        assertEquals(mapOf(Rarity.Common to 26, Rarity.Uncommon to 7, Rarity.Rare to 3), counts)
    }

    @Test fun missingStatsAndUnknownRarityAreTolerated() = runBlocking {
        respond(200, """[{"id":99,"name":"New Bird","scientific_name":"Avis nova","slug":"new_bird","rarity":"legendary","extra":1}]""")
        val bird = birds().single()

        assertEquals(null, bird.wingspan)
        assertEquals(Rarity.Common, bird.rarityLevel)
    }

    @Test fun errors() = runBlocking {
        respond(500, """{"error":"Database unavailable"}""")
        assertEquals(ApiResult.Failure(ApiError.Http(500, "Database unavailable")), repo.getBirds())

        respond(200, "<!DOCTYPE html><html></html>", contentType = "text/html")
        val html = repo.getBirds()
        assertTrue(html.toString(), html is ApiResult.Failure && html.error is ApiError.Unexpected)

        respond(200, """{"not":"a list"}""")
        val wrongShape = repo.getBirds()
        assertTrue(wrongShape.toString(), wrongShape is ApiResult.Failure && wrongShape.error is ApiError.Unexpected)

        server.close()
        assertEquals(ApiResult.Failure(ApiError.Network), repo.getBirds())
    }

    @Test fun rarityFrom() {
        assertEquals(Rarity.Rare, Rarity.from(" RARE "))
        assertEquals(Rarity.Uncommon, Rarity.from("uncommon"))
        assertEquals(Rarity.Common, Rarity.from(null))
    }
}
