package com.campussync.app.utils

import com.campussync.app.models.MarkEntry

/**
 * Pure utility for academic calculations.
 * This object contains no Firebase dependencies to ensure stability during unit testing.
 */
object MarkUtils {

    /**
     * Calculates the weighted average: sum(mark * weight) / sum(weight).
     * Returns 0.0 if the list is empty or total weight is zero.
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
