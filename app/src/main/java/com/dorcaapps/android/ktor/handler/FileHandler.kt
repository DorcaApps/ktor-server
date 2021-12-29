package com.dorcaapps.android.ktor.handler

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import com.dorcaapps.android.ktor.datapersistence.AppDatabase
import com.dorcaapps.android.ktor.datapersistence.FileData
import com.dorcaapps.android.ktor.datapersistence.LoginData
import com.dorcaapps.android.ktor.datapersistence.OrderType
import com.dorcaapps.android.ktor.dto.MediaData
import com.dorcaapps.android.ktor.extensions.asEncryptedFile
import com.dorcaapps.android.ktor.extensions.decodeSampledBitmap
import com.dorcaapps.android.ktor.extensions.getScaledBitmapWithOriginalAspectRatio
import com.dorcaapps.android.ktor.extensions.writeEncrypted
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    suspend fun hasAccount(username: String) =
        database.loginDataDao().getLoginData(username) != null

    suspend fun saveLoginData(loginData: LoginData) =
        database.loginDataDao().insertLoginData(loginData) > 0

    suspend fun getLoginData(username: String) = database.loginDataDao().getLoginData(username)

    suspend fun deleteFileDataWith(id: Int): Boolean {
        val fileData = database.fileDataDao().getFileDataWithId(id) ?: return false
        val didDeleteFile = File(context.filesDir, fileData.filename)
            .delete()
        val didDeleteThumbnail = File(context.filesDir, fileData.thumbnailFilename)
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
                database.fileDataDao()
                    .getPagedMediaDataRecentLast(limit = pageSize, offset = offset)
            OrderType.MOST_RECENT_FIRST ->
                database.fileDataDao()
                    .getPagedMediaDataRecentFirst(limit = pageSize, offset = offset)
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

    fun saveImageAndItsThumbnail(
        byteArray: ByteArray,
        imageFile: File,
        thumbnailFile: File
    ) {
        val encryptedImageFile = imageFile.asEncryptedFile(context)
        encryptedImageFile.openFileOutput().use { outputStream ->
            outputStream.write(byteArray)
        }
        val thumbnail = encryptedImageFile.decodeSampledBitmap(100, 100)
        val stream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream)
        thumbnailFile.writeEncrypted(context, stream.toByteArray())
    }

    suspend fun saveVideoAndItsThumbnail(
        bytes: ByteArray,
        videoFile: File,
        thumbnailFile: File
    ): Unit =
        coroutineScope {
            val tempFile = File.createTempFile("prefix", "suffix")
            launch {
                tempFile.writeBytes(bytes)
                val mediaRetriever = MediaMetadataRetriever().apply { setDataSource(tempFile.path) }
                val thumbnailBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    mediaRetriever.getScaledFrameAtTime(
                        TimeUnit.SECONDS.toMicros(1),
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        100,
                        100
                    )
                } else {
                    mediaRetriever.getFrameAtTime(
                        TimeUnit.SECONDS.toMicros(1),
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ).getScaledBitmapWithOriginalAspectRatio()
                }
                mediaRetriever.release()

                val stream = ByteArrayOutputStream()
                thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                thumbnailFile.writeEncrypted(
                    context,
                    stream.toByteArray()
                )
            }.invokeOnCompletion {
                tempFile.delete()
            }
            launch {
                videoFile.writeEncrypted(context, bytes)
            }

        }
}