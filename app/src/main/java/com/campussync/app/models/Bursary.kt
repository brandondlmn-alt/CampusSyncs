package com.campussync.app.models

/**
 * Data class representing a Bursary opportunity.
 * Contains a no-arg constructor required for Firebase Firestore deserialization.
 */
data class Bursary(
    var id: String = "",
    var name: String = "",
    var provider: String = "",
    var description: String = "",
    var closingDate: String = "",
    var applyUrl: String = ""
)
