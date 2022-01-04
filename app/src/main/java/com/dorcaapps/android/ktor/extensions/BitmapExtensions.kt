package com.dorcaapps.android.ktor.extensions

import android.content.Context
import android.graphics.Bitmap
import java.io.File

fun Bitmap.getScaledBitmapWithOriginalAspectRatio(targetSize: Int = 100): Bitmap {
    val originalWidth = width
    val originalHeight = height
    val targetWidth: Int
    val targetHeight: Int
    if (originalWidth >= originalHeight) {
        targetWidth = targetSize
        targetHeight = targetSize * originalHeight / originalWidth
    } else {
        targetHeight = targetSize
        targetWidth = targetSize * originalWidth / originalHeight
    }
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

fun Bitmap.compressIntoEncryptedFile(file: File, context: Context) {
    file.asEncryptedFile(context)
        .openFileOutput().use { output ->
            compress(Bitmap.CompressFormat.PNG, 100, output)
        }
}