package com.davidhowe.chatgptclone.data.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import com.davidhowe.chatgptclone.data.preferences.GptClonePreferences.UserPreferenceKeys.ACTIVE_CHAT_UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

//TODO Move to DataStore
@Singleton
class GptClonePreferences @Inject constructor(
    private val sharedPrefs: SharedPreferences,
) {

    suspend fun getActiveChatUUID(): String {
        return getEntry(ACTIVE_CHAT_UUID, "")
    }

    suspend fun setActiveChatUUID(uuid: String) {
        return setEntry(uuid, ACTIVE_CHAT_UUID)
    }

    fun clearAllData() {
        sharedPrefs.edit { clear() }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> setEntry(value: T, key: String) {
        withContext(Dispatchers.IO) {
            when (value) {
                is String -> sharedPrefs.edit { putString(key, value) }
                is Int -> sharedPrefs.edit { putInt(key, value) }
                else -> throw IllegalArgumentException("Unsupported type thus far: ${value}")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> getEntry(key: String, defaultValue: T): T {
        return withContext(Dispatchers.IO) {
            when (defaultValue) {
                is String -> sharedPrefs.getString(key, defaultValue) as T
                is Int -> sharedPrefs.getInt(key, defaultValue) as T
                else -> throw IllegalArgumentException("Unsupported type thus far: ${defaultValue}")
            }
        }
    }

    object UserPreferenceKeys {
        const val ACTIVE_CHAT_UUID = "ACTIVE_CHAT_UUID"
    }
}