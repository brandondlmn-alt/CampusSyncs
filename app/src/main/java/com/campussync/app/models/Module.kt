package com.campussync.app.models

/**
 * Data class representing a University Module.
 * Used to group timetable entries and marks.
 */
data class Module(
    var id: String = "",
    var studentId: String = "",
    var code: String = "",
    var name: String = "",
    var targetClassesPerWeek: Int = 0
)
