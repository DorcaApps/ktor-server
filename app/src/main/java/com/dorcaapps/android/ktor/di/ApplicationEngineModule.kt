package com.dorcaapps.android.ktor.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.server.engine.*
import io.ktor.server.netty.*

@InstallIn(SingletonComponent::class)
@Module
object ApplicationEngineModule {
    @Provides
    fun provideApplicationEngine(): ApplicationEngine = embeddedServer(Netty, port = 8080) {}
}