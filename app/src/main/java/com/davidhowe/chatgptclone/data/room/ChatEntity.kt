package com.davidhowe.chatgptclone.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chatEntities")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var lastModifiedAt: Long = 0L,
    val uuid: String = UUID.randomUUID().toString(),
    var title: String = "",
    var summaryContent: String = "",
)
