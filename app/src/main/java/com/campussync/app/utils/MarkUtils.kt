package com.campussync.app.utils

import com.campussync.app.models.MarkEntry

/**
 * Utility for performing academic calculations without Firebase dependencies.
 */
object MarkUtils {

    /**
     * Calculates the weighted average for a list of marks.
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
