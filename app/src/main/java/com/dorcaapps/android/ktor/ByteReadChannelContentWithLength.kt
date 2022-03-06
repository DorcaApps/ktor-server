package com.dorcaapps.android.ktor

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*

/**
 * Ktor does not yet support responding with a ByteReadChannel with content length header.
 * Also, in order for the PartialContent plugin to work,
 * any kind of OutgoingContent.ReadChannelContent is required.
 * @see <a href="https://ktor.io/docs/partial-content.html#detailed-description">Ktor docs</a>
 * */
class ByteReadChannelContentWithLength(
    private val body: ByteReadChannel,
    override val contentType: ContentType,
    override val status: HttpStatusCode? = null,
    override val contentLength: Long? = null
) : OutgoingContent.ReadChannelContent() {
    override fun readFrom(): ByteReadChannel = body
}