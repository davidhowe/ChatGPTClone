package com.davidhowe.chatgptclone.ui.textchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            summaryList = emptyList()
        )
    )
    val uiStateNav: StateFlow<TextChatNavDrawerUiState> = _uiStateNav.asStateFlow()

    private val _processedMessage = MutableStateFlow<String>("")
    val processedMessage = _processedMessage.asStateFlow()

    private var activeChat: Chat? = null
    private var activeChatUUID: String? = null
    private var stopActiveProcesses = false

    init {
        viewModelScope.launch(ioDispatcher) {
            delay(500)
            refreshChatHistories()
        }
    }

    fun onNewChatClicked() {
        Timber.d("onNewChatClicked")
        viewModelScope.launch(ioDispatcher) {
            stopActiveProcesses = true
            activeChatUUID = null
            activeChat = null
            _processedMessage.value = ""
            updateState {
                copy(
                    title = "",
                    messages = emptyList(),
                    isProcessing = false
                )
            }
            activeChat = textChatUseCases.startChat()
            delay(200)
            stopActiveProcesses = false
            if (activeChat == null) {
                // todo error message
            }
        }
    }

    fun onChatClicked(chat: ChatSummaryDomain) {
        Timber.d("onChatClicked")
        viewModelScope.launch(ioDispatcher) {
            stopActiveProcesses = true
            _processedMessage.value = ""
            activeChatUUID = chat.uuid
            updateState {
                copy(
                    title = chat.title,
                    messages = textChatUseCases.getMessagesForChat(chat.uuid),
                    isProcessing = true
                )
            }
            activeChat = textChatUseCases.startChat(chat.uuid)
            delay(500)
            stopActiveProcesses = false
            if (activeChat != null) {
                updateState {
                    copy(
                        isProcessing = false
                    )
                }
            } else {
                // todo error message
            }
        }
    }

    fun onSearchTextChanged(text: String) {
        refreshChatHistories(text)
    }

    fun onClickSend(text: String) {
        Timber.d("onClickSend, text: $text")
        viewModelScope.launch(ioDispatcher) {
            // Simulate user send
            delay(500) // Give keyboard time to close

            val newUserMessage = ChatMessageDomain(
                isFromUser = true,
                content = text,
            )

            updateState {
                copy(
                    isProcessing = true,
                    messages = messages + newUserMessage,
                )
            }

            if (activeChatUUID == null) {
                activeChatUUID = textChatUseCases.storeNewChat()
                if (activeChatUUID == null) {
                    // todo handle error storing chat
                    return@launch
                }
                val title = textChatUseCases.generateChatTitle(
                    chatUUID = activeChatUUID!!,
                    firstMessage = text
                )
                updateState {
                    copy(
                        title = title,
                    )
                }
                refreshChatHistories()
                activeChat = textChatUseCases.startChat()
            }

            if (activeChat == null || activeChatUUID == null) {
                // todo handle error activating chat
                return@launch
            }

            textChatUseCases.storeNewMessage(
                chatUUID = activeChatUUID!!,
                message = newUserMessage,
            )

            _processedMessage.value = ""
            try {
                activeChat?.sendMessageStream(text)?.collect { chunk ->
                    var words = (chunk.text ?: "").split(" ")
                    words.forEach { word ->
                        if (stopActiveProcesses)
                            return@collect
                        delay(70L) // 70ms per word
                        _processedMessage.value += if (_processedMessage.value.isEmpty()) word else " $word"
                    }
                }
            } finally {
                if (!stopActiveProcesses) {
                    if (_processedMessage.value.isNotBlank()) {
                        val newMessage = ChatMessageDomain(
                            isFromUser = false,
                            content = _processedMessage.value,
                        )
                        if (activeChatUUID == null) {
                            //todo show error
                            return@launch
                        }
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
    }

    private fun refreshChatHistories(textFilter: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            val result = textChatUseCases.getChatSummaryHistory(textFilter)
            _uiStateNav.value = _uiStateNav.value.copy(
                summaryList = result,
            )
        }
    }

    private inline fun updateState(update: TextChatUiState.() -> TextChatUiState) {
        _uiStateMain.value = _uiStateMain.value.update()
    }
}