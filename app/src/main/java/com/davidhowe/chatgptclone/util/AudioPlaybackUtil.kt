package com.davidhowe.chatgptclone.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.davidhowe.chatgptclone.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

/**
 * Callback interface for audio playback events.
 */
interface AudioPlaybackCallback {
    fun onAmplitudeLevel(level: Float)
    fun onPlaybackEnded()
}

/**
 * Utility class for playing 16-bit PCM audio from a ByteArray using AudioTrack,
 * with amplitude monitoring for equalizer visualization.
 */
@Singleton
class AudioPlaybackUtil @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var audioTrack: AudioTrack? = null
    private var amplitudeMonitoringJob: Job? = null
    private var audioPlaybackCallback: AudioPlaybackCallback? = null
    private var isPlaying = false
    private val trackLock = Any() // For thread-safe AudioTrack access

    /**
     * Sets the callback for playback events.
     */
    fun setCallback(callback: AudioPlaybackCallback?) {
        this.audioPlaybackCallback = callback
    }

    /**
     * Plays audio from a ByteArray, handling WAV headers if present.
     * @param audioData ByteArray containing 16-bit PCM audio (raw or WAV format).
     * @param sampleRate Sample rate in Hz (default 24000 for Google TTS LINEAR16).
     */
    suspend fun playAudio(audioData: ByteArray, sampleRate: Int = 24000) {
        Timber.d("Starting audio playback, data size: ${audioData.size} bytes")

        if (isPlaying) {
            // NB, we dont want to send callback here, as it will be sent when the playback ends officially
            try {
                audioTrack?.stop()
            } catch (e: IllegalStateException) {
                Timber.w(e, "Error stopping AudioTrack")
            }
            audioTrack?.release()
            audioTrack = null
            isPlaying = false
        }

        try {
            // Extract PCM data (strip WAV header if present)
            val pcmData = extractPcmData(audioData)
            if (pcmData.isEmpty()) {
                Timber.e("Invalid or empty audio data")
                withContext(Dispatchers.Main) {
                    audioPlaybackCallback?.onPlaybackEnded()
                }
                return
            }

            // Initialize AudioTrack
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            synchronized(trackLock) {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack?.play()
                isPlaying = true
            }

            // Start amplitude monitoring
            startAmplitudeMonitoring(pcmData, bufferSize, sampleRate)

            // Stream audio data
            CoroutineScope(ioDispatcher).launch {
                try {
                    var offset = 0
                    val chunkSize = bufferSize
                    while (offset < pcmData.size && isPlaying) {
                        val length = minOf(chunkSize, pcmData.size - offset)
                        synchronized(trackLock) {
                            if (isPlaying) {
                                audioTrack?.write(pcmData, offset, length)
                            }
                        }
                        offset += length
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error during audio playback")
                } finally {
                    withContext(Dispatchers.Main) {
                        stopPlayback()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start playback")
            withContext(Dispatchers.Main) {
                stopPlayback()
            }
        }
    }

    /**
     * Extracts raw PCM data from a ByteArray, removing WAV header if present.
     */
    private fun extractPcmData(audioData: ByteArray): ByteArray {
        if (audioData.size < 44 || audioData[0].toChar() != 'R' || audioData[1].toChar() != 'I') {
            return audioData // Assume raw PCM if not a WAV
        }
        try {
            // Standard WAV header is 44 bytes for PCM
            return audioData.copyOfRange(44, audioData.size)
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract PCM data")
            return byteArrayOf()
        }
    }

    /**
     * Starts monitoring amplitude for equalizer visualization.
     */
    private fun startAmplitudeMonitoring(audioData: ByteArray, bufferSize: Int, sampleRate: Int) {
        amplitudeMonitoringJob?.cancel()
        amplitudeMonitoringJob = CoroutineScope(Dispatchers.Default).launch {
            val interval = 100L
            val maxPossibleAmplitude = 32767f
            var smoothedLevel = 0f
            val alpha = 0.5f

            var offset = 0
            val chunkSize = bufferSize

            while (isActive && offset < audioData.size && isPlaying) {
                val length = minOf(chunkSize, audioData.size - offset)
                val chunk = audioData.copyOfRange(offset, offset + length)
                val amplitude = calculateMaxAmplitude(chunk)
                val normalized = (amplitude / maxPossibleAmplitude).coerceIn(0f, 1f)
                smoothedLevel = alpha * normalized + (1 - alpha) * smoothedLevel

                withContext(Dispatchers.Main) {
                    audioPlaybackCallback?.onAmplitudeLevel(smoothedLevel)
                }

                offset += (sampleRate * interval / 1000 * 2).toInt() // 2 bytes per sample
                delay(interval)
            }

            if (offset >= audioData.size) {
                withContext(Dispatchers.Main) {
                    stopPlayback()
                }
            }
        }
    }

    /**
     * Calculates max amplitude from a 16-bit PCM chunk.
     */
    private fun calculateMaxAmplitude(chunk: ByteArray): Float {
        var maxAmplitude = 0
        for (i in chunk.indices step 2) {
            if (i + 1 < chunk.size) {
                val sample = (chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)
                maxAmplitude = max(maxAmplitude, abs(sample))
            }
        }
        return maxAmplitude.toFloat()
    }

    /**
     * Stops playback and cleans up resources.
     */
    fun stopPlayback() {
        synchronized(trackLock) {
            if (isPlaying) {
                try {
                    audioTrack?.stop()
                } catch (e: IllegalStateException) {
                    Timber.w(e, "Error stopping AudioTrack")
                }
                audioTrack?.release()
                audioTrack = null
                isPlaying = false
            }
            amplitudeMonitoringJob?.cancel()
        }
        audioPlaybackCallback?.onPlaybackEnded()
    }

    /**
     * Releases resources when the instance is no longer needed.
     */
    fun release() {
        stopPlayback()
        audioPlaybackCallback = null
    }
}