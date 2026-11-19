package com.birddex.app.data.network

import android.util.Log
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the HTTP stack. NetworkModule uses these for the app and the unit tests use them
 * against a fake server, so tests exercise the same interceptors the app runs.
 */
object ApiClients {

    val json: Json = Json {
        ignoreUnknownKeys = true // the API can add fields without breaking the app
        explicitNulls = false
    }

    fun okHttpClient(
        baseUrl: String,
        cookieJar: CookieJar,
        userAgent: String,
        debugLogging: Boolean,
    ): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
        }
        .addInterceptor(AuthOriginInterceptor(originOf(baseUrl)))
        .addInterceptor(HtmlErrorPageInterceptor())
        // Lambda cold starts can take several seconds (detect is slowest).
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        // Added last so it logs exactly what goes on the wire (including Origin).
        .apply { if (debugLogging) addInterceptor(debugHttpLogger()) }
        .build()

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** `https://birddex.fun/anything` → `https://birddex.fun` (port kept if non-default). */
    fun originOf(baseUrl: String): String {
        val url = baseUrl.toHttpUrl()
        val defaultPort = if (url.scheme == "https") 443 else 80
        return "${url.scheme}://${url.host}" + if (url.port != defaultPort) ":${url.port}" else ""
    }

    /**
     * Debug-only HTTP log (logcat tag BirdDexHttp). Never prints credentials: cookie headers
     * are redacted everywhere, and /api/auth/ calls log headers only, because their bodies
     * carry passwords (requests) and session tokens (responses).
     */
    private fun debugHttpLogger(): Interceptor {
        fun logger(level: HttpLoggingInterceptor.Level) =
            HttpLoggingInterceptor { Log.d("BirdDexHttp", it) }.apply {
                this.level = level
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
            }
        val withBodies = logger(HttpLoggingInterceptor.Level.BODY)
        val headersOnly = logger(HttpLoggingInterceptor.Level.HEADERS)
        return Interceptor { chain ->
            val isAuthCall = chain.request().url.encodedPath.startsWith("/api/auth/")
            (if (isAuthCall) headersOnly else withBodies).intercept(chain)
        }
    }
}
