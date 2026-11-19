package com.birddex.app.di

import android.content.Context
import android.util.Log
import com.birddex.app.BuildConfig
import com.birddex.app.data.network.AuthApi
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.PersistentCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Cookie file name; excluded from cloud backup in res/xml so sessions don't move devices. */
    const val COOKIE_STORE_FILE = "datastore/cookies.preferences_pb"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true // the API can add fields without breaking the app
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideCookieJar(@ApplicationContext context: Context): PersistentCookieJar {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PersistentCookieJar.createStore(scope) { File(context.filesDir, COOKIE_STORE_FILE) }
        return PersistentCookieJar(store)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(cookieJar: PersistentCookieJar): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "BirdDex-Android/${BuildConfig.VERSION_NAME}")
                        .build(),
                )
            }
            // Lambda cold starts can take several seconds (detect is slowest).
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .apply { if (BuildConfig.DEBUG) addInterceptor(debugHttpLogger()) }
            .build()

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

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideBirdDexApi(retrofit: Retrofit): BirdDexApi = retrofit.create()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()
}
