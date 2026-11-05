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

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val collection by lazy { db.collection("chats") }
    private val TAG = "ChatRepository"

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Saves a message to Firestore.
     */
    suspend fun saveMessage(message: ChatMessage): Result<Unit> {
        return try {
            val uid = getUserId()
            if (uid.isEmpty()) return Result.failure(Exception("User not logged in"))

            val docRef = collection.document()
            message.id = docRef.id
            message.studentId = uid
            docRef.set(message).await()
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

            val snapshot = collection
                .whereEqualTo("studentId", uid)
                .get()
                .await()

            val history = snapshot.toObjects(ChatMessage::class.java)
            val sortedHistory = history.sortedBy { it.timestamp }

            Log.d(TAG, "Loaded ${sortedHistory.size} messages from history")
            Result.success(sortedHistory)
        } catch (e: Exception) {
            Log.e(TAG, "History fetch failed", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a specific message from Firestore.
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            if (messageId.isEmpty()) return Result.failure(Exception("Invalid message ID"))
            collection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            Result.failure(e)
        }
    }
}