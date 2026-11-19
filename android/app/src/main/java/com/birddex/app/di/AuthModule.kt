package com.birddex.app.di

import com.birddex.app.BuildConfig
import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.NetworkAuthRepository
import com.birddex.app.data.auth.SessionExpiry
import com.birddex.app.data.network.AuthApi
import com.birddex.app.data.network.PersistentCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApi,
        cookieJar: PersistentCookieJar,
        json: Json,
        sessionExpiry: SessionExpiry,
    ): AuthRepository {
        val site = BuildConfig.BASE_URL.trimEnd('/')
        return NetworkAuthRepository(
            api = api,
            cookieJar = cookieJar,
            json = json,
            // The same website pages the web app uses (frontend/src/stores/auth.js).
            verifyCallbackUrl = "$site/login",
            resetPasswordUrl = "$site/reset-password",
            sessionExpiry = sessionExpiry,
        )
    }
}
