package com.dorcaapps.android.ktor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

private const val NOTIFICATION_ID = 1
private const val STOP_SERVICE_VALUE = 42
private const val STOP_SERVICE_KEY = "arrrr"

class KtorService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private val server by lazy {
        KtorServer(applicationContext)
    }

    private val notificationHelper by lazy {
        NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getIntExtra(STOP_SERVICE_KEY, -1) == STOP_SERVICE_VALUE) {
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        start()
    }

    override fun onDestroy() {
        server.stop(0, 0)
        notificationHelper.cancelNotification(NOTIFICATION_ID)
        super.onDestroy()
        Log.e("MTest", "onDestroy")
    }

    private fun start() {
        startForeground(
            NOTIFICATION_ID,
            notificationHelper.createNotification(this, STOP_SERVICE_KEY, STOP_SERVICE_VALUE)
        )
        server.start()
    }
}