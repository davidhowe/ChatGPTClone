package com.davidhowe.chatgptclone.data.local

import com.davidhowe.chatgptclone.data.room.MessageEntity
import com.google.firebase.vertexai.type.Content
import com.google.firebase.vertexai.type.content
import java.util.UUID

data class ChatMessageDomain(
    val uuid: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val isFromUser: Boolean,
    var content: String,
)

fun MessageEntity.toDomain(): ChatMessageDomain {
    return ChatMessageDomain(
        uuid = uuid,
        createdAt = createdAt,
        isFromUser = isFromUser,
        content = content,
    )
}

fun ChatMessageDomain.toAIMessageContent(): Content {
    return content(role = if (isFromUser) "user" else "model") { text(content) }
}