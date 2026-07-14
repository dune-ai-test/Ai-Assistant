package com.midnight.assistant.data

enum class Role { USER, ASSISTANT, SYSTEM }

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GatewayModel(
    val id: String,
    val displayName: String,
    val contextWindow: Int? = null
)

sealed class GatewayResult<out T> {
    data class Success<T>(val value: T) : GatewayResult<T>()
    data class Failure(val message: String) : GatewayResult<Nothing>()
}
