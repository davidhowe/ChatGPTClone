package com.davidhowe.chatgptclone.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM messageEntities")
    suspend fun getAll(): List<MessageEntity>

    @Query("SELECT * FROM messageEntities WHERE rowId = :id LIMIT 1")
    suspend fun getById(id: Int): MessageEntity?

    @Query("SELECT * FROM messageEntities WHERE chatUUID = :uuid")
    suspend fun getMessagesByChatId(uuid: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messageEntities WHERE chatUUID = :uuid")
    suspend fun deleteMessagesByChatId(uuid: Int)
}