package com.campussync.app

import com.campussync.app.models.MarkEntry
import com.campussync.app.utils.MarkUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for academic mark calculation logic.
 * Decoupled from Firebase to ensure CI stability.
 */
class MarkCalculationTest {

    @Test
    fun testWeightedAverage_Basic() {
        val marks = listOf(
            MarkEntry(mark = 80.0, weight = 50.0),
            MarkEntry(mark = 90.0, weight = 50.0)
        )
        val result = MarkUtils.calculateWeightedAverage(marks)
        assertEquals(85.0, result, 0.1)
    }

    @Test
    fun testWeightedAverage_VaryingWeights() {
        val marks = listOf(
            MarkEntry(mark = 100.0, weight = 10.0),
            MarkEntry(mark = 50.0, weight = 90.0)
        )
        val result = MarkUtils.calculateWeightedAverage(marks)
        assertEquals(55.0, result, 0.1)
    }

    @Test
    fun testWeightedAverage_EmptyList() {
        val marks = emptyList<MarkEntry>()
        val result = MarkUtils.calculateWeightedAverage(marks)
        assertEquals(0.0, result, 0.1)
    }

    @Test
    fun testWeightedAverage_ZeroWeights() {
        val marks = listOf(
            MarkEntry(mark = 80.0, weight = 0.0)
        )
        val result = MarkUtils.calculateWeightedAverage(marks)
        assertEquals(0.0, result, 0.1)
    }
}
