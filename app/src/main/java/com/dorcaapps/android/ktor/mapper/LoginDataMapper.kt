package com.dorcaapps.android.ktor.mapper

import com.dorcaapps.android.ktor.datapersistence.LoginData
import com.dorcaapps.android.ktor.dto.LoginDataDTO

fun LoginDataDTO.toDomainModel() = LoginData(
    username = username,
    password = password
)