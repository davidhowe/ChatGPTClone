package com.davidhowe.chatgptclone.data.datasource

import com.davidhowe.chatgptclone.data.room.MessageDao
import com.davidhowe.chatgptclone.data.room.MessageEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageLocalDataSource @Inject constructor(
    private val messageDao: MessageDao
) {

    suspend fun getAllMessages(): List<MessageEntity> {
        return messageDao.getAll()
    }

    suspend fun getMessageById(id: Int): MessageEntity? {
        return messageDao.getById(id)
    }

    suspend fun getMessagesForChat(chatId: Int): List<MessageEntity> {
        return messageDao.getMessagesByChatId(chatId)
    }

    suspend fun insertMessage(message: MessageEntity): Long {
        return messageDao.insert(message)
    }

    suspend fun insertMessages(messages: List<MessageEntity>) {
        messageDao.insertAll(messages)
    }

    suspend fun deleteMessage(message: MessageEntity) {
        messageDao.delete(message)
    }

    suspend fun deleteMessagesByChatId(chatId: Int) {
        messageDao.deleteMessagesByChatId(chatId)
    }
}
