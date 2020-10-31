package com.dorcaapps.android.ktor.extensions

import java.math.BigInteger
import java.security.MessageDigest

fun String.toMD5() = MessageDigest.getInstance("MD5").run {
    String.format(
        "%032x",
        BigInteger(
            1,
            digest(toByteArray())
        )
    )
}

fun String.toMD5ByteArray(): ByteArray = MessageDigest.getInstance("MD5").run {
    digest(toByteArray())
}