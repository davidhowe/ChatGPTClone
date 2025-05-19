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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max


interface AudioPlaybackCallback {
    fun onAmplitudeLevel(level: Float)
    fun onPlaybackEnded()
}

@Singleton
class AudioPlaybackUtil @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var amplitudeMonitoringJob: Job? = null
    private var audioPlaybackCallback: AudioPlaybackCallback? = null
    private var audioTrack: AudioTrack? = null

    fun setCallback(callback: AudioPlaybackCallback) {
        this.audioPlaybackCallback = callback
    }

    fun playAudio(audioData: ByteArray) {
        val sampleRate = 24000
        // Stop any existing playback
        stopPlayback()

        // Initialize AudioTrack
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

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

        // Start playback and amplitude monitoring
        audioTrack?.play()
        startAmplitudeMonitoring(audioData, bufferSize, sampleRate)

        // Stream audio data
        CoroutineScope(ioDispatcher).launch {
            var offset = 0
            val chunkSize = bufferSize
            while (offset < audioData.size && audioTrack != null) {
                val length = minOf(chunkSize, audioData.size - offset)
                audioTrack?.write(audioData, offset, length)
                offset += length
            }
            // Stop and cleanup after playback
            withContext(Dispatchers.Main) {
                stopPlayback()
            }
        }
    }

    private fun startAmplitudeMonitoring(
        audioData: ByteArray,
        bufferSize: Int,
        sampleRate: Int,
    ) {
        amplitudeMonitoringJob?.cancel()

        amplitudeMonitoringJob = CoroutineScope(Dispatchers.Default).launch {
            val interval = 100L
            val maxPossibleAmplitude = 32767f // Max for 16-bit PCM

            var smoothedLevel = 0f
            val alpha = 0.5f // Smoothing factor (match your recorder)

            var offset = 0
            val chunkSize = bufferSize // Process audio in chunks matching AudioTrack buffer

            while (isActive && offset < audioData.size) {
                // Extract a chunk of audio data
                val length = minOf(chunkSize, audioData.size - offset)
                val chunk = audioData.copyOfRange(offset, offset + length)

                // Calculate max amplitude for the chunk
                val amplitude = calculateMaxAmplitude(chunk)

                // Normalize amplitude (0–1)
                val normalized = (amplitude / maxPossibleAmplitude).coerceIn(0f, 1f)

                // Apply smoothing
                smoothedLevel = alpha * normalized + (1 - alpha) * smoothedLevel

                // Deliver amplitude to callback
                withContext(Dispatchers.Main) {
                    audioPlaybackCallback?.onAmplitudeLevel(smoothedLevel)
                }

                // Move to next chunk, accounting for time interval
                offset += (sampleRate * interval / 1000 * 2).toInt() // 2 bytes per sample
                delay(interval)
            }
        }
    }

    // Calculate max amplitude from 16-bit PCM chunk
    private fun calculateMaxAmplitude(chunk: ByteArray): Float {
        var maxAmplitude = 0
        for (i in chunk.indices step 2) {
            if (i + 1 < chunk.size) {
                // Convert two bytes to 16-bit sample
                val sample = (chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)
                maxAmplitude = max(maxAmplitude, abs(sample))
            }
        }
        return maxAmplitude.toFloat()
    }

    fun stopPlayback() {
        audioPlaybackCallback?.onPlaybackEnded()
        amplitudeMonitoringJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}



