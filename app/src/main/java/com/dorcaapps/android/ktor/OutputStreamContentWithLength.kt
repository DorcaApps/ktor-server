package com.dorcaapps.android.ktor

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.OutputStream

/**
 * Ktor does not yet support responding with a ByteReadChannel or OutputStream with content length header.
 * Also, in order for the PartialContent plugin to work,
 * any kind of OutgoingContent.ReadChannelContent is required.
 * Thus this class does not work with PartialContent
 * @see <a href="https://ktor.io/docs/partial-content.html#detailed-description">Ktor docs</a>
 * */
class OutputStreamContentWithLength(
    private val body: suspend OutputStream.() -> Unit,
    override val contentType: ContentType,
    override val status: HttpStatusCode? = null,
    override val contentLength: Long? = null
) : OutgoingContent.WriteChannelContent() {

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.toOutputStream().use { it.body() }
    }
}

