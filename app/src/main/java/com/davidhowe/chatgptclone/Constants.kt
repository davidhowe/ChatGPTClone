package com.davidhowe.chatgptclone

const val DatabaseName = "chatgpt-clone-mobile-db-1"
const val AIConfigModel = "gemini-2.0-flash"
const val AIConfigTemp = 0.6f

enum class SpeechChatState {
    idle, userTalking, aiResponding
}