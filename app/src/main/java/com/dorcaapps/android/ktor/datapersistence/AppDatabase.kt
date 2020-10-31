package com.dorcaapps.android.ktor.datapersistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FileData::class, LoginData::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDataDao(): FileDataDao
    abstract fun loginDataDao(): LoginDataDao
}