package com.campussync.app.utils

import com.campussync.app.models.MarkEntry

/**
 * Pure math logic for academic calculations.
 * No Firebase dependencies here to ensure tests pass in CI.
 */
object MarkUtils {
    fun calculateWeightedAverage(marks: List<MarkEntry>): Double {
        if (marks.isEmpty()) return 0.0
        var totalWeightedMark = 0.0
        var totalWeight = 0.0
        for (m in marks) {
            totalWeightedMark += (m.mark * m.weight)
            totalWeight += m.weight
        }
        return if (totalWeight == 0.0) 0.0 else totalWeightedMark / totalWeight
    }
}
