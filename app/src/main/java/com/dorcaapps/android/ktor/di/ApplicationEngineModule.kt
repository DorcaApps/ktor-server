package com.dorcaapps.android.ktor.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import io.ktor.server.engine.*
import io.ktor.server.jetty.*

@InstallIn(ApplicationComponent::class)
@Module
object ApplicationEngineModule {
    @Provides
    fun provideApplicationEngine(): ApplicationEngine = embeddedServer(Jetty, port = 8080) {}
}