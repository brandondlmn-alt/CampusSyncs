package com.campussync.app

import com.campussync.app.models.Expense
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for budget and expense calculation logic.
 */
class BudgetCalculationTest {

    @Test
    fun testRemainingBudget_Calculation() {
        val allowance = 2000.0
        val expenses = listOf(
            Expense(amount = 150.50),
            Expense(amount = 300.00),
            Expense(amount = 50.00)
        )
        
        val totalSpent = expenses.sumOf { it.amount }
        val remaining = allowance - totalSpent
        
        assertEquals(500.50, totalSpent, 0.01)
        assertEquals(1499.50, remaining, 0.01)
    }

    @Test
    fun testBudgetOverspent_Calculation() {
        val allowance = 100.0
        val expenses = listOf(Expense(amount = 150.0))
        
        val totalSpent = expenses.sumOf { it.amount }
        val remaining = allowance - totalSpent
        
        assertEquals(-50.0, remaining, 0.01)
    }
}
