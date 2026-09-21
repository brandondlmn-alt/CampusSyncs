package com.campussync.app.data

import com.campussync.app.models.Accommodation
import com.campussync.app.models.Bursary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching academic resources such as Bursaries and Accommodation.
 */
class ResourceRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    /**
     * Fetches all bursary entries from Firestore.
     */
    suspend fun getBursaries(): Result<List<Bursary>> {
        return try {
            val snapshot = db.collection("bursaries").get().await()
            val bursaries = snapshot.toObjects(Bursary::class.java)
            Result.success(bursaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all accommodation listings from Firestore.
     */
    suspend fun getAccommodation(): Result<List<Accommodation>> {
        return try {
            val snapshot = db.collection("accommodation").get().await()
            val accommodation = snapshot.toObjects(Accommodation::class.java)
            Result.success(accommodation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
