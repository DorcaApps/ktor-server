package com.dorcaapps.android.ktor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.security.crypto.EncryptedFile
import com.dorcaapps.android.ktor.datapersistence.OrderType
import com.dorcaapps.android.ktor.dto.SessionCookie
import com.dorcaapps.android.ktor.extensions.asEncryptedFile
import com.dorcaapps.android.ktor.extensions.putDecryptedContentsIntoOutputStream
import com.dorcaapps.android.ktor.extensions.receiveMultipartOrNull
import com.dorcaapps.android.ktor.extensions.writeEncrypted
import com.dorcaapps.android.ktor.handler.AuthenticationHandler
import com.dorcaapps.android.ktor.handler.FileHandler
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.sessions.*
import io.ktor.util.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class KtorServer(private val appContext: Context) {

    private val serverEngine by lazy { createServerEngine() }

    private val fileHandler = FileHandler(appContext)
    private val authenticationHandler = AuthenticationHandler()

    fun start() {
        serverEngine.start(false)
    }

    fun stop(gracePeriodMillis: Long, timeOutMillis: Long) {
        serverEngine.stop(gracePeriodMillis, timeOutMillis)
    }

    private fun createServerEngine(): JettyApplicationEngine = embeddedServer(Jetty, port = 8080) {
        installFeatures(this)
        routing { installRoutes(this) }
    }

    private fun installFeatures(application: Application): Unit = application.run {
        install(ContentNegotiation) {
            json()
        }
        install(DefaultHeaders)
        install(CallLogging) {
            logger = LoggerFactory.getLogger("Application.Test")
            this.format {
                "\nRequest:\n" +
                        it.request.headers.flattenEntries().joinToString("\n") +
                        "\nResponse:\n" +
                        it.response.headers.allValues().flattenEntries().joinToString("\n")
            }
        }
        install(Sessions) {
            Log.e("MTest", "sessions")
            cookie<SessionCookie>("Session-Cookie")
        }
        install(Authentication, authenticationHandler.authenticationConfig)
    }

    private fun installRoutes(routing: Routing): Unit = routing.run {
        get("/") {
            call.respondText(
                "Hello World",
                ContentType.Text.Plain
            )
        }
        authenticate(Constants.Authentication.DIGEST) {
            get("/login") {
                call.sessions.set(authenticationHandler.getNewSessionCookie())
                call.respond(HttpStatusCode.OK)
            }
        }

        authenticate(Constants.Authentication.SESSION) {
            route("/media") {
                route("/{id}") {
                    route("/thumbnail") { installMediaIdThumbnailRoutes(this) }
                    installMediaIdRoutes(this)
                }
                installMediaRoutes(this)
            }
        }
    }

    private fun installMediaIdThumbnailRoutes(route: Route): Unit = route.run {
        get {
            val id = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val allFileData = fileHandler.getAllFileData()
            Log.e("MTest", allFileData.count().toString())
            allFileData.forEach {
                Log.e("MTest", it.toString())
            }
            val fileData = fileHandler.getFileData(id) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val contentType = fileData.contentType

            val thumbnailFile = when (contentType.contentType) {
                ContentType.Video.Any.contentType, ContentType.Image.Any.contentType -> {
                    File(appContext.filesDir, fileData.thumbnailFilename)
                }
                else -> {
                    call.respond(HttpStatusCode.InternalServerError)
                    return@get
                }
            }
            call.respondOutputStream(contentType = ContentType.Image.PNG) {
                thumbnailFile.putDecryptedContentsIntoOutputStream(appContext, this)
            }
        }
    }

    private fun installMediaIdRoutes(route: Route): Unit = route.run {
        delete {
            val id = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }
            val didDelete = fileHandler.deleteFileDataWith(id)
            if (didDelete) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        get {
            val id = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val fileData = fileHandler.getFileData(id) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val file = File(appContext.filesDir, fileData.filename)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    fileData.originalFilename
                ).toString()
            )
            call.respondOutputStream(contentType = fileData.contentType) {
                file.putDecryptedContentsIntoOutputStream(appContext, this)
            }
        }
    }

    private fun installMediaRoutes(route: Route): Unit = route.run {
        get("/") {
            val pageSize = call.parameters["pageSize"]?.toIntOrNull()?.takeIf { it >= 1 } ?: 10
            val page = call.parameters["page"]?.toIntOrNull()?.takeIf { it >= 1 } ?: 1
            val orderType = OrderType.getWithDefault(call.parameters["order"]) ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val result = fileHandler.getPagedMediaData(
                page = page,
                pageSize = pageSize,
                orderType = orderType
            )
            call.respond(result)
        }

        post("/") {
            val multipart = call.receiveMultipartOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val parts = multipart.readAllParts()
            val filePart = parts.filterIsInstance<PartData.FileItem>().singleOrNull() ?: run {
                parts.forEach { it.dispose() }
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val creationDate = OffsetDateTime.now()
            val originalMediaFilename = filePart.originalFileName ?: "filename"
            val mediaFilename = "$creationDate#$originalMediaFilename"
            val thumbnailFilename = "$mediaFilename#thumbnail.png"
            val mediaFile = File(appContext.filesDir, mediaFilename)
            val thumbnailFile = File(appContext.filesDir, thumbnailFilename)

            val contentType = filePart.contentType
            when (contentType?.contentType) {
                ContentType.Image.Any.contentType -> {
                    saveImageAndItsThumbnail(filePart, mediaFile, thumbnailFile)
                }
                ContentType.Video.Any.contentType -> {
                    saveVideoAndItsThumbnail(filePart, mediaFile, thumbnailFile)
                }
                else -> {
                    parts.forEach { it.dispose() }
                    call.respond(HttpStatusCode.UnsupportedMediaType)
                    return@post
                }
            }
            fileHandler.addFileData(
                mediaFilename,
                originalMediaFilename,
                thumbnailFilename,
                creationDate,
                mediaFile.length(),
                contentType
            )
            parts.forEach { it.dispose() }
            call.respond(TestEntity(1, "ayaya"))
        }
    }

    private suspend fun saveImageAndItsThumbnail(
        filePart: PartData.FileItem,
        imageFile: File,
        thumbnailFile: File
    ) {
        val encryptedImageFile = imageFile.asEncryptedFile(appContext)
        encryptedImageFile.openFileOutput().use { outputStream ->
            filePart.streamProvider().use { inputStream ->
                inputStream.copyToSuspend(outputStream)
            }
        }
        val thumbnail =
            decodeSampledBitmapFromFile(encryptedImageFile, 100, 100)
        val stream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream)
        thumbnailFile.writeEncrypted(appContext, stream.toByteArray())
    }

    private suspend fun saveVideoAndItsThumbnail(
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
                    appContext,
                    stream.toByteArray()
                )
            }
            launch {
                videoFile.writeEncrypted(appContext, bytes)
            }

        }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }


    fun decodeSampledBitmapFromFile(
        encryptedFile: EncryptedFile,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            encryptedFile.openFileInput().use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, this)
            }


            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false

            encryptedFile.openFileInput().use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, this)
            }!!
        }
    }

}

suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}