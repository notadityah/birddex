package com.birddex.app.di

import com.birddex.app.data.feedback.FeedbackRepository
import com.birddex.app.data.feedback.NetworkFeedbackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackModule {
    @Binds
    abstract fun bindFeedbackRepository(impl: NetworkFeedbackRepository): FeedbackRepository
}
