package com.campussync.app.models

/**
 * Data class representing a message in the AI Chatbot.
 * Updated with var properties and no-arg constructor for Firestore.
 */
data class ChatMessage(
    var content: String = "",
    var isUser: Boolean = false,
    var timestamp: Long = System.currentTimeMillis(),
    var studentId: String = ""
)
