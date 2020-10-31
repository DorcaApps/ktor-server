package com.dorcaapps.android.ktor.mapper

import com.dorcaapps.android.ktor.datapersistence.LoginData
import com.dorcaapps.android.ktor.dto.LoginDataDTO
import java.math.BigInteger
import java.security.MessageDigest

fun LoginDataDTO.toDomainModel(realm: String) = LoginData(
    username = username,
    passwordHash = MessageDigest.getInstance("MD5").run {
        String.format(
            "%032x",
            BigInteger(
                1,
                digest("$username:$realm:$password".toByteArray())
            )
        )
    }
)