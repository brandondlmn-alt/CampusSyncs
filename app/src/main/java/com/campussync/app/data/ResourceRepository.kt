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
            // Map documents manually to attach the Firestore ID to the model
            val bursaries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Bursary::class.java)?.apply {
                    id = doc.id // <--- THIS IS THE FIX
                }
            }
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
            // Map documents manually to attach the Firestore ID to the model
            val accommodation = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Accommodation::class.java)?.apply {
                    id = doc.id // <--- THIS IS THE FIX
                }
            }
            Result.success(accommodation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}