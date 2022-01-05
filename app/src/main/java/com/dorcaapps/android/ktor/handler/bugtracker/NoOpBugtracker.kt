package com.dorcaapps.android.ktor.handler.bugtracker

import android.content.Context

object NoOpBugtracker : Bugtracker {
    override fun init(context: Context) {

    }

    override fun trackThrowable(throwable: Throwable) {

    }
}