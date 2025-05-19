package com.davidhowe.chatgptclone.ui.speechchat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.davidhowe.chatgptclone.SpeechChatState
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class WaveSample(val targetValue: Float, var currentValue: Float = 0f, var decay: Float = 1f)


@Composable
fun ChatVisualiser(
    modifier: Modifier = Modifier,
    chatState: SpeechChatState,
    volumeProvider: () -> Float,
    waveDetail: Int = 120
) {
    val morphProgress = remember { Animatable(0f) }

    var frameTimeMillis by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { timeNanos ->
                frameTimeMillis = timeNanos / 1_000_000 // convert to ms
            }
        }
    }

    LaunchedEffect(chatState) {
        val target = if (chatState == SpeechChatState.aiResponding) 1f else 0f
        morphProgress.animateTo(
            targetValue = target,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    val samples = remember {
        mutableStateListOf<WaveSample>().apply {
            repeat(waveDetail) {
                add(WaveSample(0f))
            }
        }
    }

    var currentVolume = 0f

    LaunchedEffect(chatState) {
        while (true) {
            currentVolume = volumeProvider().coerceIn(0f, 1f)
            Timber.d("currentVolume: $currentVolume")

            if (chatState == SpeechChatState.aiResponding) {
                samples.removeAt(0)
                samples.add(WaveSample(targetValue = currentVolume))
            }

            samples.forEach {
                // animate up to target
                if (it.currentValue < it.targetValue) {
                    it.currentValue += 0.1f
                    if (it.currentValue > it.targetValue) it.currentValue = it.targetValue
                }
                it.decay *= 0.95f
            }

            delay(33L)
        }
    }

    Canvas(
        modifier = modifier
            .height(160.dp)
            .fillMaxWidth()
    ) {
        drawYarnBallMorph(
            this,
            morphProgress.value,
            volume = currentVolume,
            frameTimeMillis = frameTimeMillis,
        )
        drawEqualizerMorph(this, samples, morphProgress.value)
    }
}

fun drawYarnBallMorph(
    drawScope: DrawScope,
    morphProgress: Float,
    volume: Float,
    frameTimeMillis: Long,
) = with(drawScope) {
    val center = size.center
    val minRadius = size.minDimension / 4.5f
    val maxRadius = size.minDimension / 2.2f
    val baseRadius = lerp(minRadius, maxRadius, volume)
    val colors = listOf(
        Color(0xFF8E24AA), Color(0xFFBA68C8),
        Color(0xFFCE93D8), Color(0xFFD1C4E9)
    )
    val spacing = 30f//lerp(15f, 30f, volume)
    val segments = 100

    // Volume-driven dynamics
    val wobbleMultiplier = 1f

    val baseSpinSpeed = lerp(0.2f, 4f, volume) // 🐢 use lower upper bound for subtle speed

    colors.forEachIndexed { i, color ->
        val radius = baseRadius - (i * spacing)

        // Alternate direction for each ring
        val direction = if (i % 2 == 0) 1f else -1f

        // Volume-controlled speed with slight variation by ring
        val time = frameTimeMillis / 1000f * (baseSpinSpeed + i * 0.1f) * direction

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


fun drawEqualizerMorph(drawScope: DrawScope, samples: List<WaveSample>, morphProgress: Float) =
    with(drawScope) {
        val centerY = size.height / 2f
        val stepX = size.width / samples.size.toFloat()
        val path = Path()
        val color = Color(0xFF8E24AA)

        samples.forEachIndexed { i, sample ->
            val x = i * stepX
            val amplitude = sample.currentValue * sample.decay * centerY * morphProgress

            val midX = x + stepX / 2
            val nextX = x + stepX

            val baseY = centerY
            val topY = baseY - amplitude
            val bottomY = baseY + amplitude

            if (i == 0) {
                path.moveTo(x, baseY)
            }

            path.lineTo(midX, topY)
            path.lineTo(nextX, bottomY)
            path.lineTo(nextX, baseY)
        }

        drawPath(
            path,
            color = color.copy(alpha = morphProgress),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
    }

fun lerp(start: Float, end: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * end
}









