package com.davidhowe.chatgptclone.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.davidhowe.chatgptclone.DatabaseName

/**
 * The Room database for this app
 */
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DatabaseName)
                .fallbackToDestructiveMigration(true)
                .build()
        }
    }
}
