package com.birddex.app.di

import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.birds.OfflineFirstBirdRepository
import com.birddex.app.data.sightings.OfflineFirstSightingRepository
import com.birddex.app.data.sightings.SightingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BirdsModule {
    @Binds
    abstract fun bindBirdRepository(impl: OfflineFirstBirdRepository): BirdRepository

    @Binds
    abstract fun bindSightingRepository(impl: OfflineFirstSightingRepository): SightingRepository
}
