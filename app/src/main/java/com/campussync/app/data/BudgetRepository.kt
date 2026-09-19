package com.campussync.app.data

import com.campussync.app.models.BudgetSettings
import com.campussync.app.models.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Repository for managing Budget settings and Expenses in Firestore.
 */
class BudgetRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    // --- Budget Settings ---

    suspend fun getBudgetSettings(): Result<BudgetSettings?> {
        return try {
            val doc = db.collection("budgetSettings").document(getUserId()).get().await()
            val settings = doc.toObject(BudgetSettings::class.java)
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBudgetSettings(allowance: Double): Result<Unit> {
        return try {
            val settings = BudgetSettings(studentId = getUserId(), monthlyAllowance = allowance)
            db.collection("budgetSettings").document(getUserId()).set(settings).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Expenses ---

    suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            val docRef = db.collection("expenses").document()
            expense.id = docRef.id
            expense.studentId = getUserId()
            docRef.set(expense).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            db.collection("expenses").document(expense.id).set(expense).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            db.collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a Flow of expenses for the current month.
     * Both filtering by date and sorting are done client-side to avoid the requirement for a composite index.
     */
    fun getCurrentMonthExpenses(): Flow<List<Expense>> = callbackFlow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        // Only filter by studentId to avoid requiring a composite index for inequalities
        val subscription = db.collection("expenses")
            .whereEqualTo("studentId", getUserId())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allExpenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                
                // Filter by date and sort descending in Kotlin
                val filteredAndSorted = allExpenses
                    .filter { it.date >= startOfMonth }
                    .sortedByDescending { it.date }
                
                trySend(filteredAndSorted)
            }
        awaitClose { subscription.remove() }
    }
}
