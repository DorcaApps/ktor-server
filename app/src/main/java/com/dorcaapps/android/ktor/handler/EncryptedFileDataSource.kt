package com.dorcaapps.android.ktor.handler

import android.media.MediaDataSource
import androidx.security.crypto.EncryptedFile

class EncryptedFileDataSource(private val encryptedFile: EncryptedFile) : MediaDataSource() {
    private var input = encryptedFile.openFileInput()
    private var position = 0L

    override fun close() {
        input.close()
    }

    override fun readAt(
        position: Long,
        buffer: ByteArray?,
        offset: Int,
        size: Int
    ): Int {
        if (size == 0) {
            return 0
        }
        if (position < this.position) {
            // TODO: Check if mark() + markSupported() + reset() are valid options
            input.close()
            input = encryptedFile.openFileInput()
            this.position = 0
        }
        if (position > this.position) {
            // TODO: Amount skipped may differ
            input.skip(position - this.position)
            this.position = position
        }
        val bytesReadAmount = input.read(buffer, 0, size)
        this.position += bytesReadAmount
        return bytesReadAmount
    }

    override fun getSize(): Long = -1
}