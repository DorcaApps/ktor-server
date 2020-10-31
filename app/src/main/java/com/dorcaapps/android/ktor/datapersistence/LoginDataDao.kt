package com.dorcaapps.android.ktor.datapersistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LoginDataDao {

    @Insert
    fun insertLoginData(loginData: LoginData): Long

    @Query("SELECT * FROM logindata WHERE username = :username")
    fun getLoginData(username: String): LoginData?

}