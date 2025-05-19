package com.davidhowe.chatgptclone.ui.textchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.davidhowe.chatgptclone.R
import com.davidhowe.chatgptclone.data.local.ChatMessageDomain
import com.davidhowe.chatgptclone.data.local.ChatSummaryDomain
import com.davidhowe.chatgptclone.util.StringFunction
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun TextChatScreen(
    viewModel: TextChatViewModel,
    navHostController: NavHostController,
) {
    val uiStateMain by viewModel.uiStateMain.collectAsStateWithLifecycle()
    val uiStateNav by viewModel.uiStateNav.collectAsStateWithLifecycle()
    val uiStateProcessedMessage by viewModel.processedMessage.collectAsStateWithLifecycle()

    TextChatScreenContent(
        titleText = uiStateMain.title,
        messages = uiStateMain.messages,
        isProcessing = uiStateMain.isProcessing,
        processedMessage = uiStateProcessedMessage,
        navChatList = uiStateNav.summaryList,
        onClickSend = viewModel::onClickSend
    )
}

@Composable
fun TextChatScreenContent(
    titleText: String,
    messages: List<ChatMessageDomain>,
    isProcessing: Boolean = false,
    processedMessage: String,
    navChatList: List<ChatSummaryDomain>,
    onClickSend: StringFunction
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }
    var messageScrollToggle by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isProcessing, messageScrollToggle) {
        Timber.d("Composing LaunchedEffect")
        delay(50) // debounce to avoid scroll spam
        if (messages.isNotEmpty()) {
            if (isProcessing) {
                listState.scrollToItem(
                    index = messages.lastIndex + 1,
                    scrollOffset = Int.MAX_VALUE
                )
            } else {
                listState.animateScrollToItem(
                    index = messages.lastIndex,
                    scrollOffset = Int.MAX_VALUE
                )
            }
        }
    }

    TextChatNavDrawer(
        title = titleText,
        onNewChatClicked = {
            Timber.d("onNewChatClicked")
            // todo handle new chat requested
        },
        onChatClicked = {
            Timber.d("onChatClicked, id: ${it.uuid}")
            // todo handle chat click
        },
        onSearchTextChanged = {
            Timber.d("onSearchTextChanged, text: $it")
            // todo handle search text change
        },
        chatList = navChatList,
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (messages.isEmpty()) {
                    Text(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        text = stringResource(R.string.chat_empty_message),
                        textAlign = TextAlign.Center
                    )
                } else {

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp),
                        state = listState,
                    ) {
                        items(messages.size) { index ->
                            MessageBubble(
                                message = messages[index].content,
                                isFromUser = messages[index].isFromUser,
                                isProcessing = false
                            )
                        }

                        if (isProcessing) {
                            if (processedMessage.isEmpty()) {
                                item(key = "shimmering-bubble") {
                                    ShimmeringThinkingBubble()
                                }
                            } else {
                                item(key = "processing-message") {
                                    MessageBubble(
                                        message = processedMessage,
                                        isFromUser = false,
                                        isProcessing = true,
                                    )
                                }
                            }
                        }
                    }
                }

                TextChatInputBar(
                    modifier = Modifier,
                    inputText = inputText,
                    onTextChange = {
                        inputText = it
                        messageScrollToggle = !messageScrollToggle
                    },
                    onSendClick = {
                        messageScrollToggle = !messageScrollToggle
                        if (inputText.isNotBlank() && !isProcessing) {
                            onClickSend.invoke(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    onSpeechClick = {},
                )
            }
        }
    )
}