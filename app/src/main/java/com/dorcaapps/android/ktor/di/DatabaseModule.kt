package com.dorcaapps.android.ktor.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.dorcaapps.android.ktor.datapersistence.AppDatabase
import com.dorcaapps.android.ktor.extensions.getPassphrase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@InstallIn(ApplicationComponent::class)
@Module
object DatabaseModule {
    @Provides
    fun provideDatabase(@ApplicationContext context: Context, sharedPreferences: SharedPreferences): AppDatabase {
        val passphrase = sharedPreferences.getPassphrase()
        val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
        val factory = SupportFactory(passphraseBytes)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "appdatabase.db"
        ).openHelperFactory(factory)
            .build()
    }
}