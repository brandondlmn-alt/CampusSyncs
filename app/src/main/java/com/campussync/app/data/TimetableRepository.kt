package com.campussync.app.data

import com.campussync.app.models.TimetableEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing Timetable entries in Firestore.
 */
class TimetableRepository {

    // Initialize Firebase instances lazily to allow unit testing without a mock environment.
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val collection by lazy { db.collection("timetable") }

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Adds a new timetable entry.
     */
    suspend fun addEntry(entry: TimetableEntry): Result<Unit> {
        return try {
            val docRef = collection.document()
            entry.id = docRef.id
            entry.studentId = getUserId()
            docRef.set(entry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing timetable entry.
     */
    suspend fun updateEntry(entry: TimetableEntry): Result<Unit> {
        return try {
            collection.document(entry.id).set(entry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a timetable entry.
     */
    suspend fun deleteEntry(entryId: String): Result<Unit> {
        return try {
            collection.document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a Flow of timetable entries for the current user, updated in real-time.
     */
    fun getTimetableEntries(): Flow<List<TimetableEntry>> = callbackFlow {
        val subscription = collection
            .whereEqualTo("studentId", getUserId())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.toObjects(TimetableEntry::class.java) ?: emptyList()
                
                val dayOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                val sortedEntries = entries.sortedWith(compareBy(
                    { dayOrder.indexOf(it.dayOfWeek) },
                    { it.startTime }
                ))
                
                trySend(sortedEntries)
            }
        awaitClose { subscription.remove() }
    }
}
