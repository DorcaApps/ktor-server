package com.dorcaapps.android.ktor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.dorcaapps.android.ktor.Constants.Notification.NOTIFICATION_ID_SERVICE
import com.dorcaapps.android.ktor.Constants.Notification.SERVICE_ACTION_KEY
import com.dorcaapps.android.ktor.Constants.Notification.SERVICE_ACTION_REGISTER
import com.dorcaapps.android.ktor.Constants.Notification.SERVICE_ACTION_STOP
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KtorService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    @Inject
    lateinit var server: KtorServer

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actionId = intent?.getIntExtra(SERVICE_ACTION_KEY, -1)
            ?.takeUnless { it == -1 }
            ?: return super.onStartCommand(intent, flags, startId)
        when {
            actionId == SERVICE_ACTION_STOP -> stopSelf()
            actionId >= SERVICE_ACTION_REGISTER -> registerUser(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        start()
    }

    override fun onDestroy() {
        server.stop(0, 0)
        notificationHelper.cancelNotification(NOTIFICATION_ID_SERVICE)
        super.onDestroy()
        Log.e("MTest", "onDestroy")
    }

    private fun start() {
        startForeground(
            NOTIFICATION_ID_SERVICE,
            notificationHelper.createForegroundNotification(SERVICE_ACTION_STOP)
        )
        server.start()
    }

    private fun registerUser(intent: Intent) {
        val notificationId =
            intent.getIntExtra(SERVICE_ACTION_KEY, -1).takeUnless { it == -1 } ?: return
        Log.e("MTest", intent.toString())
        notificationHelper.cancelNotification(notificationId)
    }
}