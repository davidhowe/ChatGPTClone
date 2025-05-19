package com.davidhowe.chatgptclone

const val DatabaseName = "chatgpt-clone-mobile-db-1"
const val AIConfigModel = "gemini-2.0-flash"
const val AIConfigTemp = 0.6f
const val AIPromptGenerateTitle =
    "Generate a short, descriptive chat title based on the following opening text. The title must be plain text only and no longer than 20 characters. Do not include punctuation, quotes, or formatting. Only return the title.\n" +
            "Opening text: "

enum class SpeechChatState {
    idle, userTalking, aiResponding
}