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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
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
                startSimulatedVolumeLevels(_aiVolumeLevel, 5000L)
                _uiState.value = _uiState.value.copy(
                    chatState = SpeechChatState.aiResponding
                )
                startSimulatedVolumeLevels(_aiVolumeLevel, 5000L)
                _uiState.value = _uiState.value.copy(
                    chatState = SpeechChatState.idle
                )
                startSimulatedVolumeLevels(_aiVolumeLevel, 5000L)
                _uiState.value = _uiState.value.copy(
                    chatState = SpeechChatState.userTalking
                )
            }
        }
    }

    /*suspend fun startSimulatedVolumeLevels() {
        val random = Random(System.currentTimeMillis())

        repeat(400) { i ->
            val voicePulse = abs(sin(i / 6f)) * random.nextFloat()
            _aiVolumeLevel.value = voicePulse.coerceIn(0f, 1f)
            delay(30L) // ~33 FPS
        }
    }*/


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
                ((phaseProgress - (1f - transitionDuration / phaseDuration)) * phaseDuration / transitionDuration).coerceIn(0f, 1f)
            } else {
                0f
            }
            val effectiveBaseVolume = baseVolume * (1f - transitionFactor) + nextPhaseVolume * transitionFactor

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
                voicePulse = (effectiveBaseVolume + (phraseWave + syllableWave + burstEnvelope + randomVariation) * volumeScale)
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

/*
fun drawYarnBallMorph(
    drawScope: DrawScope,
    morphProgress: Float,
    volume: Float,
    frameTimeMillis: Long
) = with(drawScope) {
    val center = size.center
    val minRadius = size.minDimension / 4.5f
    val maxRadius = size.minDimension / 2.2f
    val baseRadius = lerp(minRadius, maxRadius, volume)
    val colors = listOf(
        Color(0xFF8E24AA), Color(0xFFBA68C8),
        Color(0xFFCE93D8), Color(0xFFD1C4E9)
    )
    val spacing = lerp(15f, 30f, volume)
    val segments = 100

    // Volume-driven dynamics
    val wobbleMultiplier = 1f//lerp(0.5f, 1.5f, volume) // how big the wobble grows
    val spinSpeed = lerp(0.5f, 9f, volume)          // how fast the rings spin

    val time = frameTimeMillis / 300f * spinSpeed

    colors.forEachIndexed { i, color ->
        val radius = baseRadius - (i * spacing)
        val angleOffset = time + i * 30f
        val path = Path()

        for (j in 0..segments) {
            val theta = j / segments.toDouble() * 2 * PI

            val wobble = sin(theta * (4 + i) + angleOffset) * (4 + i) * wobbleMultiplier
            val effectiveRadius = radius + wobble.toFloat()

            val xBall = center.x + cos(theta) * effectiveRadius
            val yBall = center.y + sin(theta) * effectiveRadius

            val t = j / segments.toFloat()
            val xFlat = size.width * t
            val yFlat = center.y

            val x = lerp(xBall.toFloat(), xFlat, morphProgress)
            val y = lerp(yBall.toFloat(), yFlat, morphProgress)

            if (j == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        val strokeWidth = lerp(2.5f + i * 0.4f, 0f, morphProgress)
        if (strokeWidth > 0.3f) {
            drawPath(
                path,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
 */