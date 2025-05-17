package com.davidhowe.chatgptclone.ui.speechChat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.davidhowe.chatgptclone.ui.textChat.TextChatViewModel


@Composable
fun SpeechChatScreen(
    viewModel: TextChatViewModel,
    navHostController: NavHostController,
) {
    SpeechChatScreenContent()
}

@Composable
fun SpeechChatScreenContent() {

}

@Preview
@Composable
fun SpeechChatScreenPreview() {
    SpeechChatScreenContent()
}