package com.dorcaapps.android.ktor.datapersistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileDataDao {

    @Query("SELECT * FROM filedata where id = :id")
    suspend fun getFileDataWithId(id: Int): FileData?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFileData(fileData: FileData)

}