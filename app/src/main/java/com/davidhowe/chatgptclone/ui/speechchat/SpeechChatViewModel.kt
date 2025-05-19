package com.davidhowe.chatgptclone.ui.speechchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidhowe.chatgptclone.SpeechChatState
import com.davidhowe.chatgptclone.data.datasource.ChatLocalDataSource
import com.davidhowe.chatgptclone.data.local.ChatMessageDomain
import com.davidhowe.chatgptclone.data.preferences.GptClonePreferences
import com.davidhowe.chatgptclone.di.IoDispatcher
import com.davidhowe.chatgptclone.domain.usecase.ChatUseCases
import com.davidhowe.chatgptclone.domain.usecase.SpeechAudioUseCases
import com.davidhowe.chatgptclone.util.AudioPlaybackCallback
import com.davidhowe.chatgptclone.util.AudioPlaybackUtil
import com.davidhowe.chatgptclone.util.AudioRecorderCallback
import com.davidhowe.chatgptclone.util.AudioRecorderUtil
import com.davidhowe.chatgptclone.util.DeviceUtil
import com.davidhowe.chatgptclone.util.ResourceUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.vertexai.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the speech chat screen.
 */
data class SpeechChatUiState(
    val chatState: SpeechChatState = SpeechChatState.idle,
)

/**
 * Events emitted to the UI.
 */
sealed class SpeechChatEvent {
    object RequestAudioRecordPermission : SpeechChatEvent()
    object NavigateBack : SpeechChatEvent()
    data class ShowToast(val message: String) : SpeechChatEvent()
}

/**
 * Internal processing steps to enforce chat flow.
 */
private enum class ProcessingStep {
    IDLE,
    RECORDING,
    PROCESSING,
    PLAYBACK
}

@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel
class SpeechChatViewModel @Inject constructor(
    private val resourceUtil: ResourceUtil,
    private val preferences: GptClonePreferences,
    private val chatLocalDataSource: ChatLocalDataSource,
    private val chatUseCases: ChatUseCases,
    private val speechAudioUseCases: SpeechAudioUseCases,
    private val audioRecorderUtil: AudioRecorderUtil,
    private val audioPlaybackUtil: AudioPlaybackUtil,
    private val deviceUtil: DeviceUtil,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeechChatUiState())
    val uiState: StateFlow<SpeechChatUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SpeechChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _voiceLevel = MutableStateFlow(0f)
    val voiceLevel: StateFlow<Float> = _voiceLevel.asStateFlow()

    private var activeChat: Chat? = null
    private var activeChatUUID: String? = null
    private var hasAudioRecordPermission = false
    private var chatJob: Job? = null
    private val actionChannel =
        Channel<Action>(Channel.UNLIMITED) // Changed to UNLIMITED for debugging
    private var wasSilenceDetected = false
    private var currentStep = ProcessingStep.IDLE // Track step for callbacks
    private var aiIsTalking = false

    /**
     * Internal actions to drive the chat flow.
     */
    private sealed class Action {
        object StartRecording : Action()
        data class ProcessRecording(val audioData: ByteArray) : Action()
        data class PlayResponse(val audioData: ByteArray) : Action()
        object PlaybackEnded : Action()
        object Stop : Action()
    }

    init {
        initializeChat()
        setupCallbacks()
        startChatFlow()
    }

    /**
     * Initializes the chat session.
     */
    private fun initializeChat() {

        viewModelScope.launch(ioDispatcher) {
            if (!deviceUtil.isConnectedInternet()) {
                showNoInternetToastError()
                _eventFlow.emit(SpeechChatEvent.NavigateBack)
            }

            activeChatUUID = preferences.getActiveChatUUID().ifBlank { null }
            activeChat = if (activeChatUUID.isNullOrBlank()) {
                chatUseCases.startChat()
            } else {
                chatUseCases.startChat(activeChatUUID!!)
            }

            if (activeChat == null) {
                _eventFlow.emit(SpeechChatEvent.ShowToast("Failed to start chat"))
                _eventFlow.emit(SpeechChatEvent.NavigateBack)
            } else {
                updateState { copy(chatState = SpeechChatState.idle) }
                actionChannel.send(Action.StartRecording)
            }
        }
    }

    /**
     * Sets up callbacks for audio recording and playback.
     */
    private fun setupCallbacks() {
        audioRecorderUtil.setCallback(object : AudioRecorderCallback {
            override fun onAmplitudeLevel(level: Float) {
                if (currentStep != ProcessingStep.RECORDING) {
                    return
                }
                _voiceLevel.value = level
            }

            override fun onVoiceEnded() {
                Timber.d("Recorder: Voice ended at ${System.currentTimeMillis()}")
                if (currentStep != ProcessingStep.IDLE && currentStep != ProcessingStep.RECORDING) {
                    Timber.d("Suppressing StartRecording during PLAYBACK or PROCESSING")
                    return
                }
                wasSilenceDetected = false
                viewModelScope.launch {
                    audioRecorderUtil.stopRecording()
                }
            }

            override fun onSilenceDetected() {
                Timber.d("Recorder: Silence detected at ${System.currentTimeMillis()}")
                if (currentStep != ProcessingStep.IDLE && currentStep != ProcessingStep.RECORDING) {
                    Timber.d("Suppressing StartRecording during PLAYBACK or PROCESSING")
                    return
                }
                wasSilenceDetected = true
                viewModelScope.launch {
                    audioRecorderUtil.stopRecording()
                    delay(100) // Debounce to ensure stop completes
                    actionChannel.send(Action.StartRecording)
                }
            }

            override fun onRecordingStarted() {
                Timber.d("Recorder: Started at ${System.currentTimeMillis()}")
                updateState { copy(chatState = SpeechChatState.userTalking) }
            }

            override fun onRecordingStopped(recordingByteArray: ByteArray?) {
                Timber.d("Recorder: Stopped, data size: ${recordingByteArray?.size ?: 0}, wasSilence: $wasSilenceDetected")
                if (currentStep != ProcessingStep.IDLE && currentStep != ProcessingStep.RECORDING) {
                    Timber.d("Suppressing recording during in valid states")
                    return
                }
                updateState { copy(chatState = SpeechChatState.idle) }
                if (!wasSilenceDetected && recordingByteArray != null && recordingByteArray.isNotEmpty()) {
                    viewModelScope.launch {
                        actionChannel.send(Action.ProcessRecording(recordingByteArray))
                    }
                } else {
                    Timber.d("Skipping silent or empty recording, restarting")
                    viewModelScope.launch {
                        delay(100) // Debounce to ensure state stability
                        actionChannel.send(Action.StartRecording)
                    }
                }
            }
        })

        audioPlaybackUtil.setCallback(object : AudioPlaybackCallback {
            override fun onAmplitudeLevel(level: Float) {
                _voiceLevel.value = level
            }

            override fun onPlaybackEnded() {
                Timber.d("Playback: Ended at ${System.currentTimeMillis()}")
                viewModelScope.launch {
                    actionChannel.send(Action.PlaybackEnded)
                }
            }
        })
    }

    /**
     * Manages the chat flow using a state machine.
     */
    private fun startChatFlow() {
        chatJob?.cancel()
        chatJob = viewModelScope.launch(ioDispatcher) {
            actionChannel.receiveAsFlow().collect { action ->
                Timber.d("currentStep: $currentStep action $action")
                when (currentStep) {
                    ProcessingStep.IDLE -> {
                        when (action) {
                            is Action.StartRecording -> {
                                if (hasAudioRecordPermission) {
                                    delay(1000)
                                    if (currentStep != ProcessingStep.IDLE) {
                                        Timber.d("Ignoring StartRecording, already in $currentStep")
                                        return@collect
                                    }
                                    currentStep = ProcessingStep.RECORDING
                                    audioRecorderUtil.startRecording()
                                } else {
                                    _eventFlow.emit(SpeechChatEvent.RequestAudioRecordPermission)
                                }
                            }

                            else -> Timber.w("Invalid action $action in IDLE")
                        }
                    }

                    ProcessingStep.RECORDING -> {
                        when (action) {
                            is Action.ProcessRecording -> {
                                currentStep = ProcessingStep.PROCESSING
                                processAudio(action.audioData)
                            }

                            is Action.StartRecording -> {
                                Timber.d("Ignoring StartRecording, already recording")
                            }

                            else -> Timber.w("Invalid action $action in RECORDING")
                        }
                    }

                    ProcessingStep.PROCESSING -> {
                        when (action) {
                            is Action.PlayResponse -> {
                                currentStep = ProcessingStep.PLAYBACK
                                // Clear queued StartRecording actions
                                while (actionChannel.tryReceive().isSuccess) {
                                    Timber.d("Cleared queued action during PLAYBACK entry")
                                }
                                aiIsTalking = true
                                updateState { copy(chatState = SpeechChatState.aiResponding) }
                                audioPlaybackUtil.playAudio(action.audioData)
                            }

                            is Action.StartRecording -> {
                                Timber.d("Deferring StartRecording until processing ends")
                            }

                            else -> Timber.w("Invalid action $action in PROCESSING")
                        }
                    }

                    ProcessingStep.PLAYBACK -> {
                        when (action) {
                            is Action.PlaybackEnded -> {
                                currentStep = ProcessingStep.IDLE
                                aiIsTalking = false
                                updateState { copy(chatState = SpeechChatState.idle) }
                                delay(100) // Debounce to ensure playback fully stops
                                actionChannel.send(Action.StartRecording)
                            }

                            is Action.StartRecording -> {
                                Timber.d("Ignoring StartRecording during playback")
                            }

                            else -> Timber.w("Invalid action $action in PLAYBACK")
                        }
                    }
                }

                if (action is Action.Stop) {
                    currentStep = ProcessingStep.IDLE
                    audioRecorderUtil.stopRecording()
                    audioPlaybackUtil.stopPlayback()
                    updateState { copy(chatState = SpeechChatState.idle) }
                }
            }
        }
    }

    /**
     * Processes recorded audio and generates AI response.
     */
    private suspend fun processAudio(audioData: ByteArray) {
        val userMessageTime = System.currentTimeMillis()
        try {
            Timber.d("Processing audio, size: ${audioData.size}")
            updateState { copy(chatState = SpeechChatState.idle) }
            val response = activeChat?.let { chat ->
                chatUseCases.sendAudioMessage(chat, audioData)
            }

            if (response?.text.isNullOrBlank()) {
                Timber.w("AI response empty")
                _eventFlow.emit(SpeechChatEvent.ShowToast("Failed to process audio, try again"))
                updateState { copy(chatState = SpeechChatState.idle) }
                actionChannel.send(Action.StartRecording)
            } else {
                // Save AI message to DB
                chatUseCases.storeNewMessage(
                    chatUUID = activeChatUUID!!,
                    message = ChatMessageDomain(
                        isFromUser = false,
                        content = response.text ?: "",
                    )
                )

                val speechByteArray = speechAudioUseCases.synthesizeSpeech(response.text ?: "")
                if (speechByteArray == null || speechByteArray.isEmpty()) {
                    Timber.w("TTS synthesis failed")
                    _eventFlow.emit(SpeechChatEvent.ShowToast("Failed to generate speech, try again"))
                    updateState { copy(chatState = SpeechChatState.idle) }
                    actionChannel.send(Action.StartRecording)
                } else {
                    Timber.d("TTS synthesis success, playing response")
                    actionChannel.send(Action.PlayResponse(speechByteArray))

                    // Attempt to transcribe and save the users message while ai is talking
                    viewModelScope.launch(ioDispatcher) {
                        val transcribeResult =
                            chatUseCases.runAITranscribeOnAudioByteArray(audioData, userMessageTime)
                        if (transcribeResult != null) {
                            chatUseCases.storeNewMessage(
                                chatUUID = activeChatUUID!!,
                                message = ChatMessageDomain(
                                    createdAt = transcribeResult.second,
                                    isFromUser = true,
                                    content = transcribeResult.first,
                                )
                            )
                        }

                        if (chatLocalDataSource.getChatByUUID(activeChatUUID!!)?.title.isNullOrBlank()) {
                            chatUseCases.generateChatTitle(
                                chatUUID = activeChatUUID!!,
                                firstMessage = response.text ?: "New chat!"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing audio")
            _eventFlow.emit(SpeechChatEvent.ShowToast("Error processing audio, try again"))
            updateState { copy(chatState = SpeechChatState.idle) }
            actionChannel.send(Action.StartRecording)
        }
    }

    /**
     * Handles permission denial.
     */
    fun onNoAudioRecordPermission() {
        hasAudioRecordPermission = false
        updateState { copy(chatState = SpeechChatState.idle) }
        viewModelScope.launch {
            _eventFlow.emit(SpeechChatEvent.RequestAudioRecordPermission)
        }
    }

    /**
     * Handles permission grant.
     */
    fun onDetectAudioRecordPermission() {
        hasAudioRecordPermission = true
        viewModelScope.launch {
            actionChannel.send(Action.StartRecording)
        }
    }

    /**
     * Handles permission request results.
     */
    fun onPermissionRequestResult(
        permissionName: String,
        isGranted: Boolean,
        permissionStatus: PermissionStatus
    ) {
        Timber.d("Permission $permissionName: granted=$isGranted, rationale=${permissionStatus.shouldShowRationale}")
        when {
            isGranted -> {
                hasAudioRecordPermission = true
                viewModelScope.launch {
                    _eventFlow.emit(SpeechChatEvent.ShowToast("Permission granted"))
                    actionChannel.send(Action.StartRecording)
                }
            }

            !isGranted && !permissionStatus.shouldShowRationale -> {
                viewModelScope.launch {
                    _eventFlow.emit(SpeechChatEvent.ShowToast("Enable $permissionName in settings"))
                    _eventFlow.emit(SpeechChatEvent.NavigateBack)
                }
            }

            !isGranted && permissionStatus.shouldShowRationale -> {
                viewModelScope.launch {
                    _eventFlow.emit(SpeechChatEvent.RequestAudioRecordPermission)
                }
            }
        }
    }

    fun showNoInternetToastError() {
        viewModelScope.launch(ioDispatcher) {
            _eventFlow.emit(SpeechChatEvent.ShowToast("No internet connection")) // todo add to string resources
        }
    }

    /**
     * Stops all operations and resets the chat.
     */
    fun stopChat() {
        viewModelScope.launch {
            actionChannel.send(Action.Stop)
        }
    }

    override fun onCleared() {
        audioRecorderUtil.release()
        audioPlaybackUtil.release()
        chatJob?.cancel()
        actionChannel.close()
        super.onCleared()
    }

    private inline fun updateState(update: SpeechChatUiState.() -> SpeechChatUiState) {
        if (aiIsTalking && _uiState.value.update().chatState != SpeechChatState.aiResponding) {
            Timber.d("Suppressing update during AI response")
            return
        }
        _uiState.value = _uiState.value.update()
        Timber.d("Updated chatState: ${_uiState.value.chatState}, currentStep: $currentStep")
    }
}