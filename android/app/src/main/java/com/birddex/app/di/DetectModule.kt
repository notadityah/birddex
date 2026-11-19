package com.birddex.app.di

import com.birddex.app.data.detect.BitmapPhotoPreparer
import com.birddex.app.data.detect.CacheDetectPhotoStore
import com.birddex.app.data.detect.DetectPhotoStore
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
}
