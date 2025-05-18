package com.davidhowe.chatgptclone.data.local

import com.davidhowe.chatgptclone.data.room.ChatEntity
import com.davidhowe.chatgptclone.data.room.MessageEntity

data class ChatDomain(
    val uuid: String,
    val title: String,
    val summary: String,
    val messages: List<ChatMessageDomain>,
    val createdAt: Long,
    val lastModifiedAt: Long,
)

fun ChatEntity.toDomain(messages: List<MessageEntity>): ChatDomain {
    return ChatDomain(
        uuid = uuid,
        title = title,
        summary = summaryContent,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        messages = messages.map { it.toDomain() }
    )
}