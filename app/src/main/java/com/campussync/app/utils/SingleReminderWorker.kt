package com.campussync.app.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SingleReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val moduleName = inputData.getString("moduleName") ?: "Assignment"
        val type = inputData.getString("type") ?: "Task"
        val notificationId = inputData.getInt("notificationId", 1)

        NotificationHelper.showOngoingNotification(
            applicationContext,
            notificationId,
            "Reminder: $moduleName ⏰",
            "Your $type for $moduleName is due! This notification will stay here until you dismiss it."
        )

        return Result.success()
    }
}