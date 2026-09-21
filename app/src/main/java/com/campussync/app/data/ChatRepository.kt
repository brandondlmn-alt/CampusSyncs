package com.campussync.app.data

import android.util.Log
import com.campussync.app.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing Chat history in Firestore.
 */
class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("chats")
    private val TAG = "ChatRepository"

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Saves a message to Firestore.
     */
    suspend fun saveMessage(message: ChatMessage): Result<Unit> {
        return try {
            val uid = getUserId()
            if (uid.isEmpty()) return Result.failure(Exception("User not logged in"))

            message.studentId = uid
            collection.add(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Save failed", e)
            Result.failure(e)
        }
    }

    /**
     * Loads chat history for the current user.
     */
    suspend fun getChatHistory(): Result<List<ChatMessage>> {
        return try {
            val uid = getUserId()
            if (uid.isEmpty()) return Result.failure(Exception("User not logged in"))

            // Query messages for the current user.
            val snapshot = collection
                .whereEqualTo("studentId", uid)
                .get()
                .await()

            val history = snapshot.toObjects(ChatMessage::class.java)

            // Sort history client-side by timestamp.
            val sortedHistory = history.sortedBy { it.timestamp }

            Log.d(TAG, "Loaded ${sortedHistory.size} messages from history")
            Result.success(sortedHistory)
        } catch (e: Exception) {
            Log.e(TAG, "History fetch failed", e)
            Result.failure(e)
        }
    }
}
