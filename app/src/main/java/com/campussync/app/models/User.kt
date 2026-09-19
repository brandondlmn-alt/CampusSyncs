package com.campussync.app.models

/**
 * User data class representing a student in the system.
 * Contains an empty constructor required for Firebase Firestore deserialization.
 */
data class User(
    var uid: String = "",
    var email: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var institution: String = "",
    var course: String = "",
    var yearOfStudy: String = "",
    var language: String = "English",
    var notificationsEnabled: Boolean = true,
    var createdAt: Long = System.currentTimeMillis()
)
