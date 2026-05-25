package com.lachlan.stitchstash.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPiece(
    val startX: Float,
    val color: Color,
    val drift: Float,
    val speed: Float,
    val size: Float,
)

@Composable
fun ConfettiBurst(active: Boolean) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
    )
    val pieces = remember {
        List(50) {
            ConfettiPiece(
                startX = Random.nextFloat(),
                color = palette.random(),
                drift = Random.nextFloat() * 0.2f - 0.1f,
                speed = 0.6f + Random.nextFloat() * 0.6f,
                size = 6f + Random.nextFloat() * 8f,
            )
        }
    }
    var trigger by remember { mutableStateOf(0f) }
    LaunchedEffect(active) {
        if (active) trigger = 1f
    }
    val progress by animateFloatAsState(
        targetValue = trigger,
        animationSpec = tween(durationMillis = 1800, easing = LinearEasing),
        label = "confetti",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (progress <= 0f || progress >= 1f) return@Canvas
        pieces.forEach { piece ->
            val y = (-0.1f + progress * piece.speed) * size.height
            val x = (piece.startX + piece.drift * progress + sin(progress * 6f) * 0.02f) * size.width
            drawCircle(
                color = piece.color,
                radius = piece.size,
                center = Offset(x, y),
            )
        }
    }
}
