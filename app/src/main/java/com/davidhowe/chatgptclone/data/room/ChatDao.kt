package com.davidhowe.chatgptclone.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatDao {
    @Query("SELECT * FROM chatEntities ORDER BY createdAt DESC")
    suspend fun getAll(): List<ChatEntity>

    @Query("SELECT * FROM chatEntities WHERE rowId = :id LIMIT 1")
    suspend fun getById(id: Int): ChatEntity?

    @Query("SELECT * FROM chatEntities WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUUID(uuid: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: ChatEntity): Long

    @Delete
    suspend fun delete(chat: ChatEntity)
}
