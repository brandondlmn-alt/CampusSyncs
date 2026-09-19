package com.campussync.app.data

import com.campussync.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository class to handle Firebase Authentication and Firestore user profile operations.
 * Uses Coroutines (await()) for cleaner asynchronous code.
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Registers a new user with email and password, then saves their profile to Firestore.
     */
    suspend fun register(email: String, password: String, profile: User): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User registration failed: No UID")
            
            // Update profile with the new UID
            profile.uid = uid
            
            // Save to Firestore
            db.collection("users").document(uid).set(profile).await()
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs in an existing user.
     */
    suspend fun login(email: String, password: String): Result<User?> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val snapshot = db.collection("users").document(uid).get().await()
                val user = snapshot.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed: User null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a password reset email.
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Checks if a user is currently logged in.
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Gets the current user ID.
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
