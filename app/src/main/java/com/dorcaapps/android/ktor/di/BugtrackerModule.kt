package com.dorcaapps.android.ktor.di

import com.dorcaapps.android.ktor.handler.bugtracker.Bugtracker
import com.dorcaapps.android.ktor.handler.bugtracker.NoOpBugtracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BugtrackerModule {
    @Provides
    @Singleton
    fun provideBugtracker(): Bugtracker = NoOpBugtracker
}