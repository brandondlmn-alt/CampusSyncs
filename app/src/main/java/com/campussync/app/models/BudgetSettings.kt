package com.campussync.app.models

/**
 * Data class representing student's budget settings.
 */
data class BudgetSettings(
    var studentId: String = "",
    var monthlyAllowance: Double = 0.0
)
