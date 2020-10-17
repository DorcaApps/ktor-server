package com.dorcaapps.android.ktor.datapersistence

import androidx.room.TypeConverter
import io.ktor.http.*
import java.time.OffsetDateTime

class Converters {
    @TypeConverter
    fun offsetDateTimeToString(value: OffsetDateTime): String = value.toString()

    @TypeConverter
    fun stringToOffsetDateTime(value: String): OffsetDateTime = OffsetDateTime.parse(value)

    @TypeConverter
    fun contentTypeToString(value: ContentType): String = value.toString()

    @TypeConverter
    fun stringToContentType(value: String): ContentType = ContentType.parse(value)
}