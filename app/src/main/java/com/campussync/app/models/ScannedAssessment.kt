package com.campussync.app.models

/**
 * Transient model used for the review step in the Assessment Scanner.
 */
data class ScannedAssessment(
    var programmeCode: String = "",    // NEW — will be filtered before saving
    var moduleCode: String = "",
    var moduleName: String = "",
    var assessmentType: String = "",
    var submissionMethod: String = "",
    var requiresTurnitin: Boolean = false,
    var dueDate: String = "",
    var dueTime: String = ""
)
