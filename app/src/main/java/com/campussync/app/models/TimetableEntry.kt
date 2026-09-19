package com.campussync.app.models

/**
 * Data class representing a single class/lecture entry in the student's timetable.
 * Contains a no-arg constructor required for Firebase Firestore deserialization.
 */
data class TimetableEntry(
    var id: String = "",
    var studentId: String = "",
    var moduleCode: String = "",
    var moduleName: String = "",
    var dayOfWeek: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var venue: String = ""
)
