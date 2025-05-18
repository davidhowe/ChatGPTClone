package com.davidhowe.chatgptclone

const val DatabaseName = "chatgpt-clone-mobile-db-1"

enum class ChatMessageStatus {
    SENDING, SENT, FAILED
}

enum class SpeechChatState {
    idle, userTalking, aiResponding
}