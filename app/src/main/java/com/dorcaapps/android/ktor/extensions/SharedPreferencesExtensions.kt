package com.dorcaapps.android.ktor.extensions

import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.*

private const val PASSPHRASE_KEY = "UltraSafePassphraseKey"
fun SharedPreferences.getPassphrase(): String =
    getString(PASSPHRASE_KEY, null) ?: run {
        val passphrase = UUID.randomUUID().toString()
        edit { putString(PASSPHRASE_KEY, passphrase) }
        passphrase
    }