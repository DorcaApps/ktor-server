package com.dorcaapps.android.ktor

import android.app.Application
import com.dorcaapps.android.ktor.handler.bugtracker.Bugtracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KtorApplication : Application() {
    @Inject lateinit var bugtracker: Bugtracker

    override fun onCreate() {
        super.onCreate()
        bugtracker.init(this)
    }
}