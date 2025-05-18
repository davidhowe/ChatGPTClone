package com.davidhowe.chatgptclone.ui.textChat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.davidhowe.chatgptclone.SampleData.sampleDataNavListChats
import timber.log.Timber


@Composable
fun TextChatScreen(
    viewModel: TextChatViewModel,
    navHostController: NavHostController,
) {
    TextChatScreenContent()
}

@Composable
fun TextChatScreenContent(
) {
    // TODO Gen by AI
    val titleText = rememberSaveable { mutableStateOf("My New App") }

    val navChatList = remember {
        mutableStateOf(sampleDataNavListChats)
    }

    TextChatNavDrawer(
        title = titleText.value,
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
        chatList = navChatList.value,
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text("ChatGPT Clone")
            }
        }
    )
}

@Preview
@Composable
fun TextChatScreenPreview() {
    TextChatScreenContent()
}