package com.dorcaapps.android.ktor.extensions

import android.graphics.BitmapFactory
import java.io.FileInputStream

fun BitmapFactory.Options.calculateInSampleSize(inputStream: FileInputStream, reqWidth: Int, reqHeight: Int): Int {
    inJustDecodeBounds = true
    BitmapFactory.decodeStream(inputStream, null, this)
    // Decode bitmap with inSampleSize set
    inJustDecodeBounds = false

    // Raw height and width of image
    val (height: Int, width: Int) = run { outHeight to outWidth }
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