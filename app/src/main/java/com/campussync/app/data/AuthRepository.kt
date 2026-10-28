package com.campussync.app.data

import com.campussync.app.models.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for Firebase Authentication and Firestore user profile management.
 */
class AuthRepository {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    suspend fun register(email: String, password: String, profile: User): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User registration failed: No UID")
            profile.uid = uid
            db.collection("users").document(uid).set(profile).await()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User?> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val snapshot = db.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    Result.success(user)
                } else {
                    Result.failure(Exception("User profile not found. Please contact support or register again."))
                }
            } else {
                Result.failure(Exception("Login failed: User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs in with Google credentials and ensures a Firestore profile exists.
     */
    suspend fun signInWithGoogle(credential: AuthCredential): Result<User?> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google Sign-In failed")
            val uid = firebaseUser.uid

            val snapshot = db.collection("users").document(uid).get().await()
            
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                Result.success(user)
            } else {
                // Create a basic profile for new Google users
                val newUser = User(
                    uid = uid,
                    email = firebaseUser.email ?: "",
                    firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "",
                    lastName = firebaseUser.displayName?.split(" ")?.getOrNull(1) ?: "",
                    createdAt = System.currentTimeMillis()
                )
                db.collection("users").document(uid).set(newUser).await()
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<User?> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val snapshot = db.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.failure(Exception("User profile not found in database"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            db.collection("users").document(uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")
            val email = user.email ?: throw Exception("User email not found")
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
