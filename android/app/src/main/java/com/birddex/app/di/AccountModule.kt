package com.birddex.app.di

import com.birddex.app.data.account.AccountRepository
import com.birddex.app.data.account.NetworkAccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {
    @Binds
    abstract fun bindAccountRepository(impl: NetworkAccountRepository): AccountRepository
}
