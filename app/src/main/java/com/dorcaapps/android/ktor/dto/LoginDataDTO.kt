package com.dorcaapps.android.ktor.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginDataDTO(
    val username: String,
    val password: String
)