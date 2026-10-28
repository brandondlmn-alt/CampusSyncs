package com.campussync.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * BroadcastReceiver to trigger local notifications for assessment deadlines.
 */
class AssessmentNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val moduleCode = intent.getStringExtra("MODULE_CODE") ?: "Assessment"
        val type = intent.getStringExtra("TYPE") ?: "Deadline"
        val notificationId = intent.getIntExtra("ID", 0)

        createNotificationChannel(context)

        // Get the app launch intent
        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, notificationId, mainIntent, flags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Reminder: $moduleCode")
            .setContentText("Your $type is due soon!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission missing on API 33+
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Assessment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for assignment and test deadlines"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "assessment_reminders"
    }
}
