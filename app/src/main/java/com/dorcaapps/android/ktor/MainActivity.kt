package com.dorcaapps.android.ktor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.Serializable

@Serializable
data class TestEntity(val myInt: Int, val myString: String)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startForegroundService(Intent(this, KtorService::class.java))
    }
}