package com.dorcaapps.android.ktor.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.security.crypto.EncryptedFile

fun EncryptedFile.decodeSampledBitmap(reqWidth: Int, reqHeight: Int): Bitmap = // First decode with inJustDecodeBounds=true to check dimensions
    BitmapFactory.Options().run {
        // Calculate inSampleSize
        openFileInput().use { fileInputStream ->
            inSampleSize = calculateInSampleSize(fileInputStream, reqWidth, reqHeight)
        }

        openFileInput().use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, this)
        }!!
    }