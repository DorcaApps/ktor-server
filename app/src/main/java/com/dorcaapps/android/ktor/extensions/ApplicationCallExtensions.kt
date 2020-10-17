package com.dorcaapps.android.ktor.extensions

import io.ktor.application.*
import io.ktor.request.*

suspend fun ApplicationCall.receiveMultipartOrNull() = try {
    receiveMultipart()
} catch (e: Exception) {
    null
}