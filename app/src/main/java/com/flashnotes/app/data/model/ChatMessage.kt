package com.flashnotes.app.data.model

/**
 * Represents a single message in the Chat tab conversation.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val role: Role,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role {
        USER, AI
    }
}
