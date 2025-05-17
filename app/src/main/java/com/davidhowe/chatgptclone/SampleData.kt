package com.davidhowe.chatgptclone

import com.davidhowe.chatgptclone.data.local.ChatSummary

object SampleData {
    val sampleDataNavListChats = listOf(
        ChatSummary.build(
            "First Chat",
            "This is the first chat"
        ),
        ChatSummary.build(
            "Second Chat",
            "This is the second chat"
        ),
        ChatSummary.build(
            "Third Chat",
            "This is the third chat"
        ),
    )
}