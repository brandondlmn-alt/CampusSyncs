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

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Return null if user isn't logged in, preventing empty string queries
    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun getBudgetSettings(): Result<BudgetSettings?> {
        return try {
            val uid = getUserId() ?: return Result.failure(Exception("User not authenticated"))
            val doc = db.collection("budgetSettings").document(uid).get().await()
            val settings = doc.toObject(BudgetSettings::class.java)
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBudgetSettings(allowance: Double): Result<Unit> {
        return try {
            val uid = getUserId() ?: return Result.failure(Exception("User not authenticated"))
            val settings = BudgetSettings(studentId = uid, monthlyAllowance = allowance)
            db.collection("budgetSettings").document(uid).set(settings).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            val uid = getUserId() ?: return Result.failure(Exception("User not authenticated"))
            val docRef = db.collection("expenses").document()
            expense.id = docRef.id
            expense.studentId = uid
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
     * Returns a real-time Flow of expenses for the current month.
     * Now optimized to filter by date directly in the Firestore query!
     */
    fun getCurrentMonthExpenses(): Flow<List<Expense>> = callbackFlow {
        val uid = getUserId()
        if (uid == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        // Optimized: We only download the current month's expenses now
        val subscription = db.collection("expenses")
            .whereEqualTo("studentId", uid)
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()

                // Still sort them locally by newest first
                val sortedExpenses = expenses.sortedByDescending { it.date }

                trySend(sortedExpenses)
            }
        awaitClose { subscription.remove() }
    }
}