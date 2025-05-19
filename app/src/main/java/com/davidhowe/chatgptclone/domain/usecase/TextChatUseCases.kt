package com.davidhowe.chatgptclone.domain.usecase

import com.davidhowe.chatgptclone.data.datasource.ChatLocalDataSource
import com.davidhowe.chatgptclone.data.datasource.MessageLocalDataSource
import com.davidhowe.chatgptclone.data.local.ChatMessageDomain
import com.davidhowe.chatgptclone.data.local.toAIMessageContent
import com.davidhowe.chatgptclone.data.local.toDomain
import com.davidhowe.chatgptclone.data.room.ChatEntity
import com.davidhowe.chatgptclone.data.room.MessageEntity
import com.google.firebase.vertexai.Chat
import com.google.firebase.vertexai.GenerativeModel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextChatUseCases @Inject constructor(
    // TODO: Refactor this into individual use cases as app expands
    private val generativeModel: GenerativeModel,
    private val chatLocalDataSource: ChatLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource,
) {

    suspend fun startChat(existingChatUUID: String? = null): Chat? {
        return try {
            if (existingChatUUID.isNullOrBlank()) {
                generativeModel.startChat()
            } else {
                val messages = messageLocalDataSource.getMessagesForChat(existingChatUUID)
                messages.sortedBy { it.createdAt }
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


}