package com.lachlan.stitchstash.ui.stickers

import androidx.annotation.RawRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.lachlan.stitchstash.domain.stickers.StickerCatalog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pretty sticker tile.
 *  - Always renders the gradient circle + emoji + decorative trim (works offline, no asset)
 *  - If a matching Lottie raw resource exists, overlays the animation on top
 *  - If `earned = false`, renders a locked silhouette
 */
@Composable
fun StickerVisual(
    type: String,
    size: Dp = 80.dp,
    earned: Boolean = true,
    spin: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val def = StickerCatalog.get(type)
    val palette = StickerPalettes.forType(type)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (earned) {
            EarnedStickerBackground(palette = palette, spin = spin)
            StickerForeground(emoji = def.emoji, type = type)
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EarnedStickerBackground(palette: StickerPalette, spin: Boolean) {
    val rotation by rememberInfiniteTransition(label = "stickerSpin").animateFloat(
        initialValue = 0f,
        targetValue = if (spin) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation),
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f

        // Outer scalloped trim
        val scallops = 12
        val scallopRadius = radius * 0.13f
        for (i in 0 until scallops) {
            val angle = (i.toFloat() / scallops) * 2f * PI.toFloat()
            val ringR = radius - scallopRadius
            val sx = cx + cos(angle) * ringR
            val sy = cy + sin(angle) * ringR
            drawCircle(
                color = palette.trim,
                radius = scallopRadius,
                center = Offset(sx, sy),
            )
        }

        // Main gradient disc
        val gradient = Brush.radialGradient(
            colors = listOf(palette.center, palette.edge),
            center = Offset(cx, cy),
            radius = radius * 0.92f,
        )
        drawCircle(brush = gradient, radius = radius * 0.78f, center = Offset(cx, cy))

        // Inner highlight ring
        drawCircle(
            color = palette.highlight,
            radius = radius * 0.62f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f),
        )
    }
}

@Composable
private fun StickerForeground(emoji: String, type: String) {
    val context = LocalContext.current
    var lottieRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(type) {
        // Look up `sticker_<type>` raw resource by name. Returns 0 if absent.
        val resId = context.resources.getIdentifier("sticker_$type", "raw", context.packageName)
        lottieRes = if (resId != 0) resId else null
    }

    if (lottieRes != null) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes!!))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.fillMaxSize(0.78f),
        )
    } else {
        // Custom-drawn icon foreground + emoji combo
        Box(contentAlignment = Alignment.Center) {
            StickerIconForeground(type = type)
            Text(
                emoji,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun StickerIconForeground(type: String) {
    val palette = StickerPalettes.forType(type)
    Canvas(modifier = Modifier.fillMaxSize(0.7f)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f

        // Six small dots around the inside as a stitched-circle motif
        for (i in 0 until 6) {
            val a = (i / 6f) * 2f * PI.toFloat() + PI.toFloat() / 6f
            drawCircle(
                color = palette.accent.copy(alpha = 0.55f),
                radius = r * 0.08f,
                center = Offset(cx + cos(a) * r * 0.78f, cy + sin(a) * r * 0.78f),
            )
        }
    }
}

@RawRes
@Suppress("UNUSED_PARAMETER")
private fun resourceForType(type: String): Int? = null // resolved at runtime by name
