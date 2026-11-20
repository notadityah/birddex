package com.birddex.app.data.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Whether this phone has shown the first-run welcome (website: useOnboarding.js, which keeps it
 * in localStorage). Per device rather than per account, like the website.
 */
interface WelcomeStore {
    /** Read from disk; false until the welcome has been closed once (or if the file can't be read). */
    val seen: Flow<Boolean>

    /** Records that it was closed. Fire and forget, so leaving the app right after still saves it. */
    fun markSeen()
}

class DataStoreWelcomeStore(
    private val dataStore: DataStore<Preferences>,
    /** Outlives the screen that called [markSeen]. */
    private val scope: CoroutineScope,
) : WelcomeStore {

    override val seen: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY] == true }
        .distinctUntilChanged()

    override fun markSeen() {
        scope.launch {
            // If it can't be written, the welcome just shows again next launch.
            try {
                dataStore.edit { it[KEY] = true }
            } catch (_: IOException) {
            }
        }
    }

    companion object {
        /** Change the suffix to show the welcome to everyone again after a real content change. */
        private val KEY = booleanPreferencesKey("welcome_seen_v1")

        /** A corrupt file is replaced with an empty one, which shows the welcome again. */
        fun createStore(scope: CoroutineScope, file: () -> File): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = file,
            )
    }
}
