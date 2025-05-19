package com.davidhowe.chatgptclone.ui.speechchat

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.davidhowe.chatgptclone.SpeechChatState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus.Granted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeechChatScreen(
    viewModel: SpeechChatViewModel,
    navHostController: NavHostController,
) {

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingPermissionName =
        rememberSaveable { mutableStateOf("") }

    val audioRecordPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            debounceJob?.cancel() // Cancel any previous pending dispatch
            debounceJob = coroutineScope.launch {
                delay(500)
                viewModel.onPermissionRequestResult(
                    pendingPermissionName.value,
                    isGranted,
                    audioRecordPermissionState.status
                )
                pendingPermissionName.value = ""
            }
        }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SpeechChatEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                SpeechChatEvent.NavigateBack -> {
                    navHostController.navigateUp()
                }

                SpeechChatEvent.RequestAudioRecordPermission -> {
                    Timber.d("is SpeechChatEvent.RequestAudioRecordPermission")
                    pendingPermissionName.value = Manifest.permission.RECORD_AUDIO
                    requestPermissionLauncher.launch(pendingPermissionName.value)
                }
            }
        }
    }

    LaunchedEffect(audioRecordPermissionState.status) {
        if (audioRecordPermissionState.status != Granted && pendingPermissionName.value.isBlank()) {
            Timber.d("SpeechChatScreen LaunchedEffect: Requesting permission")
            viewModel.onNoAudioRecordPermission()
        }
    }

    SpeechChatScreenContent(
        chatState = uiState.chatState,
        volumeProvider = { viewModel.aiVolumeLevel.value }
    )

    BackHandler {
        Timber.d("On back pressed SpeechChatScreen")
        navHostController.navigateUp()
    }
}

@Composable
fun SpeechChatScreenContent(
    chatState: SpeechChatState,
    volumeProvider: () -> Float,
) {
    LaunchedEffect(chatState) {
        Timber.d("SpeechChatScreen LaunchedEffect: $chatState")
        when (chatState) {
            SpeechChatState.idle -> {
                // Do nothing
            }

            SpeechChatState.aiResponding -> {
                // Do nothing
            }

            SpeechChatState.userTalking -> {
                // Do nothing
            }
        }
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