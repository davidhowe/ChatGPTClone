package com.davidhowe.chatgptclone.ui.speechchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhowe.chatgptclone.SpeechChatState
import com.davidhowe.chatgptclone.di.IoDispatcher
import com.davidhowe.chatgptclone.util.ResourceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

data class SpeechChatUiState(
    val chatState: SpeechChatState = SpeechChatState.aiResponding,
)

@HiltViewModel
class SpeechChatViewModel @Inject constructor(
    private val resourceUtil: ResourceUtil,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeechChatUiState())
    val uiState: StateFlow<SpeechChatUiState> = _uiState.asStateFlow()

    private val _aiVolumeLevel = MutableStateFlow(0f) // Seperate from uiState so we dont trigger recomposition
    val aiVolumeLevel: StateFlow<Float> = _aiVolumeLevel

    init {
        _uiState.value = SpeechChatUiState()
        viewModelScope.launch(ioDispatcher) {
            repeat(50) {
                _uiState.value = _uiState.value.copy(
                    chatState = SpeechChatState.userTalking
                )
                delay(2000L)
                _uiState.value = _uiState.value.copy(
                    chatState = SpeechChatState.aiResponding
                )
                startSimulatedVolumeLevels()
                _uiState.value = _uiState.value.copy(
                    chatState = SpeechChatState.idle
                )
                delay(2000L)
                _uiState.value = _uiState.value.copy(
                    chatState = SpeechChatState.userTalking
                )
            }
        }
    }

    suspend fun startSimulatedVolumeLevels() {
        val random = Random(System.currentTimeMillis())

        repeat(1500) { i ->
            val voicePulse = abs(sin(i / 6f)) * Random.nextFloat()
            _aiVolumeLevel.value = voicePulse.coerceIn(0f, 1f)
            delay(30L) // ~33 FPS
        }
    }


}