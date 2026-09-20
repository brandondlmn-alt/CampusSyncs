package com.campussync.app.data

import android.util.Log
import com.campussync.app.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("chats")
    private val TAG = "ChatRepository"

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

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

    suspend fun getChatHistory(): Result<List<ChatMessage>> {
        return try {
            val uid = getUserId()
            if (uid.isEmpty()) return Result.failure(Exception("User not logged in"))

            // Simplest possible query: only one 'where' clause, NO 'orderBy'
            val snapshot = collection
                .whereEqualTo("studentId", uid)
                .get()
                .await()

            val history = snapshot.toObjects(ChatMessage::class.java)

            // WE SORT LOCALLY IN KOTLIN (Does not require Firestore Index)
            val sortedHistory = history.sortedBy { it.timestamp }

            Log.d(TAG, "Loaded ${sortedHistory.size} messages from history")
            Result.success(sortedHistory)
        } catch (e: Exception) {
            Log.e(TAG, "History fetch failed", e)
            Result.failure(e)
        }
    }
}