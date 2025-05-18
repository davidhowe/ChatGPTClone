package com.davidhowe.chatgptclone.data.local

data class ChatSummaryDomain(
    val uuid: String,
    val title: String,
    val content: String,
    val lastModifiedAt: Long,
) {

    companion object {
        fun build(chatDomain: ChatDomain): ChatSummaryDomain {
            return ChatSummaryDomain(
                uuid = chatDomain.uuid,
                title = chatDomain.title,
                content = chatDomain.summary,
                lastModifiedAt = chatDomain.lastModifiedAt
            )
        }
    }
}