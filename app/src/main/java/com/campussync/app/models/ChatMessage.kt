package com.campussync.app.models

/**
 * Data class representing a message in the AI Chatbot.
 */
data class ChatMessage(
    var id: String = "",
    var content: String = "",
    var isUser: Boolean = false,
    var timestamp: Long = System.currentTimeMillis(),
    var studentId: String = ""
)