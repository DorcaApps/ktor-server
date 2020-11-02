package com.dorcaapps.android.ktor.extensions

import android.graphics.Bitmap

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