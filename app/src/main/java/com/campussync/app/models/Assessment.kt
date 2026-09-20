package com.campussync.app.models

/**
 * Data model for a saved university assessment.
 * Contains a no-arg constructor required for Firestore.
 */
data class Assessment(
    var id: String = "",
    var studentId: String = "",
    var moduleCode: String = "",
    var moduleName: String = "",
    var assessmentType: String = "",
    var submissionMethod: String = "",  // "Online Submission" or "Campus Sitting"
    var requiresTurnitin: Boolean = false,
    var dueDate: String = "",           // ISO format "2026-09-15"
    var dueTime: String = "",           // "23:50"
    var createdAt: Long = System.currentTimeMillis(),
    var completed: Boolean = false
)
