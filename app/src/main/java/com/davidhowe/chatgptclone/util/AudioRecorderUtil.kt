package com.davidhowe.chatgptclone.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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

interface AudioRecorderCallback {
    fun onAmplitudeLevel(level: Float)
    fun onVoiceEnded()
    fun onSilenceDetected()
    fun onRecordingStarted()
    fun onRecordingStopped(recording: ByteArray?)
}

@Singleton
class AudioRecorderUtil @Inject constructor(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    private var amplitudeMonitoringJob: Job? = null
    private var audioRecorderCallback: AudioRecorderCallback? = null

    fun setCallback(callback: AudioRecorderCallback) {
        this.audioRecorderCallback = callback
    }

    fun startRecording() {
        Timber.d("startRecording")
        createNewRecording()
        recorder?.start()
        audioRecorderCallback?.onRecordingStarted()
        startAmplitudeMonitoring()
    }

    fun stopRecording() {
        Timber.d("stopRecording")
        amplitudeMonitoringJob?.cancel()
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            recorder = null
            amplitudeMonitoringJob?.cancel()
            val byteArray =
                getByteArrayFromFilePath(currentFile?.path.takeUnless { it.isNullOrBlank() } ?: "")
            audioRecorderCallback?.onRecordingStopped(byteArray)
        }

        if (currentFile?.exists() != true) {
            Timber.e("Message audio recording not saved")
        }

        clearState()
    }

    private fun createNewRecording() {
        currentFile = File.createTempFile(
            "recording_message_${System.currentTimeMillis()}",
            ".m4a",
            context.filesDir
        )
        initRecorder(currentFile!!.absolutePath)
        try {
            recorder?.apply {
                prepare()
            }
        } catch (e: Exception) {
            Timber.e(e)
            // currentFile?.delete()
            recorder?.release()
            recorder = null
            throw IllegalStateException("Failed to start recording: ${e.message}")
        }
    }

    private fun initRecorder(outputPath: String) {
        recorder?.release()
        recorder = null

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(32000)
            setOutputFile(outputPath)
        }
    }

    private fun clearState() {
        currentFile = null
        recorder?.release()
        recorder = null
    }

    private fun startAmplitudeMonitoring() {
        amplitudeMonitoringJob?.cancel()

        amplitudeMonitoringJob = CoroutineScope(Dispatchers.Default).launch {
            var silentDuration = 0L
            val silenceThreshold = 1000
            val silenceTimeLimit = 2000L
            val interval = 100L
            var wasSpeaking = false
            val maxPossibleAmplitude = 32767f

            var smoothedLevel = 0f
            val alpha = 0.5f // Lower = smoother (0.1–0.3 is typical)

            while (isActive && recorder != null) {
                val amplitude = recorder?.maxAmplitude ?: 0
                val normalized = (amplitude / maxPossibleAmplitude).coerceIn(0f, 1f)

                smoothedLevel = alpha * normalized + (1 - alpha) * smoothedLevel

                withContext(Dispatchers.Main) {
                    audioRecorderCallback?.onAmplitudeLevel(smoothedLevel)
                }

                if (amplitude < silenceThreshold) {
                    silentDuration += interval
                    if (silentDuration >= silenceTimeLimit) {
                        withContext(Dispatchers.Main) {
                            if (wasSpeaking) {
                                audioRecorderCallback?.onVoiceEnded()
                            } else {
                                currentFile?.delete()
                                audioRecorderCallback?.onSilenceDetected()
                            }
                        }
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

    fun getByteArrayFromFilePath(filePath: String): ByteArray? {
        return try {
            val file = File(filePath)
            val fileInputStream = FileInputStream(file)
            val byteArray = ByteArray(file.length().toInt())
            fileInputStream.read(byteArray)
            fileInputStream.close()
            byteArray
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}



