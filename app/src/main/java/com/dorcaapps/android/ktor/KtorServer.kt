package com.dorcaapps.android.ktor

import android.content.Context
import android.util.Log
import com.dorcaapps.android.ktor.datapersistence.OrderType
import com.dorcaapps.android.ktor.dto.LoginDataDTO
import com.dorcaapps.android.ktor.dto.SessionCookie
import com.dorcaapps.android.ktor.extensions.putDecryptedContentsIntoOutputStream
import com.dorcaapps.android.ktor.extensions.receiveMultipartOrNull
import com.dorcaapps.android.ktor.handler.AuthenticationHandler
import com.dorcaapps.android.ktor.handler.FileHandler
import com.dorcaapps.android.ktor.mapper.toDomainModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.sessions.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import java.io.File
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorServer @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val notificationHelper: NotificationHelper,
    private val fileHandler: FileHandler,
    private val authenticationHandler: AuthenticationHandler,
    private val serverEngine: ApplicationEngine
) {

    fun start() {
        serverEngine.start(false)
        setupApplicationEngine()
    }

    fun stop(gracePeriodMillis: Long, timeOutMillis: Long) {
        serverEngine.stop(gracePeriodMillis, timeOutMillis)
    }

    private fun setupApplicationEngine() {
        serverEngine.application.apply {
            installFeatures(this)
            routing { installRoutes(this) }
        }
    }

    private fun installFeatures(application: Application): Unit = application.run {
        install(DefaultHeaders)
        install(Authentication, authenticationHandler.authenticationConfig)
        install(ContentNegotiation) {
            json()
        }
        install(Sessions) {
            cookie<SessionCookie>(SessionCookie.name)
        }
        install(CallLogging) {
            logger = LoggerFactory.getLogger("Application.Test")
            format {
                "\nRequest: ${it.request.toLogString()}\n    " +
                        it.request.headers.flattenEntries().joinToString("\n    ") +
                        "\nResponse: ${it.response.status()}\n    " +
                        it.response.headers.allValues().flattenEntries().joinToString("\n    ")
            }
        }
    }

    private fun installRoutes(routing: Routing): Unit = routing.run {
        get("/") {
            call.respondText(
                "Hello World",
                ContentType.Text.Plain
            )
        }

        post("/register") {
            val loginData =
                call.receiveOrNull<LoginDataDTO>()?.toDomainModel("login") ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
            notificationHelper.showRegisterNotification(loginData)
            call.respond(HttpStatusCode.OK)
        }

        authenticate(Constants.Authentication.LOGIN) {
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
                    File(appContext.filesDir, fileData.thumbnailFilename).takeIf { it.exists() }
                }
                else -> null
            } ?: run {
                call.respond(HttpStatusCode.InternalServerError)
                return@get
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
            val file = File(appContext.filesDir, fileData.filename).takeIf { it.exists() }
                ?: run {
                    call.respond(HttpStatusCode.InternalServerError)
                    return@get
                }
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
                    fileHandler.saveImageAndItsThumbnail(filePart, mediaFile, thumbnailFile)
                }
                ContentType.Video.Any.contentType -> {
                    fileHandler.saveVideoAndItsThumbnail(filePart, mediaFile, thumbnailFile)
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
            call.respond(HttpStatusCode.OK)
        }
    }
}