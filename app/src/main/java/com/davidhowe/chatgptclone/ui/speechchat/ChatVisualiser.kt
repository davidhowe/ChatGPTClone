package com.davidhowe.chatgptclone.ui.speechchat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.davidhowe.chatgptclone.SpeechChatState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ChatVisualiser(
    modifier: Modifier = Modifier,
    chatState: SpeechChatState,
    volumeProvider: () -> Float,
) {
    when (chatState) {
        SpeechChatState.idle, SpeechChatState.userTalking -> {
            ChatVisualYarnBall(modifier = modifier, chatState = chatState)
        }

        SpeechChatState.aiResponding -> {
            ChatVisualEqualizer(
                modifier = modifier,
                chatState = chatState,
                volumeProvider = volumeProvider,
            )
        }
    }
}

@Composable
fun ChatVisualYarnBall(
    chatState: SpeechChatState,
    modifier: Modifier = Modifier
) {
    val isTalking = chatState == SpeechChatState.userTalking
    val baseSpeed = when (chatState) {
        SpeechChatState.idle -> 1f
        SpeechChatState.aiResponding -> 2f
        SpeechChatState.userTalking -> 5f
    }

    val ringCount = 4
    val colors = listOf(
        Color(0xFF8E24AA),
        Color(0xFFBA68C8),
        Color(0xFFCE93D8),
        Color(0xFFD1C4E9)
    )

    // Track rotation and scale per ring
    val rings = remember(ringCount) {
        List(ringCount) {
            RingState(
                scale = Animatable(1f),
                angle = Animatable(0f)
            )
        }
    }

    // Animate each ring
    LaunchedEffect(chatState) {
        rings.forEachIndexed { index, ring ->
            // Wobble pulse
            if (isTalking) {
                launch {
                    ring.scale.animateTo(
                        1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600 + index * 100),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }
            } else {
                launch {
                    ring.scale.animateTo(1f, tween(400))
                }
            }

            // Continuous rotation
            launch {
                while (true) {
                    ring.angle.animateTo(
                        ring.angle.value + 360f,
                        animationSpec = tween(
                            durationMillis = (10000 / baseSpeed).toInt() + index * 250,
                            easing = LinearEasing
                        )
                    )
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .size(160.dp)
    ) {
        val center = size.center
        val maxRadius = size.minDimension / 2.2f // larger ball

        rings.forEachIndexed { i, ring ->
            val gap = 14f // wider spacing between threads
            val radius = maxRadius - (i * gap)
            val rotation = ring.angle.value
            val scale = ring.scale.value

            drawWobblyRing(
                center = center,
                baseRadius = radius * scale,
                rotation = rotation + (i * 45), // phase offset per ring
                wobbleFrequency = 5 + i,
                wobbleAmplitude = 4f + i * 1.2f, // smaller wobble to reduce overlap
                strokeWidth = 2.5f + (i * 0.4f), // subtle variation
                color = colors[i % colors.size]
            )
        }
    }

}


private class RingState(
    val scale: Animatable<Float, AnimationVector1D>,
    val angle: Animatable<Float, AnimationVector1D>
)

private fun DrawScope.drawWobblyRing(
    center: Offset,
    baseRadius: Float,
    rotation: Float,
    wobbleFrequency: Int,
    wobbleAmplitude: Float,
    strokeWidth: Float,
    color: Color
) {
    val path = Path()
    val segments = 120
    val rotationRad = Math.toRadians(rotation.toDouble())

    for (i in 0..segments) {
        val angle = (i / segments.toDouble()) * 2 * PI
        val wave = sin(angle * wobbleFrequency) * wobbleAmplitude
        val adjustedRadius = baseRadius + wave.toFloat()

        val finalAngle = angle + rotationRad

        val x = center.x + cos(finalAngle) * adjustedRadius
        val y = center.y + sin(finalAngle) * adjustedRadius

        if (i == 0) path.moveTo(x.toFloat(), y.toFloat())
        else path.lineTo(x.toFloat(), y.toFloat())
    }

    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

data class WaveSample(
    val targetValue: Float,
    var currentValue: Float = 0f,
    var decay: Float = 1f
)

@Composable
fun ChatVisualEqualizer(
    chatState: SpeechChatState,
    volumeProvider: () -> Float,
    modifier: Modifier = Modifier,
    waveDetail: Int = 80 // number of waveform "pulses" across the screen
) {
    val transitionProgress = remember { Animatable(0f) }
    val target = if (chatState == SpeechChatState.aiResponding) 1f else 0f

    LaunchedEffect(chatState) {
        transitionProgress.animateTo(
            targetValue = target,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    // Sample buffer with decay support
    val samples = remember {
        mutableStateListOf<WaveSample>().apply {
            repeat(waveDetail) {
                add(WaveSample(Random.nextFloat()))
            }
        }
    }

    // Push new samples and apply decay
    LaunchedEffect(chatState) {
        while (true) {
            if (chatState == SpeechChatState.aiResponding) {
                val newSample = volumeProvider().coerceIn(0f, 1f)
                samples.removeAt(0)
                samples.add(WaveSample(targetValue = newSample))
            }

            samples.forEach {
                // Animate upward toward target value
                if (it.currentValue < it.targetValue) {
                    it.currentValue += 0.1f
                    if (it.currentValue > it.targetValue) it.currentValue = it.targetValue
                }

                // Apply decay
                it.decay *= 0.95f
            }

            delay(33L)
        }
    }


    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val centerY = size.height / 2f
        val stepX = size.width / waveDetail.toFloat()
        val path = Path()
        val color = Color(0xFF8E24AA)

        if (transitionProgress.value >= 0.99f) {
            samples.forEachIndexed { i, sample ->
                val x = i * stepX
                val amplitude = sample.currentValue * sample.decay * centerY

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
                path = path,
                color = color,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        } else {
            // TODO: Insert your morphing yarn ball fallback drawing here
        }
    }
}








