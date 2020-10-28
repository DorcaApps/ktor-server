package com.dorcaapps.android.ktor.handler

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.dorcaapps.android.ktor.datapersistence.AppDatabase
import com.dorcaapps.android.ktor.datapersistence.FileData
import com.dorcaapps.android.ktor.datapersistence.OrderType
import com.dorcaapps.android.ktor.dto.MediaData
import com.dorcaapps.android.ktor.extensions.asEncryptedFile
import com.dorcaapps.android.ktor.extensions.copyToSuspend
import com.dorcaapps.android.ktor.extensions.decodeSampledBitmap
import com.dorcaapps.android.ktor.extensions.writeEncrypted
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class FileHandler(private val applicationContext: Context) {
    private val database by lazy { AppDatabase.get(applicationContext) }

    suspend fun deleteFileDataWith(id: Int): Boolean {
        val fileData = database.fileDataDao().getFileDataWithId(id) ?: return false
        val didDeleteFile = File(applicationContext.filesDir, fileData.filename)
            .delete()
        val didDeleteThumbnail = File(applicationContext.filesDir, fileData.thumbnailFilename)
            .delete()
        return if (didDeleteFile && didDeleteThumbnail) {
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

    suspend fun getFileData(id: Int): FileData? {
        return database.fileDataDao().getFileDataWithId(id)
    }

    suspend fun getAllFileData(): List<FileData> = database.fileDataDao().getAllFileData()

    suspend fun addFileData(
        filename: String,
        originalFilename: String,
        thumbnailFilename: String,
        creationDate: OffsetDateTime,
        size: Long,
        contentType: ContentType
    ) {
        val fileData = FileData(
            filename,
            originalFilename,
            thumbnailFilename,
            creationDate,
            size,
            contentType
        )
        database.fileDataDao().insertFileData(fileData)
    }

    suspend fun saveImageAndItsThumbnail(
        filePart: PartData.FileItem,
        imageFile: File,
        thumbnailFile: File
    ) {
        val encryptedImageFile = imageFile.asEncryptedFile(applicationContext)
        encryptedImageFile.openFileOutput().use { outputStream ->
            filePart.streamProvider().use { inputStream ->
                inputStream.copyToSuspend(outputStream)
            }
        }
        val thumbnail = encryptedImageFile.decodeSampledBitmap(100, 100)
        val stream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream)
        thumbnailFile.writeEncrypted(applicationContext, stream.toByteArray())
    }

    suspend fun saveVideoAndItsThumbnail(
        filePart: PartData.FileItem,
        videoFile: File,
        thumbnailFile: File
    ): Unit =
        coroutineScope {
            val bytes = filePart.streamProvider().readBytes()
            launch {
                val tempFile = File.createTempFile("prefix", "suffix")
                tempFile.outputStream().use { outputStream ->
                    filePart.streamProvider().use { inputStream ->
                        inputStream.copyToSuspend(outputStream)
                    }
                    outputStream.write(bytes)
                }
                val mediaRetriever =
                    MediaMetadataRetriever().apply { setDataSource(tempFile.path) }
                val thumbnailBitmap = mediaRetriever.getScaledFrameAtTime(
                    TimeUnit.SECONDS.toMicros(1),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    100,
                    100
                )
                val stream = ByteArrayOutputStream()
                thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                mediaRetriever.close()
                tempFile.delete()

                thumbnailFile.writeEncrypted(
                    applicationContext,
                    stream.toByteArray()
                )
            }
            launch {
                videoFile.writeEncrypted(applicationContext, bytes)
            }

        }
}