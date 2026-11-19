package com.birddex.app.data.network

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * OkHttp cookie jar that survives app restarts. This is how the app stays signed in:
 * better-auth's session cookie is saved here on sign-in and sent on every API call.
 *
 * Cookies live in memory and are written through to [dataStore] on every change.
 * Persistent cookies (with Max-Age/Expires) are saved; session-only cookies stay in
 * memory. OkHttp calls this on its own background threads, so the blocking DataStore
 * reads/writes here never run on the main thread. Call [clear] off the main thread too.
 */
class PersistentCookieJar(
    private val dataStore: DataStore<Preferences>,
    private val now: () -> Long = System::currentTimeMillis,
) : CookieJar {

    private val lock = Any()
    private var cookies: MutableList<Cookie>? = null // loaded from disk on first use

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            val current = loaded()
            val time = now()
            for (cookie in cookies) {
                current.removeAll { it.sameIdentity(cookie) }
                // A cookie that arrives already expired is the server deleting it.
                if (cookie.expiresAt > time) current += cookie
            }
            write(current)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        val current = loaded()
        if (current.removeAll { it.expiresAt <= now() }) write(current)
        current.filter { it.matches(url) }
    }

    /** Forgets every cookie, e.g. on sign-out. */
    fun clear() {
        synchronized(lock) {
            cookies = mutableListOf()
            write(emptyList())
        }
    }

    private fun loaded(): MutableList<Cookie> =
        cookies ?: runBlocking { read() }.toMutableList().also { cookies = it }

    // Unreadable data means "signed out", never a crash.
    private suspend fun read(): List<Cookie> {
        val stored = runCatching { dataStore.data.first()[KEY] }.getOrNull() ?: return emptyList()
        val time = now()
        return runCatching { json.decodeFromString<List<StoredCookie>>(stored) }
            .getOrDefault(emptyList())
            .map { it.toCookie() }
            .filter { it.expiresAt > time }
    }

    // A failed disk write keeps the cookies in memory for this run instead of failing the request.
    private fun write(cookies: List<Cookie>) {
        val stored = json.encodeToString(cookies.filter { it.persistent }.map(StoredCookie::from))
        runCatching { runBlocking { dataStore.edit { it[KEY] = stored } } }
            .onFailure { Log.w(TAG, "Couldn't save cookies to disk", it) }
    }

    private fun Cookie.sameIdentity(other: Cookie) =
        name == other.name && domain == other.domain && path == other.path

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean,
    ) {
        fun toCookie(): Cookie = Cookie.Builder()
            .name(name)
            .value(value)
            .expiresAt(expiresAt)
            .apply { if (hostOnly) hostOnlyDomain(domain) else domain(domain) }
            .path(path)
            .apply { if (secure) secure() }
            .apply { if (httpOnly) httpOnly() }
            .build()

        companion object {
            fun from(c: Cookie) = StoredCookie(
                c.name, c.value, c.expiresAt, c.domain, c.path, c.secure, c.httpOnly, c.hostOnly,
            )
        }
    }

    companion object {
        private const val TAG = "PersistentCookieJar"
        private val KEY = stringPreferencesKey("cookies")
        private val json = Json { ignoreUnknownKeys = true }

        /** The DataStore this jar expects: a corrupt file is replaced with an empty one. */
        fun createStore(scope: CoroutineScope, file: () -> File): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = file,
            )
    }
}
