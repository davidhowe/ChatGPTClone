package com.davidhowe.chatgptclone.ui.speechchat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidhowe.chatgptclone.SpeechChatState
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// Data class for animation parameters
data class AnimationParams(
    val rotationSpeed: Float, // Degrees per second
    val pulseScale: Float, // Scale factor for pulsing
    val waveformAmplitude: Float // Max height of waveform
)

@Composable
fun ChatAnimationIndicator(
    state: SpeechChatState,
    modifier: Modifier = Modifier
) {
    // Map enum states to animation parameters
    val params = when (state) {
        SpeechChatState.idle -> AnimationParams(
            rotationSpeed = 90f, // Slower orbit
            pulseScale = 1f, // No pulsing
            waveformAmplitude = 0f // No waveform
        )

        SpeechChatState.userTalking -> AnimationParams(
            rotationSpeed = 270f, // Faster orbit
            pulseScale = 2.3f, // Pulsing effect
            waveformAmplitude = 0f // No waveform
        )

        SpeechChatState.aiResponding -> AnimationParams(
            rotationSpeed = 0f, // No rotation
            pulseScale = 1f, // No pulsing
            waveformAmplitude = 50f // Waveform active
        )
    }

    // Transition for smooth state changes
    val transition = updateTransition(targetState = params, label = "chatAnimation")
    val rotationSpeed by transition.animateFloat(
        transitionSpec = { tween(500) },
        label = "rotationSpeed"
    ) { it.rotationSpeed }
    val pulseScale by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "pulseScale"
    ) { it.pulseScale }
    val waveformAmplitude by transition.animateFloat(
        transitionSpec = { tween(700) },
        label = "waveformAmplitude"
    ) { it.waveformAmplitude }

    // Infinite rotation animation for orbiting
    val rotationAngle = remember { Animatable(0f) }
    LaunchedEffect(rotationSpeed) {
        if (rotationSpeed > 0f) {
            rotationAngle.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (360000 / rotationSpeed).toInt().coerceAtLeast(100),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotationAngle.snapTo(0f)
        }
    }

    // Pulse animation for userTalking state
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (state == SpeechChatState.userTalking) {
            pulseAnim.animateTo(
                targetValue = pulseScale,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAnim.snapTo(1f)
        }
    }

    // Simulated audio magnitude for waveform (replace with real audio data later)
    var waveformMagnitude by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        if (state == SpeechChatState.aiResponding) {
            while (true) {
                waveformMagnitude = (sin(System.currentTimeMillis() / 1000.0) * 0.5 + 0.5).toFloat()
                kotlinx.coroutines.delay(50)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        val yarnRadius = 50.dp.toPx() // Base radius of yarn orbit
        val lineThickness = 4.dp.toPx()
        val numLines = 3 // Number of orbiting lines
        val lineLengthDegrees = 60f // Angular length of each line

        when (state) {
            SpeechChatState.idle, SpeechChatState.userTalking -> {
                // Draw orbiting lines following each other
                repeat(numLines) { index ->
                    val baseAngle = (rotationAngle.value + index * (360f / numLines)) % 360f
                    drawOrbitingLine(
                        center = center,
                        baseRadius = yarnRadius * (1f + index * 0.1f), // Slight radius variation
                        startAngle = baseAngle,
                        sweepAngle = lineLengthDegrees * pulseAnim.value, // Pulse affects length
                        thickness = lineThickness * pulseAnim.value, // Pulse affects thickness
                        color = Color.Blue
                    )
                }
            }

            SpeechChatState.aiResponding -> {
                // Draw waveform
                val path = Path()
                val lineY = center.y
                path.moveTo(0f, lineY)

                val numPoints = 50
                val step = canvasWidth / (numPoints - 1)
                for (i in 0 until numPoints) {
                    val x = i * step
                    val amplitude =
                        waveformAmplitude * waveformMagnitude * (1 - (x / canvasWidth - 0.5f).pow(2))
                    val y =
                        lineY + sin((x / canvasWidth) * 4 * Math.PI + System.currentTimeMillis() / 200.0).toFloat() * amplitude
                    path.lineTo(x, y)
                }
                path.lineTo(canvasWidth, lineY)

                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(
                        width = lineThickness,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
        }
    }
}

// Helper function to draw a single orbiting line
private fun DrawScope.drawOrbitingLine(
    center: Offset,
    baseRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    thickness: Float,
    color: Color
) {
    val path = Path()
    val segments = 20 // Smoothness of curve
    val angleStep = sweepAngle / segments

    for (i in 0..segments) {
        val angle = Math.toRadians((startAngle + i * angleStep).toDouble()).toFloat()
        val radius = baseRadius * (1f + sin(i * 0.3f) * 0.1f) // Slight wobble for yarn effect
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = thickness,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )
}

@Preview(showBackground = true)
@Composable
fun ChatAnimationIndicatorPreview() {
    var state by remember { mutableStateOf(SpeechChatState.idle) }
    MaterialTheme {
        Column {
            ChatAnimationIndicator(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
            LaunchedEffect(Unit) {
                while (true) {
                    state = SpeechChatState.idle
                    kotlinx.coroutines.delay(2000)
                    state = SpeechChatState.userTalking
                    kotlinx.coroutines.delay(2000)
                    state = SpeechChatState.aiResponding
                    kotlinx.coroutines.delay(2000)
                }
            }
        }
    }
}