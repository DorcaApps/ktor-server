package com.dorcaapps.android.ktor.dto

import io.ktor.auth.*
import kotlinx.serialization.Serializable
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

@Serializable
data class SessionCookie(val id: String): Principal {
    companion object {
        const val name = "Session-Cookie"

        fun create() = SessionCookie(
            String.format(
                "%032x",
                BigInteger(
                    1,
                    MessageDigest.getInstance("SHA-512").digest(
                        UUID.randomUUID().toString().toByteArray()
                    )
                )
            )
        )
    }
}