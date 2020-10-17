package com.dorcaapps.android.ktor

import android.content.Context
import com.dorcaapps.android.ktor.datapersistence.AppDatabase
import com.dorcaapps.android.ktor.datapersistence.FileData
import com.dorcaapps.android.ktor.datapersistence.OrderType
import com.dorcaapps.android.ktor.dto.MediaData
import io.ktor.http.*
import java.io.File
import java.time.OffsetDateTime

class FileHandler(private val applicationContext: Context) {
    private val database by lazy { AppDatabase.get(applicationContext) }

    suspend fun deleteFileDataWith(id: Int): Boolean {
        val fileData = database.fileDataDao().getFileDataWithId(id) ?: return false
        val didDeleteFile = File(applicationContext.filesDir, fileData.filename)
            .delete()
        return if (didDeleteFile) {
            database.fileDataDao().deleteFileData(fileData) == 1
        } else {
            false
        }
    }

    suspend fun getPagedMediaData(page: Int, pageSize: Int, orderType: OrderType): List<MediaData> {
        val offset = (page - 1) * pageSize
        return when (orderType) {
            OrderType.MOST_RECENT_LAST ->
                database.fileDataDao().getPagedMediaDataRecentLast(limit = pageSize, offset = offset)
            OrderType.MOST_RECENT_FIRST ->
                database.fileDataDao().getPagedMediaDataRecentFirst(limit = pageSize, offset = offset)
        }
    }

    suspend fun getFileData(id: Int): Pair<File, ContentType>? {
        val fileData = database.fileDataDao().getFileDataWithId(id) ?: return null
        return Pair(File(applicationContext.filesDir, fileData.filename), fileData.contentType)
    }

    suspend fun addFileData(
        filename: String,
        originalFilename: String,
        creationDate: OffsetDateTime,
        size: Long,
        contentType: ContentType
    ) {
        val fileData = FileData(
            filename,
            originalFilename,
            creationDate,
            size,
            contentType
        )
        database.fileDataDao().insertFileData(fileData)
    }
}