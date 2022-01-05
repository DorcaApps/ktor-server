package com.dorcaapps.android.ktor.handler.bugtracker

import android.content.Context

interface Bugtracker {
    fun init(context: Context)
    fun trackThrowable(throwable: Throwable)
}