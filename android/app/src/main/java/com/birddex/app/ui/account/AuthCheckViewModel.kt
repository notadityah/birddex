package com.birddex.app.ui.account

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.BuildConfig
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.AuthResult
import com.birddex.app.data.auth.AuthUser
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.PersistentCookieJar
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Debug-only developer panel (A2.1 spike, now driving AuthRepository from A2.2). Logs
 * results, cookie names and expiry only, never cookie values, tokens, emails or bodies.
 * Removed when the real sign-in screens land (A2.3).
 */
@HiltViewModel
class AuthCheckViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val api: BirdDexApi,
    private val cookieJar: PersistentCookieJar,
) : ViewModel() {

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun signIn(email: String, password: String) = runStep("Sign in") {
        log("sign-in → ${auth.signIn(email, password).describe()}")
        log("state → ${auth.sessionState.value::class.simpleName}")
        log(cookieSummary())
    }

    fun checkAndSignOut() = runStep("Check session, sightings, sign-out") {
        log(cookieSummary())
        log("session → ${auth.refreshSession().describe()}")
        logSightings()
        auth.signOut()
        log("signed out → state ${auth.sessionState.value::class.simpleName}")
        log(cookieSummary())
        logSightings()
    }

    private fun AuthResult<AuthUser?>.describe(): String = when (this) {
        is AuthResult.Success -> value?.let { "OK, signed in (verified=${it.emailVerified})" } ?: "OK, signed out"
        is AuthResult.Failure -> "failed: $error"
    }

    private suspend fun logSightings() {
        val text = try {
            val response = api.getSightings()
            val count = (response.body() as? JsonArray)?.size
            "${response.code()}${count?.let { " ($it sightings)" }.orEmpty()}"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "error ${e::class.simpleName}: ${e.message}"
        }
        log("GET /api/sightings → $text")
    }

    private suspend fun cookieSummary(): String = withContext(Dispatchers.IO) {
        val cookies = cookieJar.loadForRequest(BuildConfig.BASE_URL.toHttpUrl())
        if (cookies.isEmpty()) {
            "cookies stored: none"
        } else {
            "cookies stored: " + cookies.joinToString { c ->
                val life = if (c.persistent) "expires ${Instant.ofEpochMilli(c.expiresAt)}" else "session-only"
                "${c.name} ($life)"
            }
        }
    }

    private fun runStep(title: String, block: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            log("── $title")
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("error: ${e::class.simpleName}: ${e.message}")
            } finally {
                _busy.value = false
            }
        }
    }

    private fun log(line: String) {
        Log.i("AuthCheck", line)
        _lines.update { it + line }
    }
}
