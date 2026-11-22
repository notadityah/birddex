package com.birddex.app.di

import com.birddex.app.data.gallery.GalleryRepository
import com.birddex.app.data.gallery.NetworkGalleryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class GalleryModule {
    @Binds
    abstract fun bindGalleryRepository(impl: NetworkGalleryRepository): GalleryRepository
}
