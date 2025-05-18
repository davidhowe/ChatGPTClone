package com.davidhowe.chatgptclone.ui.textChat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhowe.chatgptclone.data.datasource.ChatLocalDataSource
import com.davidhowe.chatgptclone.data.datasource.MessageLocalDataSource
import com.davidhowe.chatgptclone.data.local.ChatDomain
import com.davidhowe.chatgptclone.data.local.ChatSummaryDomain
import com.davidhowe.chatgptclone.data.room.ChatEntity
import com.davidhowe.chatgptclone.di.IoDispatcher
import com.davidhowe.chatgptclone.util.ResourceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TextChatUiState(
    val title: String = "",
    val activeChat: ChatDomain? = null
)

data class TextChatNavDrawerUiState(
    val searchText: String = "",
    val summaryList: List<ChatSummaryDomain> = emptyList(),
)

@HiltViewModel
class TextChatViewModel @Inject constructor(
    private val chatLocalDataSource: ChatLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource,
    private val resourceUtil: ResourceUtil,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiStateMain = MutableStateFlow(TextChatUiState())
    val uiStateMain: StateFlow<TextChatUiState> = _uiStateMain.asStateFlow()

    private val _uiStateNav = MutableStateFlow(TextChatNavDrawerUiState())
    val uiStateNav: StateFlow<TextChatNavDrawerUiState> = _uiStateNav.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            chatLocalDataSource.insertChat(
                ChatEntity(
                    title = "New Title (${System.currentTimeMillis()})",
                    summaryContent = "Summary",
                )
            )

            delay(500)

            chatLocalDataSource.getChatList().forEach {
                Timber.d("Chat title: ${it.title}")
            }
        }
    }


}