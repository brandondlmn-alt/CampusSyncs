package com.campussync.app.models

/**
 * Data model for a saved university assessment.
 */
data class Assessment(
    var id: String = "",
    var studentId: String = "",
    var moduleCode: String = "",
    var moduleName: String = "",
    var assessmentType: String = "",
    var submissionMethod: String = "",
    var requiresTurnitin: Boolean = false,
    var dueDate: String = "",           // "2026-09-15"
    var dueTime: String = "",           // "23:50"
    var createdAt: Long = System.currentTimeMillis(),
    var completed: Boolean = false,
    var reminderOffsetMinutes: Int = -1
)