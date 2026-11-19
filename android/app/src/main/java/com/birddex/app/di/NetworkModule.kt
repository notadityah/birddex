package com.birddex.app.di

import android.content.Context
import com.birddex.app.BuildConfig
import com.birddex.app.data.network.ApiClients
import com.birddex.app.data.network.AuthApi
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.PersistentCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Cookie file name; excluded from cloud backup in res/xml so sessions don't move devices. */
    const val COOKIE_STORE_FILE = "datastore/cookies.preferences_pb"

    @Provides
    @Singleton
    fun provideJson(): Json = ApiClients.json

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
        ApiClients.okHttpClient(
            baseUrl = BuildConfig.BASE_URL,
            cookieJar = cookieJar,
            userAgent = "BirdDex-Android/${BuildConfig.VERSION_NAME}",
            debugLogging = BuildConfig.DEBUG,
        )

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = ApiClients.retrofit(BuildConfig.BASE_URL, client)

    @Provides
    @Singleton
    fun provideBirdDexApi(retrofit: Retrofit): BirdDexApi = retrofit.create()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()
}
