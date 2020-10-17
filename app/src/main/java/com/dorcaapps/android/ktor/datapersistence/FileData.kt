package com.dorcaapps.android.ktor.datapersistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.ktor.http.*
import java.time.OffsetDateTime

@Entity(indices = [Index(value = ["filename"], unique = true)])
data class FileData(
    val filename: String,
    val originalFilename: String,
    val lastChanged: OffsetDateTime,
    val size: Long,
    val contentType: ContentType,
    @PrimaryKey(autoGenerate = true) val id: Int? = null
)