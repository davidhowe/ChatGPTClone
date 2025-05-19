package com.davidhowe.chatgptclone.data.datasource

import com.davidhowe.chatgptclone.data.room.ChatDao
import com.davidhowe.chatgptclone.data.room.ChatEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatLocalDataSource @Inject constructor(private val chatDao: ChatDao) {

    suspend fun getChatList(): List<ChatEntity> {
        return chatDao.getAll()
    }

    suspend fun getChatById(id: Int): ChatEntity? {
        return chatDao.getById(id)
    }

    suspend fun getChatByUUID(uuid: String): ChatEntity? {
        return chatDao.getByUUID(uuid)
    }

    suspend fun insertChat(chat: ChatEntity): Long {
        return chatDao.insert(chat)
    }

    suspend fun deleteChat(chat: ChatEntity) {
        chatDao.delete(chat)
    }

    suspend fun updateChat(uuid: String, title: String, summaryContent: String) {
        val chat = getChatByUUID(uuid)
        chat?.let {
            it.title = title
            it.summaryContent = summaryContent
            it.lastModifiedAt = System.currentTimeMillis()
            insertChat(it)
        }
    }
}
