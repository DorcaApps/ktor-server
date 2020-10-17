package com.dorcaapps.android.ktor

import android.content.Context
import com.dorcaapps.android.ktor.datapersistence.AppDatabase
import com.dorcaapps.android.ktor.datapersistence.FileData
import io.ktor.http.*
import java.io.File
import java.time.OffsetDateTime

class FileHandler(private val applicationContext: Context) {
    private val database by lazy { AppDatabase.get(applicationContext) }

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