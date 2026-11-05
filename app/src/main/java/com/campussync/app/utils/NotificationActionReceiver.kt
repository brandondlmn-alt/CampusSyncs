package com.campussync.app.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Listens for the "Dismiss" button click on the ongoing notification.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "CANCEL_NOTIFICATION") {
            val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
            if (notificationId != -1) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
        }
    }
}