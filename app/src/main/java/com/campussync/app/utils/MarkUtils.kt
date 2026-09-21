package com.campussync.app.utils

import com.campussync.app.models.MarkEntry

/**
 * Utility for performing academic calculations.
 * Pure logic with no dependencies on Firebase for easy unit testing.
 */
object MarkUtils {

    /**
     * Calculates the weighted average for a list of marks.
     * Formula: sum(mark * weight) / sum(weight)
     */
    fun calculateWeightedAverage(marks: List<MarkEntry>): Double {
        if (marks.isEmpty()) return 0.0
        var totalWeightedMark = 0.0
        var totalWeight = 0.0
        
        for (m in marks) {
            totalWeightedMark += (m.mark * m.weight)
            totalWeight += m.weight
        }
        
        if (totalWeight == 0.0) return 0.0
        return totalWeightedMark / totalWeight
    }
}
