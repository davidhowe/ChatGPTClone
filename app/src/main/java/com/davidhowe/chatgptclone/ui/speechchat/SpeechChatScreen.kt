package com.davidhowe.chatgptclone.ui.speechchat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.davidhowe.chatgptclone.SpeechChatState


@Composable
fun SpeechChatScreen(
    viewModel: SpeechChatViewModel,
    navHostController: NavHostController,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SpeechChatScreenContent(
        chatState = uiState.chatState,
        volumeProvider = { viewModel.aiVolumeLevel.value }
    )
}

@Composable
fun SpeechChatScreenContent(
    chatState: SpeechChatState,
    volumeProvider: () -> Float,
) {

    LaunchedEffect(chatState) {

    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // Animation indicator
        ChatVisualiser(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            chatState = chatState,
            volumeProvider = volumeProvider,
        )
    }

}

@Preview
@Composable
fun SpeechChatScreenPreview() {
    SpeechChatScreenContent(SpeechChatState.aiResponding, { 0.5f })
}