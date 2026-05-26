package com.lachlan.stitchstash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StitchColors = lightColorScheme(
    primary = HotPink,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blush,
    onPrimaryContainer = Berry,
    secondary = Lavender,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = LavenderLight,
    onSecondaryContainer = DeepPurple,
    tertiary = Sunshine,
    onTertiary = WarmBrownText,
    tertiaryContainer = SunshineLight,
    onTertiaryContainer = WarmBrownText,
    background = CreamPink,
    onBackground = Aubergine,
    surface = PinkSurface,
    onSurface = Aubergine,
    surfaceVariant = PalePink,
    onSurfaceVariant = MauveGrey,
    outline = MauveGrey,
)

@Composable
fun StitchStashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StitchColors,
        typography = StitchTypography,
        content = content,
    )
}
