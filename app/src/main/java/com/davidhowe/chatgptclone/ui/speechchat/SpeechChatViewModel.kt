package com.davidhowe.chatgptclone.ui.speechchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhowe.chatgptclone.SpeechChatState
import com.davidhowe.chatgptclone.data.preferences.GptClonePreferences
import com.davidhowe.chatgptclone.di.IoDispatcher
import com.davidhowe.chatgptclone.domain.usecase.ChatUseCases
import com.davidhowe.chatgptclone.domain.usecase.SpeechAudioUseCases
import com.davidhowe.chatgptclone.util.AudioPlaybackCallback
import com.davidhowe.chatgptclone.util.AudioPlaybackUtil
import com.davidhowe.chatgptclone.util.AudioRecorderCallback
import com.davidhowe.chatgptclone.util.AudioRecorderUtil
import com.davidhowe.chatgptclone.util.ResourceUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.shouldShowRationale
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
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class SpeechChatUiState(
    val chatState: SpeechChatState = SpeechChatState.aiResponding,
)

sealed class SpeechChatEvent {
    object RequestAudioRecordPermission : SpeechChatEvent()
    object NavigateBack : SpeechChatEvent()
    data class ShowToast(val message: String) : SpeechChatEvent()
}

@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel
class SpeechChatViewModel @Inject constructor(
    private val resourceUtil: ResourceUtil,
    private val preferences: GptClonePreferences,
    private val chatUseCases: ChatUseCases,
    private val speechAudioUseCases: SpeechAudioUseCases,
    private val audioRecorderUtil: AudioRecorderUtil,
    private val audioPlaybackUtil: AudioPlaybackUtil,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeechChatUiState())
    val uiState: StateFlow<SpeechChatUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SpeechChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _voiceLevel =
        MutableStateFlow(0f) // Seperate from uiState so we dont trigger unnecessary recomposition
    val voiceLevel: StateFlow<Float> = _voiceLevel

    private var activeChat: Chat? = null
    private var activeChatUUID: String? = null
    private var hasAudioRecordPermission = false

    private var streamJob: Job? = null

    init {
        viewModelScope.launch(ioDispatcher) {
            activeChatUUID = preferences.getActiveChatUUID().ifBlank { null }

            updateState {
                copy(
                    chatState = SpeechChatState.idle,
                )
            }

            activeChat = if (activeChatUUID.isNullOrBlank()) {
                chatUseCases.startChat()
            } else {
                chatUseCases.startChat(activeChatUUID!!)
            }

            if (activeChat == null) {
                // todo show user error
            } else {
                setupCallBacks()
            }
        }
    }

    fun onNoAudioRecordPermission() {
        hasAudioRecordPermission = false
        viewModelScope.launch(ioDispatcher) {
            delay(1000)
            _eventFlow.emit(SpeechChatEvent.RequestAudioRecordPermission)
        }
    }

    fun onDetectAudioRecordPermission() {
        hasAudioRecordPermission = true
        audioRecorderUtil.startRecording()
    }

    fun onPermissionRequestResult(
        permissionName: String,
        isGranted: Boolean,
        permissionStatus: PermissionStatus
    ) {
        Timber.d(
            "onPermissionRequestResult: $permissionName, isGranted: $isGranted, shouldShowRationale: ${permissionStatus.shouldShowRationale}"
        )
        if (isGranted == false && permissionStatus.shouldShowRationale == false) {
            // We cant continue here anymore
            viewModelScope.launch {
                _eventFlow.emit(SpeechChatEvent.ShowToast("Enable $permissionName permission in settings"))
                _eventFlow.emit(SpeechChatEvent.NavigateBack)
            }
        } else if (isGranted == false && permissionStatus.shouldShowRationale == true) {
            // TODO Show permission rationale
            viewModelScope.launch {
                _eventFlow.emit(SpeechChatEvent.RequestAudioRecordPermission)
            }
        } else if (isGranted == true) {
            viewModelScope.launch {
                _eventFlow.emit(SpeechChatEvent.ShowToast("Permission Granted!"))
            }
        }
    }

    private fun setupCallBacks() {
        audioPlaybackUtil.setCallback(
            object : AudioPlaybackCallback {
                override fun onAmplitudeLevel(level: Float) {
                    // Update eq level
                    _voiceLevel.value = level
                }

                override fun onPlaybackEnded() {
                    Timber.d("onPlaybackEnded")
                    updateState {
                        copy(
                            chatState = SpeechChatState.idle,
                        )
                    }
                    audioRecorderUtil.startRecording()
                }
            }
        )

        audioRecorderUtil.setCallback(object : AudioRecorderCallback {
            override fun onAmplitudeLevel(level: Float) {
                // Update yarn level
                _voiceLevel.value = level
            }

            override fun onVoiceEnded() {
                Timber.d("onRecording onVoiceEnded")
                // Auto-stop recording and trigger Gemini
                viewModelScope.launch {
                    audioRecorderUtil.stopRecording()
                    updateState {
                        copy(
                            chatState = SpeechChatState.idle,
                        )
                    }
                }
            }

            override fun onSilenceDetected() {
                Timber.d("onRecording SilenceDetected")
                audioRecorderUtil.stopRecording()
                updateState {
                    copy(
                        chatState = SpeechChatState.idle,
                    )
                }
                audioRecorderUtil.startRecording()
            }

            override fun onRecordingStarted() {
                Timber.d("onRecording Started")
                updateState {
                    copy(
                        chatState = SpeechChatState.userTalking,
                    )
                }
            }

            override fun onRecordingStopped(recordingByteArray: ByteArray?) {
                Timber.d("onRecording Stopped")
                updateState {
                    copy(
                        chatState = SpeechChatState.idle,
                    )
                }
                if (activeChat != null && recordingByteArray != null) {
                    viewModelScope.launch(ioDispatcher) {
                        // todo save sent message to db (Need text form)
                        val response = chatUseCases.sendAudioMessage(
                            chat = activeChat!!,
                            audioData = recordingByteArray
                        )
                        Timber.d("AI response = ${response?.text}")
                        if (response?.text.isNullOrBlank()) {
                            _eventFlow.emit(SpeechChatEvent.ShowToast("Try again"))
                            audioRecorderUtil.startRecording()
                        } else {
                            val speechByteArray =
                                speechAudioUseCases.synthesizeSpeech(response.text ?: "")
                            if (speechByteArray == null || speechByteArray.isEmpty()) {
                                Timber.w("Speech result failed")
                                _eventFlow.emit(SpeechChatEvent.ShowToast("Try again"))
                                audioRecorderUtil.startRecording()
                            } else {
                                Timber.d("Speech result success")
                                updateState {
                                    copy(
                                        chatState = SpeechChatState.aiResponding,
                                    )
                                }
                                audioPlaybackUtil.playAudio(
                                    audioData = speechByteArray,
                                )
                            }
                        }
                    }
                } else {
                    //TODO show error message
                }
            }
        })
    }

    private inline fun updateState(update: SpeechChatUiState.() -> SpeechChatUiState) {
        _uiState.value = _uiState.value.update()
    }


    /**
     * Simulates human-like volume levels for AI speech with a repeating 9-second cycle:
     * 3s silence, 3s medium noise, 3s high noise.
     * @param _aiVolumeLevel StateFlow to update with volume levels (0.0 to 1.0).
     * @param durationMs Total duration in milliseconds (null for continuous).
     * @param seed Random seed for reproducibility (default: current time).
     */
    suspend fun startSimulatedVolumeLevels(
        _aiVolumeLevel: MutableStateFlow<Float>,
        durationMs: Long? = null,
        seed: Long = System.currentTimeMillis()
    ) {
        val random = Random(seed)
        var time = 0f // Time in seconds
        val frameDurationMs = 50L // 20 FPS
        val maxIterations = durationMs?.div(frameDurationMs)?.toInt() ?: Int.MAX_VALUE
        val cycleDuration = 9.0f // 9s cycle: 3s silence, 3s medium, 3s high
        val phaseDuration = 3.0f // 3s per phase
        val transitionDuration = 0.2f // 200ms transition between phases

        var iteration = 0
        while (iteration < maxIterations) {
            val cycleTime = time % cycleDuration
            val phase = when {
                cycleTime < phaseDuration -> 0 // Silence (0-3s)
                cycleTime < 2 * phaseDuration -> 1 // Medium noise (3-6s)
                else -> 2 // High noise (6-9s)
            }

            // Base volume and scale for each phase
            val (baseVolume, volumeScale) = when (phase) {
                0 -> 0.02f to 0.03f // Silence: ~0.0-0.05
                1 -> 0.4f to 0.2f // Medium: ~0.4-0.6
                2 -> 0.8f to 0.2f // High: ~0.8-1.0
                else -> 0f to 0f
            }

            // Smooth phase transitions
            val phaseProgress = (cycleTime % phaseDuration) / phaseDuration
            val isTransitioning = phaseProgress > (1f - transitionDuration / phaseDuration)
            val nextPhaseVolume = when {
                cycleTime < phaseDuration -> 0.4f // Silence to medium
                cycleTime < 2 * phaseDuration -> 0.8f // Medium to high
                else -> 0.02f // High to silence
            }
            val transitionFactor = if (isTransitioning) {
                ((phaseProgress - (1f - transitionDuration / phaseDuration)) * phaseDuration / transitionDuration).coerceIn(
                    0f,
                    1f
                )
            } else {
                0f
            }
            val effectiveBaseVolume =
                baseVolume * (1f - transitionFactor) + nextPhaseVolume * transitionFactor

            // Speech-like dynamics for noise phases
            var voicePulse = effectiveBaseVolume
            if (phase > 0 || transitionFactor > 0f) {
                // Phrase wave (~0.7Hz, period ~1.4s)
                val phraseWave = (sin(2 * PI * 0.7 * time) * 0.5 + 0.5).toFloat() * 0.3f

                // Syllable wave (~4Hz, period ~250ms)
                val syllableWave = (sin(2 * PI * 4 * time) * 0.5 + 0.5).toFloat() * 0.3f

                // Burst envelope (~800ms bursts every ~2s)
                val burstPeriod = 2.0f
                val burstTime = (time % burstPeriod) / burstPeriod
                val burstEnvelope = exp(-((burstTime - 0.5f).pow(2) / 0.1f)) * 0.4f

                // Subtle random variation (±5%)
                val randomVariation = (random.nextFloat() * 0.1f - 0.05f) * 0.2f

                // Combine and scale to phase range
                voicePulse =
                    (effectiveBaseVolume + (phraseWave + syllableWave + burstEnvelope + randomVariation) * volumeScale)
                        .coerceIn(0f, 1f)
            }

            // Smooth with previous value
            val previousVolume = _aiVolumeLevel.value
            _aiVolumeLevel.value = (previousVolume * 0.7f + voicePulse * 0.3f).coerceIn(0f, 1f)

            time += frameDurationMs / 1000f
            delay(frameDurationMs)
            iteration++
        }
    }


}