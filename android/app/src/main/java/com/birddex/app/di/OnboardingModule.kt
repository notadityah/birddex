package com.birddex.app.di

import android.content.Context
import com.birddex.app.data.onboarding.DataStoreWelcomeStore
import com.birddex.app.data.onboarding.WelcomeStore
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

@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {

    /**
     * App preferences (only "welcome seen" so far). Separate from the cookie file so it can be
     * backed up: restoring onto a new phone doesn't show the welcome again.
     */
    const val APP_PREFS_FILE = "datastore/app.preferences_pb"

    @Provides
    @Singleton
    fun provideWelcomeStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): WelcomeStore {
        val io = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = DataStoreWelcomeStore.createStore(io) { File(context.filesDir, APP_PREFS_FILE) }
        return DataStoreWelcomeStore(store, scope)
    }
}
