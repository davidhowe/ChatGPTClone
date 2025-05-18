package com.davidhowe.chatgptclone

import com.davidhowe.chatgptclone.data.local.ChatDomain
import com.davidhowe.chatgptclone.data.local.ChatSummaryDomain

object SampleData {
    val sampleDataNavListChats = listOf(
        ChatSummaryDomain.build(
            ChatDomain(
                uuid = "1",
                title = "First Chat",
                summary = "This is the first chat",
                messages = emptyList(),
                createdAt = 0L,
                lastModifiedAt = 0L
            )
        ),
        ChatSummaryDomain.build(
            ChatDomain(
                uuid = "2",
                title = "Second Chat",
                summary = "This is the second chat",
                messages = emptyList(),
                createdAt = 2L,
                lastModifiedAt = 2L
            )
        ),
        ChatSummaryDomain.build(
            ChatDomain(
                uuid = "3",
                title = "Third Chat",
                summary = "This is the third chat",
                messages = emptyList(),
                createdAt = 3L,
                lastModifiedAt = 3L
            )
        ),
    )
}