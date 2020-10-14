package com.dorcaapps.android.ktor

import android.content.Context
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.util.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class KtorServer(private val appContext: Context) {

    private val serverEngine by lazy { createServerEngine() }

    fun start() {
        serverEngine.start(false)
    }

    fun stop(gracePeriodMillis: Long, timeOutMillis: Long) {
        serverEngine.stop(gracePeriodMillis, timeOutMillis)
    }

    private fun createServerEngine(): JettyApplicationEngine = embeddedServer(Jetty, port = 8080) {
        val exif = ExifInterface(File(appContext.cacheDir, "Maik.jpg"))
        exif
        install(ContentNegotiation) {
            json()
        }
        install(DefaultHeaders)
        this.plus(CoroutineExceptionHandler { _, throwable ->
            Log.e("MTest", "booyah", throwable)
        })
        install(CallLogging) {
            logger = LoggerFactory.getLogger("Application.Test")
            this.format {
                "\nRequest:\n" +
                        it.request.headers.flattenEntries().joinToString("\n") +
                        "\nResponse:\n" +
                        it.response.headers.allValues().flattenEntries().joinToString("\n")
            }
        }
        routing {
            get("/") {
                call.respondText(
                    "Hello World",
                    ContentType.Text.Plain
                )
//                Log.e("MTest", call.toString())
            }
            get("/demo") {
                call.respondText(
                    "HELLO WORLD!"
                )
            }
            get("/test") {
                call.respond(HttpStatusCode.OK, TestEntity(35, "theTest"))
            }
            post("/post") {
                val requestBody = call.receive<TestEntity>()
                call.respond(HttpStatusCode.OK, TestEntity(requestBody.myInt, requestBody.myString))
            }
            post("/postfile") {
                Log.e("MTest", call.request.headers.flattenEntries().joinToString())
                val multipart = try {
                    call.receiveMultipart()
                } catch (e: Exception) {
                    Log.e("MTest", "caught", e)
                    throw e
                }

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            Log.e("MTest", "isFormItem! ${part.value}")
                        }
                        is PartData.FileItem -> {
                            val extension = File(part.originalFileName!!).extension
                            val file = File(appContext.cacheDir, part.originalFileName!!)
                            part.streamProvider().use { input ->
                                file.outputStream().buffered()
                                    .use { output -> input.copyToSuspend(output) }
                            }
                        }
                    }
                    part.dispose()
                }
                call.respond(TestEntity(1, "ayaya"))
            }
            get("/getfile") {
                call.respondFile(File(appContext.cacheDir, "Maik.jpg"))
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