package com.dorcaapps.android.ktor.datapersistence

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dorcaapps.android.ktor.extensions.getPassphrase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [FileData::class, LoginData::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDataDao(): FileDataDao
    abstract fun loginDataDao(): LoginDataDao

    companion object {
        fun get(applicationContext: Context): AppDatabase {
            val mainKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPrefsFile = "MySharedPreferencesFile"
            val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
                applicationContext,
                sharedPrefsFile,
                mainKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val passphrase = sharedPreferences.getPassphrase()
            val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
            val factory = SupportFactory(passphraseBytes)

            return Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "appdatabase.db"
            ).openHelperFactory(factory)
                .build()
        }
    }
}