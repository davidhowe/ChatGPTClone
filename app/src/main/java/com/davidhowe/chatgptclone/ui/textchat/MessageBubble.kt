package com.davidhowe.chatgptclone.ui.textchat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidhowe.chatgptclone.R

@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    message: String,
    isFromUser: Boolean,
    isThinking: Boolean = false,
    onCopyClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
) {
    val bubbleColor =
        if (isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor =
        if (isFromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isFromUser) {
        RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp)
    } else {
        RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            tonalElevation = 2.dp,
            modifier = Modifier.animateContentSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .widthIn(max = 280.dp)
            ) {
                if (isThinking) {
                    ShimmeringThinkingBubble()
                } else {
                    Text(
                        text = message,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Show Copy & Play buttons only for assistant messages
                    if (!isFromUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { onCopyClick?.invoke() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_copy),
                                    contentDescription = "Copy",
                                    tint = textColor
                                )
                            }
                            IconButton(onClick = { onPlayClick?.invoke() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_circle),
                                    contentDescription = "Play",
                                    tint = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmeringThinkingBubble(
    modifier: Modifier = Modifier
) {
    // Define shimmer colors with more contrast
    val shimmerColors = listOf(
        Color.Transparent,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        Color.Transparent
    )

    // Infinite transition for shimmer
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -200f, // Start off-screen left
        targetValue = 200f,   // End off-screen right
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse // Smooth back-and-forth
        ),
        label = "shimmerTranslate"
    )

    // Get Box size to scale gradient
    val density = LocalDensity.current
    val boxWidthDp = 200.dp // Approximate width; adjust or use Layout to measure
    val boxWidthPx = with(density) { boxWidthDp.toPx() }

    // Create horizontal gradient
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - boxWidthPx / 2, 0f),
        end = Offset(translateAnim + boxWidthPx / 2, 0f)
    )

    Box(
        modifier = modifier
            .height(24.dp) // Slightly taller for better visibility
            .fillMaxWidth(0.6f) // Mimic chat bubble width
            .clip(RoundedCornerShape(12.dp)) // Softer corners
            .background(brush)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) // Base color under shimmer
            .padding(horizontal = 12.dp, vertical = 6.dp) // Inner padding
    ) {
        // Optional: Add animated dots for "thinking" effect
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) { index ->
                val dotScale by transition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 300,
                            delayMillis = index * 100,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotScale$index"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .scale(dotScale)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Preview
@Composable
fun MessageBubbleAIPreview() {
    MessageBubble(
        message = "Hello, how can I assist you today?",
        isFromUser = false
    )
}

@Preview
@Composable
fun MessageBubbleUserPreview() {
    MessageBubble(
        message = "Hi, I'm looking for x",
        isFromUser = true
    )
}
