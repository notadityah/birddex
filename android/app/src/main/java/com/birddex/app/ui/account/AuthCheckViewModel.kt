package com.birddex.app.ui.account

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.BuildConfig
import com.birddex.app.data.network.AuthApi
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.PersistentCookieJar
import com.birddex.app.data.network.SignInEmailRequest
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
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Response

/**
 * A2.1 spike, debug builds only: proves cookie-based sign-in works against the real API.
 * Logs HTTP status codes and cookie names/expiry only, never cookie values, tokens or
 * response bodies. Replaced by the real AuthRepository and sign-in screens in A2.2–A2.3.
 */
@HiltViewModel
class AuthCheckViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val api: BirdDexApi,
    private val cookieJar: PersistentCookieJar,
) : ViewModel() {

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun signIn(email: String, password: String) = runStep("Sign in") {
        val response = authApi.signInEmail(SignInEmailRequest(email.trim(), password, rememberMe = true))
        log("sign-in → ${response.code()}${response.errorSummary()}")
        log(cookieSummary())
    }

    fun checkAndSignOut() = runStep("Check session, sightings, sign-out") {
        log(cookieSummary())
        val session = authApi.getSession()
        log("get-session → ${session.code()}, ${if (session.body() is JsonObject) "session present" else "no session"}")
        logSightings()

        val signedOut = signOut(origin = null)
        if (!signedOut) signOut(origin = BuildConfig.BASE_URL)
        log(cookieSummary())
        logSightings()
    }

    /** Returns true only for a real API success, not CloudFront's HTML error page. */
    private suspend fun signOut(origin: String?): Boolean {
        val response = authApi.signOut(origin = origin)
        val maskedError = response.headers()["x-cache"].orEmpty().contains("Error", ignoreCase = true)
        val label = if (origin == null) "without Origin" else "with Origin $origin"
        val detail = when {
            maskedError -> " but it's CloudFront's error page (${response.headers()["content-type"]}): the API returned 403/404"
            response.isSuccessful -> ": " + withContext(Dispatchers.IO) { response.body()?.string().orEmpty().take(200) }
            else -> response.errorSummary()
        }
        response.body()?.close()
        log("sign-out $label → ${response.code()}$detail")
        return response.isSuccessful && !maskedError
    }

    private suspend fun logSightings() {
        val response = api.getSightings()
        val count = (response.body() as? JsonArray)?.size
        log("GET /api/sightings → ${response.code()}${count?.let { " ($it sightings)" }.orEmpty()}")
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

    // better-auth error bodies are {"code": ..., "message": ...}; no credentials in them.
    private fun Response<*>.errorSummary(): String =
        if (isSuccessful) "" else ": " + (errorBody()?.string()?.take(200) ?: "(no body)")

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
