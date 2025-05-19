package com.davidhowe.chatgptclone.ui.textchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhowe.chatgptclone.SampleData.sampleDataNavListChats
import com.davidhowe.chatgptclone.data.local.ChatMessageDomain
import com.davidhowe.chatgptclone.data.local.ChatSummaryDomain
import com.davidhowe.chatgptclone.di.IoDispatcher
import com.davidhowe.chatgptclone.domain.usecase.TextChatUseCases
import com.davidhowe.chatgptclone.util.ResourceUtil
import com.google.firebase.vertexai.Chat
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
    val messages: List<ChatMessageDomain> = emptyList(),
    val isProcessing: Boolean = false,
)

data class TextChatNavDrawerUiState(
    val searchText: String = "",
    val summaryList: List<ChatSummaryDomain> = emptyList(),
)

@HiltViewModel
class TextChatViewModel @Inject constructor(
    private val textChatUseCases: TextChatUseCases,
    private val resourceUtil: ResourceUtil,

    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiStateMain = MutableStateFlow(TextChatUiState())
    val uiStateMain: StateFlow<TextChatUiState> = _uiStateMain.asStateFlow()

    private val _uiStateNav = MutableStateFlow(
        TextChatNavDrawerUiState(
            summaryList = sampleDataNavListChats
        )
    )
    val uiStateNav: StateFlow<TextChatNavDrawerUiState> = _uiStateNav.asStateFlow()

    private val _processedMessage = MutableStateFlow<String>("")
    val processedMessage = _processedMessage.asStateFlow()

    private var activeChat: Chat? = null
    private var activeChatUUID: String? = null

    init {
        viewModelScope.launch(ioDispatcher) {
            delay(500)
        }
    }

    fun onClickSend(text: String) {
        Timber.d("onClickSend, text: $text")
        viewModelScope.launch(ioDispatcher) {
            // Simulate user send
            delay(500) // Give keyboard time to close

            if (activeChatUUID == null) {
                activeChatUUID = textChatUseCases.storeNewChat()
                if (activeChatUUID == null) {
                    return@launch
                }
                activeChat = textChatUseCases.startChat()
            }

            if (activeChat == null) {
                // todo handle error activating chat
                return@launch
            }

            val newUserMessage = ChatMessageDomain(
                isFromUser = true,
                content = text,
            )

            textChatUseCases.storeNewMessage(
                chatUUID = activeChatUUID!!,
                message = newUserMessage,
            )

            updateState {
                copy(
                    isProcessing = true,
                    messages = messages + newUserMessage,
                )
            }

            _processedMessage.value = ""
            try {
                activeChat?.sendMessageStream(text)?.collect { chunk ->
                    val words = (chunk.text ?: "").split(" ")
                    words.forEach { word ->
                        delay(70L) // 70ms per word
                        _processedMessage.value += if (_processedMessage.value.isEmpty()) word else " $word"
                    }
                }
            } finally {

                if (_processedMessage.value.isNotBlank()) {
                    val newMessage = ChatMessageDomain(
                        isFromUser = false,
                        content = _processedMessage.value,
                    )
                    textChatUseCases.storeNewMessage(
                        chatUUID = activeChatUUID!!,
                        message = newMessage
                    )
                    updateState {
                        copy(
                            messages = messages + newMessage,
                            isProcessing = false,
                        )
                    }
                    _processedMessage.value = ""
                } else {
                    updateState {
                        copy(
                            messages = messages,
                            isProcessing = false,
                        )
                    }
                }
            }
        }
    }

    private inline fun updateState(update: TextChatUiState.() -> TextChatUiState) {
        _uiStateMain.value = _uiStateMain.value.update()
    }
}