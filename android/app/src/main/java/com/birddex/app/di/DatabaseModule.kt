package com.birddex.app.di

import android.content.Context
import androidx.room.Room
import com.birddex.app.data.birds.BirdDao
import com.birddex.app.data.local.BirdDexDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BirdDexDatabase =
        Room.databaseBuilder(context, BirdDexDatabase::class.java, "birddex.db").build()

    @Provides
    fun provideBirdDao(db: BirdDexDatabase): BirdDao = db.birdDao()
}
