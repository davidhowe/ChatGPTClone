package com.davidhowe.chatgptclone.ui.textchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhowe.chatgptclone.data.local.ChatMessageDomain
import com.davidhowe.chatgptclone.data.local.ChatSummaryDomain
import com.davidhowe.chatgptclone.data.preferences.GptClonePreferences
import com.davidhowe.chatgptclone.di.IoDispatcher
import com.davidhowe.chatgptclone.domain.usecase.ChatUseCases
import com.davidhowe.chatgptclone.util.ResourceUtil
import com.google.firebase.vertexai.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

sealed class TextChatEvent {
    object NavigateToSpeech : TextChatEvent()
    object NavigateBack : TextChatEvent()
    data class ShowToast(val message: String) : TextChatEvent()
}

@HiltViewModel
class TextChatViewModel @Inject constructor(
    private val chatUseCases: ChatUseCases,
    private val preferences: GptClonePreferences,
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

    private val _eventFlow = MutableSharedFlow<TextChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _processedMessage = MutableStateFlow<String>("")
    val processedMessage = _processedMessage.asStateFlow()

    private var activeChat: Chat? = null
    private var activeChatUUID: String? = null

    private var streamJob: Job? = null

    init {
        viewModelScope.launch(ioDispatcher) {
            delay(500)
            refreshChatHistories()
        }
    }

    override fun onCleared() {
        Timber.d("onCleared")
        super.onCleared()
        streamJob?.cancel()
    }

    fun onNewChatClicked() {
        Timber.d("onNewChatClicked")
        viewModelScope.launch(ioDispatcher) {
            streamJob?.cancel()
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
            activeChat = chatUseCases.startChat()
            if (activeChat == null) {
                // todo error message
            }
        }
    }

    fun onChatClicked(chat: ChatSummaryDomain) {
        Timber.d("onChatClicked")
        viewModelScope.launch(ioDispatcher) {
            streamJob?.cancel()
            _processedMessage.value = ""
            activeChatUUID = chat.uuid
            updateState {
                copy(
                    title = chat.title,
                    messages = chatUseCases.getMessagesForChat(chat.uuid),
                    isProcessing = true
                )
            }
            activeChat = chatUseCases.startChat(chat.uuid)
            delay(500)
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
        streamJob = viewModelScope.launch(ioDispatcher) {
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
                activeChatUUID = chatUseCases.storeNewChat()
                if (activeChatUUID == null) {
                    // todo handle error storing chat
                    return@launch
                }
                val title = chatUseCases.generateChatTitle(
                    chatUUID = activeChatUUID!!,
                    firstMessage = text
                )
                updateState {
                    copy(
                        title = title,
                    )
                }
                refreshChatHistories()
                activeChat = chatUseCases.startChat()
            }

            if (activeChat == null || activeChatUUID == null) {
                // todo handle error activating chat
                return@launch
            }

            chatUseCases.storeNewMessage(
                chatUUID = activeChatUUID!!,
                message = newUserMessage,
            )

            _processedMessage.value = ""
            try {
                activeChat?.sendMessageStream(text)?.collect { chunk ->
                    val words = (chunk.text ?: "").split(" ")
                    for (word in words) {
                        delay(70L)
                        _processedMessage.value += if (_processedMessage.value.isEmpty()) word else " $word"
                    }
                }
            } finally {
                if (_processedMessage.value.isNotBlank()) {
                    val newMessage = ChatMessageDomain(
                        isFromUser = false,
                        content = _processedMessage.value,
                    )
                    if (activeChatUUID == null) {
                        //todo show error
                        return@launch
                    }
                    chatUseCases.storeNewMessage(
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

    fun onSpeechClick() {
        Timber.d("onSpeechClick")
        viewModelScope.launch(ioDispatcher) {
            streamJob?.cancel()
            activeChatUUID?.let {
                preferences.setActiveChatUUID(it)
            }
            _eventFlow.emit(TextChatEvent.NavigateToSpeech)
        }
    }

    private fun refreshChatHistories(textFilter: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            val result = chatUseCases.getChatSummaryHistory(textFilter)
            _uiStateNav.value = _uiStateNav.value.copy(
                summaryList = result,
            )
        }
    }

    private inline fun updateState(update: TextChatUiState.() -> TextChatUiState) {
        _uiStateMain.value = _uiStateMain.value.update()
    }
}