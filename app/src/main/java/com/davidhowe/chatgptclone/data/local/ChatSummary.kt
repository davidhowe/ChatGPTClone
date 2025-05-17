package com.davidhowe.chatgptclone.data.local

import java.util.UUID

data class ChatSummary(
    val id: String,
    val title: String,
    val body: String
) {
    companion object {
        fun build(title: String, body: String): ChatSummary {
            return ChatSummary(
                id = UUID.randomUUID().toString(),
                title = title,
                body = body
            )
        }
    }
}