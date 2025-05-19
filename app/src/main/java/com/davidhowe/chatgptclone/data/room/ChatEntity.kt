package com.davidhowe.chatgptclone.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chatEntities", indices = [Index(value = ["uuid"], unique = true)])
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    var lastModifiedAt: Long = 0L,
    var title: String = "",
    var summaryContent: String = "",
)
