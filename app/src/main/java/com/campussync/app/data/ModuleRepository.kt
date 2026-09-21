package com.campussync.app.data

import com.campussync.app.models.Module
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing student Modules in Firestore.
 */
class ModuleRepository {

    // Initialize Firebase instances lazily to allow unit testing without a mock environment.
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val collection by lazy { db.collection("modules") }

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    suspend fun addModule(module: Module): Result<Unit> {
        return try {
            val docRef = collection.document()
            module.id = docRef.id
            module.studentId = getUserId()
            docRef.set(module).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateModule(module: Module): Result<Unit> {
        return try {
            collection.document(module.id).set(module).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteModule(moduleId: String): Result<Unit> {
        return try {
            collection.document(moduleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getModules(): Flow<List<Module>> = callbackFlow {
        val subscription = collection
            .whereEqualTo("studentId", getUserId())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val modules = snapshot?.toObjects(Module::class.java) ?: emptyList()
                trySend(modules)
            }
        awaitClose { subscription.remove() }
    }
}
