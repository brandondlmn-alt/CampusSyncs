package com.campussync.app.models

/**
 * Transient data class for reviewing extracted timetable data.
 */
data class ScannedTimetableEntry(
    var moduleCode: String = "",
    var moduleName: String = "",
    var dayOfWeek: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var venue: String = ""
)
