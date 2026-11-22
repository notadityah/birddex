package com.birddex.app.data.onboarding

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreWelcomeStoreTest {

    @get:Rule val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private lateinit var file: File

    @Before fun setUp() {
        file = tmp.newFolder().resolve("app.preferences_pb")
    }

    @After fun tearDown() = scopes.forEach { it.cancel() }

    /** A store on [file], like a fresh app launch. Closes the previous one (one store per file). */
    private suspend fun launchApp(): DataStoreWelcomeStore {
        // Wait until it's fully closed, or DataStore sees two stores on one file.
        scopes.forEach { it.coroutineContext.job.cancelAndJoin() }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return DataStoreWelcomeStore(DataStoreWelcomeStore.createStore(scope) { file }, scope)
    }

    @Test fun notSeenOnAFreshInstall() = runBlocking {
        assertFalse(launchApp().seen.first())
    }

    @Test fun closingItIsRememberedAfterARestart() = runBlocking {
        val store = launchApp()
        store.markSeen()
        withTimeout(5_000) { store.seen.first { it } } // written in the background

        assertTrue(launchApp().seen.first())
    }

    @Test fun aCorruptFileShowsItAgainInsteadOfCrashing() = runBlocking {
        file.writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x37, 0x42))

        assertFalse(launchApp().seen.first())
    }
}
