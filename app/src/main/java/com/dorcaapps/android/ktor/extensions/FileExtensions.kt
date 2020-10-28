package com.dorcaapps.android.ktor.extensions

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun File.writeEncrypted(applicationContext: Context, byteArray: ByteArray) {
    val encryptedFile = asEncryptedFile(applicationContext)
    encryptedFile.openFileOutput().use {
        it.write(byteArray)
        it.flush()
    }
}

suspend fun File.writeEncrypted(applicationContext: Context, inputStream: InputStream) {
    val encryptedFile = asEncryptedFile(applicationContext)
    encryptedFile.openFileOutput().buffered().use { outputStream ->
        inputStream.copyToSuspend(outputStream)
        inputStream.close()
    }
}

fun File.readEncrypted(applicationContext: Context): ByteArray {
    val encryptedFile = asEncryptedFile(applicationContext)

    val byteArrayOutputStream = ByteArrayOutputStream()
    encryptedFile.openFileInput().use {
        val inputStream = encryptedFile.openFileInput()
        var nextByte: Int = inputStream.read()
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte)
            nextByte = inputStream.read()
        }
    }
    return byteArrayOutputStream.toByteArray()
}

suspend fun File.putDecryptedContentsIntoOutputStream(
    applicationContext: Context,
    outputStream: OutputStream
) {
    val encryptedFile = asEncryptedFile(applicationContext)
    encryptedFile.openFileInput().use { inputStream ->
        inputStream.copyToSuspend(outputStream)
    }
}

fun File.asEncryptedFile(applicationContext: Context): EncryptedFile {
    val mainKey = MasterKey.Builder(applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedFile.Builder(
        applicationContext,
        this,
        mainKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()
}