package com.campussync.app.models

/**
 * Model representing an individual grade or mark entry.
 */
data class MarkEntry(
    var id: String = "",
    var studentId: String = "",
    var moduleCode: String = "",
    var assessmentType: String = "",
    var mark: Double = 0.0,
    var weight: Double = 0.0
)
