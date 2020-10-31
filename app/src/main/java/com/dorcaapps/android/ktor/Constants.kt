package com.dorcaapps.android.ktor

object Constants {
    object Authentication {
        const val LOGIN = "login"
        const val SESSION = "session"
    }

    object Notification {
        const val CHANNEL_ID_SERVICE = "the_notification_id"
        const val CHANNEL_ID_REGISTER = "the_regsiter_notification_id"

        const val NOTIFICATION_ID_SERVICE = 1
        const val NOTIFICATION_ID_REGISTER = 2

        const val SERVICE_ACTION_STOP = 0
        const val SERVICE_ACTION_REGISTER = 1
        const val SERVICE_ACTION_DATA_PAYLOAD = "awoof"
        const val SERVICE_ACTION_KEY = "arrrr"
    }
}