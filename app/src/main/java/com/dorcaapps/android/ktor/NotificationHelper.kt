package com.dorcaapps.android.ktor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dorcaapps.android.ktor.Constants.Notification.CHANNEL_ID_REGISTER
import com.dorcaapps.android.ktor.Constants.Notification.CHANNEL_ID_SERVICE
import com.dorcaapps.android.ktor.datapersistence.LoginData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var counter = Constants.Notification.NOTIFICATION_ID_REGISTER

    fun showRegisterNotification(
        loginData: LoginData
    ) {
        createNotificationChannel(CHANNEL_ID_REGISTER, "Registration")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REGISTER)
            .setContentTitle("Ktor Server")
            .setContentText("User '${loginData.username}' wants to register")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setNotificationSilent()
            .addAction(
                NotificationCompat.Action(
                    null, "Confirm", PendingIntent.getService(
                        context,
                        1,
                        Intent(context, KtorService::class.java).apply {
                            putExtra(
                                Constants.Notification.SERVICE_ACTION_KEY,
                                counter
                            )
                            putExtra(
                                Constants.Notification.SERVICE_ACTION_DATA_PAYLOAD,
                                loginData
                            )
                        },
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                )
            )
            .build()
        notificationManager.notify(counter, notification)
        counter++
    }

    fun createForegroundNotification(
        stopServiceValue: Int
    ): Notification {
        createNotificationChannel(CHANNEL_ID_SERVICE, "Ktor Server")
        return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setContentTitle("Ktor Server")
            .setContentText("Running Ktor Server...")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setNotificationSilent()
            .addAction(
                NotificationCompat.Action(
                    null, "Cancel", PendingIntent.getService(
                        context,
                        0,
                        Intent(context, KtorService::class.java).apply {
                            putExtra(
                                Constants.Notification.SERVICE_ACTION_KEY,
                                stopServiceValue
                            )
                        },
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                )
            )
            .build()
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun createNotificationChannel(channelId: String, name: String) {
        // Notification channels are only available in OREO and higher.
        // So, add a check on SDK version.
        // Create the NotificationChannel with all the parameters.
        val notificationChannel = NotificationChannel(
            channelId,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

}