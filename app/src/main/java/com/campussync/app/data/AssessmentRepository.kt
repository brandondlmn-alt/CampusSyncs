package com.campussync.app.data

import com.campussync.app.models.Assessment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing academic assessment data.
 */
class AssessmentRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val collection by lazy { db.collection("assessments") }

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Returns a real-time Flow of assessments for the current user.
     */
    fun getAssessments(): Flow<List<Assessment>> = callbackFlow {
        val subscription = collection
            .whereEqualTo("studentId", getUserId())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val assessments = snapshot?.toObjects(Assessment::class.java) ?: emptyList()
                trySend(assessments)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Updates the completion status of an assessment.
     */
    suspend fun updateCompletionStatus(assessmentId: String, isCompleted: Boolean): Result<Unit> {
        return try {
            collection.document(assessmentId).update("completed", isCompleted).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
