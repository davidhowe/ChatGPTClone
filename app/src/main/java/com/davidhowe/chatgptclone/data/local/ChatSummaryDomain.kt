package com.davidhowe.chatgptclone.data.local

import com.davidhowe.chatgptclone.data.room.ChatEntity

data class ChatSummaryDomain(
    val uuid: String,
    val title: String,
    val content: String,
    val lastModifiedAt: Long,
) {

    companion object {
        fun build(chatDomain: ChatEntity): ChatSummaryDomain {
            return ChatSummaryDomain(
                uuid = chatDomain.uuid,
                title = chatDomain.title.trim(),
                content = chatDomain.summaryContent.trim(),
                lastModifiedAt = chatDomain.lastModifiedAt
            )
        }
    }
}