package com.campussync.app.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Background worker that checks Firestore for upcoming assignment deadlines
 * and triggers a local notification based on the user's custom preferences.
 */
class DeadlineCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()

        try {
            // 1. Fetch the User to check their notification preferences
            val userDoc = db.collection("users").document(uid).get().await()

            // If they disabled notifications overall, stop here.
            val notificationsEnabled = userDoc.getBoolean("notificationsEnabled") ?: true
            if (!notificationsEnabled) return Result.success()

            // Get how many days before they want the alert (default is 1 day)
            val daysBefore = userDoc.getLong("notificationDaysBefore")?.toInt() ?: 1

            // 2. Fetch incomplete assessments for this user
            val snapshot = db.collection("assessments")
                .whereEqualTo("studentId", uid)
                .whereEqualTo("completed", false)
                .get()
                .await()

            if (snapshot.isEmpty) return Result.success()

            // 3. Setup target date based on their preference (e.g., checking for dates 2 days from now)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, daysBefore)
            val targetDateString = dateFormat.format(calendar.time)

            // 4. Loop and check
            for (doc in snapshot.documents) {
                val dueDate = doc.getString("dueDate") ?: continue
                val moduleName = doc.getString("moduleName") ?: "Assignment"
                val assessmentType = doc.getString("assessmentType") ?: "Task"
                val dueTime = doc.getString("dueTime") ?: ""

                if (dueDate == targetDateString) {
                    val title = "Upcoming Deadline ⚠️"
                    val message = "Your $moduleName $assessmentType is due in $daysBefore day(s) at $dueTime. Submit it soon!"

                    NotificationHelper.showDeadlineNotification(
                        applicationContext,
                        title,
                        message
                    )
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("DeadlineWorker", "Error checking deadlines", e)
            return Result.retry()
        }
    }
}