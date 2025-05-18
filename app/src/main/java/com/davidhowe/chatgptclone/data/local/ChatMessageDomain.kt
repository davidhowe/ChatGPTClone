package com.davidhowe.chatgptclone.data.local

import com.davidhowe.chatgptclone.ChatMessageStatus
import com.davidhowe.chatgptclone.data.room.MessageEntity

data class ChatMessageDomain(
    val uuid: String,
    val createdAt: Long,
    val isFromUser: Boolean,
    val status: ChatMessageStatus? = null,
    val content: String,
)

fun MessageEntity.toDomain(): ChatMessageDomain {
    return ChatMessageDomain(
        uuid = uuid,
        createdAt = createdAt,
        isFromUser = isFromUser,
        content = content,
    )
}