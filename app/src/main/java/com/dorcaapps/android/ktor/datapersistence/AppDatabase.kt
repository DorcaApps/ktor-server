package com.dorcaapps.android.ktor.datapersistence

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dorcaapps.android.ktor.di.AutoMigration1To2

@Database(
    entities = [FileData::class, LoginData::class],
    exportSchema = true,
    version = 2,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = AutoMigration1To2::class
        )
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDataDao(): FileDataDao
    abstract fun loginDataDao(): LoginDataDao
}