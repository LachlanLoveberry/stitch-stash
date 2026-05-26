package com.lachlan.stitchstash.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Soft animated background — gradient base with a few slow-drifting translucent
 * yarn-ball circles. Subtle, never distracting.
 */
@Composable
fun DecorativeBackground(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.surfaceVariant.copy(alpha = 0.6f),
                colorScheme.background,
            ),
        )
    }

    val blobs = remember {
        List(5) {
            Blob(
                xFraction = Random.nextFloat(),
                yFraction = Random.nextFloat(),
                radiusFraction = 0.18f + Random.nextFloat() * 0.18f,
                color = listOf(0xFFFFD9E5, 0xFFEDE0FF, 0xFFFFE9B8, 0xFFFFD1C4).random()
                    .let { Color(it).copy(alpha = 0.35f) },
                phase = Random.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "background")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            blobs.forEach { blob ->
                val cx = (blob.xFraction + sin(t + blob.phase) * 0.05f) * size.width
                val cy = (blob.yFraction + cos(t + blob.phase) * 0.04f) * size.height
                val radius = blob.radiusFraction * size.minDimension
                drawCircle(color = blob.color, radius = radius, center = Offset(cx, cy))
                // little outline loop suggests a yarn ball
                drawCircle(
                    color = blob.color.copy(alpha = 0.55f),
                    radius = radius * 0.55f,
                    center = Offset(cx + radius * 0.15f, cy - radius * 0.1f),
                    style = Stroke(width = 2f),
                )
            }
        }
    }
}

private data class Blob(
    val xFraction: Float,
    val yFraction: Float,
    val radiusFraction: Float,
    val color: Color,
    val phase: Float,
)
