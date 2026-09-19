package com.campussync.app.models

/**
 * Data class representing an academic mark/grade entry.
 * Contains a no-arg constructor required for Firebase Firestore deserialization.
 */
data class MarkEntry(
    var id: String = "",
    var studentId: String = "",
    var moduleCode: String = "",
    var assessmentType: String = "",
    var mark: Double = 0.0,
    var weight: Double = 0.0
)
