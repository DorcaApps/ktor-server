package com.dorcaapps.android.ktor.datapersistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class LoginData(
    @PrimaryKey val username: String,
    val password: String
): Serializable