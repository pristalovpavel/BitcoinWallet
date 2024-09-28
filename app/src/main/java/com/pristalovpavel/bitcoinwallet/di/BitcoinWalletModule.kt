package com.pristalovpavel.bitcoinwallet.di

import android.content.Context
import com.pristalovpavel.bitcoinwallet.api.BitcoinApi
import com.pristalovpavel.bitcoinwallet.repository.BitcoinRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BitcoinModule {

    @Provides
    @Singleton
    fun provideBitcoinApi(): BitcoinApi = BitcoinApi.create()

    @Provides
    @Singleton
    fun provideBitcoinRepository(
        api: BitcoinApi,
        @ApplicationContext context: Context
    ): BitcoinRepository = BitcoinRepository(api, context)
}