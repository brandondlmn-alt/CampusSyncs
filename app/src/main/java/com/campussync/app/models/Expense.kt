package com.campussync.app.models

/**
 * Data class representing a financial expense entry.
 */
data class Expense(
    var id: String = "",
    var studentId: String = "",
    var category: String = "",
    var amount: Double = 0.0,
    var date: Long = System.currentTimeMillis(),
    var description: String = ""
)
