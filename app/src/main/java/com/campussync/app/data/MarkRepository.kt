package com.campussync.app.data

import com.campussync.app.models.MarkEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing student academic marks in Firestore.
 */
class MarkRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("marks")

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Adds a new mark entry to Firestore.
     */
    suspend fun addMark(mark: MarkEntry): Result<Unit> {
        return try {
            val docRef = collection.document()
            mark.id = docRef.id
            mark.studentId = getUserId()
            docRef.set(mark).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing mark entry in Firestore.
     */
    suspend fun updateMark(mark: MarkEntry): Result<Unit> {
        return try {
            collection.document(mark.id).set(mark).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a specific mark entry from Firestore.
     */
    suspend fun deleteMark(markId: String): Result<Unit> {
        return try {
            collection.document(markId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a real-time Flow of mark entries for the current user.
     */
    fun getMarkEntries(): Flow<List<MarkEntry>> = callbackFlow {
        val subscription = collection
            .whereEqualTo("studentId", getUserId())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val marks = snapshot?.toObjects(MarkEntry::class.java) ?: emptyList()
                trySend(marks)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Calculates the weighted average for a list of marks.
     */
    fun calculateWeightedAverage(marks: List<MarkEntry>): Double {
        if (marks.isEmpty()) return 0.0
        var totalWeightedMark = 0.0
        var totalWeight = 0.0
        
        for (m in marks) {
            totalWeightedMark += (m.mark * m.weight)
            totalWeight += m.weight
        }
        
        if (totalWeight == 0.0) return 0.0
        return totalWeightedMark / totalWeight
    }
}
