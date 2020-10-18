package com.dorcaapps.android.ktor.datapersistence

import androidx.room.*
import com.dorcaapps.android.ktor.dto.MediaData

@Dao
interface FileDataDao {

    @Query("SELECT id, contentType FROM filedata ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedMediaDataRecentFirst(limit: Int, offset: Int): List<MediaData>

    @Query("SELECT id, contentType FROM filedata ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getPagedMediaDataRecentLast(limit: Int, offset: Int): List<MediaData>

    @Query("SELECT * FROM filedata where id = :id")
    suspend fun getFileDataWithId(id: Int): FileData?

    @Query("SELECT * FROM filedata")
    suspend fun getAllFileData(): List<FileData>

    @Delete
    suspend fun deleteFileData(fileData: FileData): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFileData(fileData: FileData)

}