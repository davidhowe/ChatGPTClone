package com.davidhowe.chatgptclone.ui.speechchat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController


@Composable
fun SpeechChatScreen(
    viewModel: SpeechChatViewModel,
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