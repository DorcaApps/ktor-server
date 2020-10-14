package com.dorcaapps.android.ktor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

private const val PRIMARY_CHANNEL_ID = "the_notification_id"

class NotificationHelper(context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotification(context: Context, stopServiceKey: String, stopServiceValue: Int): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(context, PRIMARY_CHANNEL_ID)
            .setContentTitle("Ktor Server")
            .setContentText("Running Ktor Server...")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setNotificationSilent()
            .addAction(
                NotificationCompat.Action(
                    null, "Cancel", PendingIntent.getService(
                        context,
                        0,
                        Intent(context, KtorService::class.java).apply { putExtra(stopServiceKey, stopServiceValue) },
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                )
            )
            .build()
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun createNotificationChannel() {
        // Notification channels are only available in OREO and higher.
        // So, add a check on SDK version.
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            // Create the NotificationChannel with all the parameters.
            val notificationChannel = NotificationChannel(
                PRIMARY_CHANNEL_ID,
                "Stand up notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

}