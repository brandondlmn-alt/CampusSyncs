package com.campussync.app.models

/**
 * Data model representing a student profile and academic settings.
 */
data class User(
    var uid: String = "",
    var email: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var isRosebankStudent: Boolean = true,
    var campusLocation: String = "",
    var course: String = "",
    var yearOfStudy: Int = 0,
    var currentSemester: Int = 0,
    var enrolledModules: List<Map<String, String>> = emptyList(),
    var language: String = "en",
    var notificationsEnabled: Boolean = true,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)
