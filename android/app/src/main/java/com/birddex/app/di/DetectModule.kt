package com.birddex.app.di

import com.birddex.app.data.detect.BitmapPhotoPreparer
import com.birddex.app.data.detect.CacheDetectPhotoStore
import com.birddex.app.data.detect.DetectPhotoStore
import com.birddex.app.data.detect.DetectRepository
import com.birddex.app.data.detect.NetworkDetectRepository
import com.birddex.app.data.detect.PhotoPreparer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DetectModule {
    @Binds
    abstract fun bindDetectPhotoStore(impl: CacheDetectPhotoStore): DetectPhotoStore

    @Binds
    abstract fun bindPhotoPreparer(impl: BitmapPhotoPreparer): PhotoPreparer

    @Binds
    abstract fun bindDetectRepository(impl: NetworkDetectRepository): DetectRepository
}
