package com.dorcaapps.android.ktor.datapersistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LoginDataDao {

    @Insert
    suspend fun insertLoginData(loginData: LoginData): Long

    @Query("SELECT * FROM logindata WHERE username = :username")
    suspend fun getLoginData(username: String): LoginData?

}