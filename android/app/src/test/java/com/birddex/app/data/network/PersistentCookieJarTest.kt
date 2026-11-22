package com.birddex.app.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersistentCookieJarTest {

    @get:Rule val tmp = TemporaryFolder()

    private val api = "https://birddex.fun/api/sightings".toHttpUrl()
    private val otherSite = "https://example.com/".toHttpUrl()
    private var clock = 1_000_000L
    private val scopes = mutableListOf<CoroutineScope>()
    private lateinit var storeFile: java.io.File

    @Before fun setUp() {
        storeFile = tmp.newFolder().resolve("cookies.preferences_pb")
    }

    @After fun tearDown() = scopes.forEach { it.cancel() }

    /** A DataStore on [storeFile]. Close the previous one first (one active store per file). */
    private fun newStore(): DataStore<Preferences> {
        scopes.forEach { it.cancel() }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PersistentCookieJar.createStore(scope) { storeFile }
    }

    private fun newJar() = PersistentCookieJar(newStore(), now = { clock })

    private fun sessionCookie(value: String = "abc.sig", maxAgeMs: Long = 60_000) =
        Cookie.Builder()
            .name("__Secure-better-auth.session_token")
            .value(value)
            .hostOnlyDomain("birddex.fun")
            .path("/")
            .expiresAt(clock + maxAgeMs)
            .secure()
            .httpOnly()
            .build()

    @Test fun savedCookieIsSentBackToSameSiteOnly() {
        val jar = newJar()
        jar.saveFromResponse(api, listOf(sessionCookie()))

        assertEquals(listOf("abc.sig"), jar.loadForRequest(api).map { it.value })
        assertTrue(jar.loadForRequest(otherSite).isEmpty())
    }

    @Test fun cookieSurvivesAppRestart() {
        newJar().saveFromResponse(api, listOf(sessionCookie()))

        val restarted = newJar()
        val cookie = restarted.loadForRequest(api).single()
        assertEquals("abc.sig", cookie.value)
        assertTrue(cookie.secure && cookie.httpOnly && cookie.hostOnly)
    }

    @Test fun newValueReplacesOldOne() {
        val jar = newJar()
        jar.saveFromResponse(api, listOf(sessionCookie("old")))
        jar.saveFromResponse(api, listOf(sessionCookie("new")))

        assertEquals(listOf("new"), jar.loadForRequest(api).map { it.value })
    }

    @Test fun expiredCookieIsDroppedAndNotReloaded() {
        val jar = newJar()
        jar.saveFromResponse(api, listOf(sessionCookie(maxAgeMs = 1_000)))
        clock += 2_000

        assertTrue(jar.loadForRequest(api).isEmpty())
        assertTrue(newJar().loadForRequest(api).isEmpty())
    }

    @Test fun serverDeletingCookieRemovesIt() {
        val jar = newJar()
        jar.saveFromResponse(api, listOf(sessionCookie()))
        val deletion = Cookie.Builder().name("__Secure-better-auth.session_token").value("")
            .hostOnlyDomain("birddex.fun").path("/").expiresAt(clock - 1).build()
        jar.saveFromResponse(api, listOf(deletion))

        assertTrue(jar.loadForRequest(api).isEmpty())
        assertTrue(newJar().loadForRequest(api).isEmpty())
    }

    @Test fun sessionOnlyCookieIsNotWrittenToDisk() {
        val jar = newJar()
        val sessionOnly = Cookie.Builder().name("tmp").value("1").hostOnlyDomain("birddex.fun").build()
        jar.saveFromResponse(api, listOf(sessionOnly))

        assertEquals(listOf("tmp"), jar.loadForRequest(api).map { it.name })
        assertTrue(newJar().loadForRequest(api).isEmpty())
    }

    @Test fun clearForgetsEverything() {
        val jar = newJar()
        jar.saveFromResponse(api, listOf(sessionCookie()))
        jar.clear()

        assertTrue(jar.loadForRequest(api).isEmpty())
        assertTrue(newJar().loadForRequest(api).isEmpty())
    }

    @Test fun corruptStoredDataMeansSignedOutNotCrash() {
        storeFile.writeText("not a protobuf")
        assertTrue(newJar().loadForRequest(api).isEmpty())
    }
}
