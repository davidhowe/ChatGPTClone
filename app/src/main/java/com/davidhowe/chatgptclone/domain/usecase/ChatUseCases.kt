package com.davidhowe.chatgptclone.domain.usecase

import com.davidhowe.chatgptclone.AIPromptGenerateTitle
import com.davidhowe.chatgptclone.data.datasource.ChatLocalDataSource
import com.davidhowe.chatgptclone.data.datasource.MessageLocalDataSource
import com.davidhowe.chatgptclone.data.local.ChatMessageDomain
import com.davidhowe.chatgptclone.data.local.ChatSummaryDomain
import com.davidhowe.chatgptclone.data.local.toAIMessageContent
import com.davidhowe.chatgptclone.data.local.toDomain
import com.davidhowe.chatgptclone.data.room.ChatEntity
import com.davidhowe.chatgptclone.data.room.MessageEntity
import com.google.firebase.vertexai.Chat
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.content
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatUseCases @Inject constructor(
    // TODO: Refactor this into individual use cases as app expands
    private val generativeModel: GenerativeModel,
    private val chatLocalDataSource: ChatLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource,
) {

    suspend fun getMessagesForChat(chatUUID: String): List<ChatMessageDomain> {
        return try {
            val messages = messageLocalDataSource.getMessagesForChat(chatUUID)
            messages.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e("Error retrieving messages for chat $chatUUID: ${e.message}")
            emptyList()
        }
    }

    suspend fun startChat(existingChatUUID: String? = null): Chat? {
        return try {
            if (existingChatUUID.isNullOrBlank()) {
                generativeModel.startChat()
            } else {
                val messages = messageLocalDataSource.getMessagesForChat(existingChatUUID)
                generativeModel.startChat(
                    history = messages.map { it.toDomain().toAIMessageContent() },
                )
            }
        } catch (e: Exception) {
            Timber.e("Error starting chat: ${e.message}")
            null
        }
    }

    suspend fun storeNewMessage(
        chatUUID: String,
        message: ChatMessageDomain,
    ) {
        try {
            val messageEntity = messageLocalDataSource.insertMessage(
                MessageEntity(
                    chatUUID = chatUUID,
                    isFromUser = message.isFromUser,
                    content = message.content,
                )
            )
            Timber.d("Stored new message: $messageEntity")
        } catch (e: Exception) {
            Timber.e("Error storing new message: ${e.message}")
        }
    }

    suspend fun storeNewChat(): String? {
        return try {
            val entity = ChatEntity(
                lastModifiedAt = System.currentTimeMillis()
            )
            chatLocalDataSource.insertChat(entity)
            Timber.d("Stored new chat: ${entity.uuid}")
            entity.uuid
        } catch (e: Exception) {
            Timber.e("Error storing new chat: ${e.message}")
            null
        }
    }

    suspend fun getChatSummaryHistory(textFilter: String? = null): List<ChatSummaryDomain> {
        return try {
            val chatHistories =
                chatLocalDataSource.getChatList()
            var result =
                chatHistories.filter { it.title.isNotBlank() && it.summaryContent.isNotBlank() }
                    .map { ChatSummaryDomain.build(it) }
            if (textFilter.isNullOrBlank()) {
                result
            } else {
                result.filter {
                    it.title.contains(textFilter, true) || it.content.contains(
                        textFilter,
                        true
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e("Error retrieving chat histories: ${e.message}")
            emptyList()
        }
    }

    suspend fun generateChatTitle(chatUUID: String, firstMessage: String): String {
        val title = if (firstMessage.isNotBlank()) {
            val prompt = "$AIPromptGenerateTitle $firstMessage"
            val inputContent = content {
                text(prompt)
            }
            try {
                val response = generativeModel.generateContent(
                    inputContent
                )

                response.text?.trim() ?: ""
            } catch (e: Exception) {
                Timber.e("Error generating chat title: ${e.message}")
                ""
            }
        } else {
            Timber.e("Error generating chat title: first message is blank")
            ""
        }

        Timber.d("Generated title = $title")

        if (title.isNotBlank()) {
            chatLocalDataSource.updateChat(
                uuid = chatUUID,
                title = title,
                summaryContent = firstMessage,
            )
        }

        return title
    }


}