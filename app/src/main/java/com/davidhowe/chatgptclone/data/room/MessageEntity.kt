package com.davidhowe.chatgptclone.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "messageEntities",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["chatUUID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,
    @ColumnInfo(index = true) val chatUUID: String, // NB To link on uuid for larger system considerations
    val uuid: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val isFromUser: Boolean,
    val content: String,
)
