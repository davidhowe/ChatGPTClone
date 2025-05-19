package com.davidhowe.chatgptclone.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Callback interface for audio recording events.
 */
interface AudioRecorderCallback {
    fun onAmplitudeLevel(level: Float)
    fun onVoiceEnded()
    fun onSilenceDetected()
    fun onRecordingStarted()
    fun onRecordingStopped(recording: ByteArray?)
}

/**
 * Utility class for recording audio using MediaRecorder, with amplitude monitoring.
 * Outputs M4A files (AAC-encoded) and provides real-time amplitude levels.
 */
@Singleton
class AudioRecorderUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var amplitudeMonitoringJob: Job? = null
    private var audioRecorderCallback: AudioRecorderCallback? = null
    private var isRecording = false
    private val recorderLock = Any() // For thread-safe MediaRecorder access

    /**
     * Sets the callback for recording events.
     */
    fun setCallback(callback: AudioRecorderCallback?) {
        this.audioRecorderCallback = callback
    }

    /**
     * Starts recording audio to a temporary M4A file.
     * @throws IllegalStateException if recording fails to initialize.
     */
    fun startRecording() {
        Timber.d("Starting audio recording")
        stopRecording() // Ensure clean state

        try {
            synchronized(recorderLock) {
                currentFile = createTempFile()
                recorder = initRecorder(currentFile!!.absolutePath)
                recorder?.prepare()
                recorder?.start()
                isRecording = true
            }
            audioRecorderCallback?.onRecordingStarted()
            startAmplitudeMonitoring()
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            cleanup()
            throw IllegalStateException("Failed to start recording: ${e.message}", e)
        }
    }

    /**
     * Stops recording and returns the recorded audio as a ByteArray.
     */
    fun stopRecording() {
        Timber.d("Stopping audio recording")
        amplitudeMonitoringJob?.cancel()
        var recordingData: ByteArray? = null

        synchronized(recorderLock) {
            if (isRecording) {
                try {
                    recorder?.stop()
                    recordingData = currentFile?.let { getByteArrayFromFile(it) }
                } catch (e: Exception) {
                    Timber.e(e, "Error stopping recorder")
                }
                isRecording = false
            }
            cleanup()
        }

        if (recordingData == null && currentFile?.exists() == true) {
            Timber.e("Failed to retrieve recording data")
        }
        audioRecorderCallback?.onRecordingStopped(recordingData)
    }

    /**
     * Creates a temporary file for recording.
     */
    private fun createTempFile(): File {
        return File.createTempFile(
            "recording_message_${System.currentTimeMillis()}",
            ".m4a",
            context.filesDir
        )
    }

    /**
     * Initializes MediaRecorder with AAC encoding at 16 kHz.
     */
    private fun initRecorder(outputPath: String): MediaRecorder {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(32000)
            setOutputFile(outputPath)
        }
    }

    /**
     * Starts monitoring amplitude with silence detection.
     */
    private fun startAmplitudeMonitoring() {
        amplitudeMonitoringJob?.cancel()
        amplitudeMonitoringJob = CoroutineScope(Dispatchers.Default).launch {
            var silentDuration = 0L
            val silenceThreshold = 1000
            val silenceTimeLimitAfterSpeech = 3000L // 3s after speech
            val silenceTimeLimitNoSpeech = 60000L  // 60s if no speech
            val interval = 100L
            var wasSpeaking = false
            val maxPossibleAmplitude = 32767f
            var smoothedLevel = 0f
            val alpha = 0.5f

            while (isActive && isRecording) {
                val amplitude = synchronized(recorderLock) {
                    if (isRecording) {
                        try {
                            recorder?.maxAmplitude ?: 0
                        } catch (e: IllegalStateException) {
                            Timber.w("Recorder not in valid state for amplitude")
                            0
                        }
                    } else {
                        0
                    }
                }

                val normalized = (amplitude / maxPossibleAmplitude).coerceIn(0f, 1f)
                smoothedLevel = alpha * normalized + (1 - alpha) * smoothedLevel

                withContext(Dispatchers.Main) {
                    audioRecorderCallback?.onAmplitudeLevel(smoothedLevel)
                }

                if (amplitude < silenceThreshold) {
                    silentDuration += interval
                    val currentSilenceLimit = if (wasSpeaking) silenceTimeLimitAfterSpeech else silenceTimeLimitNoSpeech
                    if (silentDuration >= currentSilenceLimit) {
                        withContext(Dispatchers.Main) {
                            if (wasSpeaking) {
                                audioRecorderCallback?.onVoiceEnded()
                            } else {
                                currentFile?.delete()
                                audioRecorderCallback?.onSilenceDetected()
                            }
                        }
                        stopRecording()
                        break
                    }
                } else {
                    wasSpeaking = true
                    silentDuration = 0
                }

                delay(interval)
            }
        }
    }

    /**
     * Reads a file into a ByteArray.
     */
    private fun getByteArrayFromFile(file: File): ByteArray? {
        return try {
            FileInputStream(file).use { input ->
                val byteArray = ByteArray(file.length().toInt())
                input.read(byteArray)
                byteArray
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to read recording file")
            null
        }
    }

    /**
     * Cleans up resources and resets state.
     */
    private fun cleanup() {
        synchronized(recorderLock) {
            try {
                recorder?.reset()
                recorder?.release()
            } catch (e: Exception) {
                Timber.e(e, "Error releasing recorder")
            }
            recorder = null
            currentFile?.delete()
            currentFile = null
            isRecording = false
        }
    }

    /**
     * Releases resources when the instance is no longer needed.
     */
    fun release() {
        stopRecording()
        audioRecorderCallback = null
    }
}