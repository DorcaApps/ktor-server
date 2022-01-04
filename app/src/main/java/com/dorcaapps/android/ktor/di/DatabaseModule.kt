package com.dorcaapps.android.ktor.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.ProvidedAutoMigrationSpec
import androidx.room.Room
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dorcaapps.android.ktor.datapersistence.AppDatabase
import com.dorcaapps.android.ktor.extensions.asEncryptedFile
import com.dorcaapps.android.ktor.extensions.getPassphrase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences,
        migrations: List<@JvmSuppressWildcards AutoMigrationSpec>
    ): AppDatabase {
        val passphrase = sharedPreferences.getPassphrase()
        val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
        val factory = SupportFactory(passphraseBytes)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "appdatabase.db"
        ).openHelperFactory(factory)
            .run {
                var builder = this
                for (migration in migrations) {
                    builder = builder.addAutoMigrationSpec(migration)
                }
                builder
            }
            .build()
    }

    @Provides
    fun provideMigrations(
        @ApplicationContext context: Context
    ): List<AutoMigrationSpec> = listOf(
        AutoMigration1To2(context)
    )
}

@ProvidedAutoMigrationSpec
class AutoMigration1To2(private val context: Context) : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        val cursor = db.query("SELECT * FROM filedata")
        if (!cursor.moveToFirst()) {
            return
        }
        val filenameIndex = cursor.getColumnIndexOrThrow("filename")
        val idIndex = cursor.getColumnIndexOrThrow("id")
        do {
            val filename = cursor.getString(filenameIndex)
            val mediaFile = File(context.filesDir, filename)
            mediaFile.asEncryptedFile(context).openFileInput().use { input ->
                var decryptedSize: Long = 0
                // TODO: Write test and check if input.skip(Long.MAX_VALUE) returns valid value
                input.buffered().use {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        iterator.next()
                        decryptedSize++
                    }
                }
                val id = cursor.getInt(idIndex)
                db.execSQL(
                    """
                        UPDATE filedata
                        SET decryptedSize = $decryptedSize
                        WHERE id = $id
                    """.trimIndent()
                )
            }
        } while (cursor.moveToNext())
    }
}