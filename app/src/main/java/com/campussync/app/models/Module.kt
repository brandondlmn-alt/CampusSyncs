package com.campussync.app.models

/**
 * Model representing a university subject or module.
 */
data class Module(
    var id: String = "",
    var studentId: String = "",
    var code: String = "",
    var name: String = "",
    var targetClassesPerWeek: Int = 0
)
