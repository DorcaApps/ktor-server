package com.dorcaapps.android.ktor

import android.content.Context
import com.dorcaapps.android.ktor.datapersistence.OrderType
import com.dorcaapps.android.ktor.extensions.putDecryptedContentsIntoOutputStream
import com.dorcaapps.android.ktor.extensions.receiveMultipartOrNull
import com.dorcaapps.android.ktor.extensions.writeEncrypted
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.OffsetDateTime

class KtorServer(private val appContext: Context) {

    private val serverEngine by lazy { createServerEngine() }

    private val fileHandler = FileHandler(appContext)

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
    }

    private fun installRoutes(routing: Routing): Unit = routing.run {
        get("/") {
            call.respondText(
                "Hello World",
                ContentType.Text.Plain
            )
        }
        route("/media") {
            installMediaRoutes(this)
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
        delete("/{id}") {
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
            val contentType = filePart.contentType ?: run {
                parts.forEach { it.dispose() }
                call.respond(HttpStatusCode.UnsupportedMediaType)
                return@post
            }
            val creationDate = OffsetDateTime.now()
            val originalFilename = filePart.originalFileName ?: "filename"
            val filename = "$creationDate#$originalFilename"
            val file = File(appContext.filesDir, filename)

            file.writeEncrypted(appContext, filePart.streamProvider())
            fileHandler.addFileData(
                filename,
                originalFilename,
                creationDate,
                file.length(),
                contentType
            )

            parts.forEach { it.dispose() }
            call.respond(TestEntity(1, "ayaya"))
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val (file, contentType) = fileHandler.getFileData(id) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respondOutputStream(contentType = contentType) {
                file.putDecryptedContentsIntoOutputStream(appContext, this)
            }
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