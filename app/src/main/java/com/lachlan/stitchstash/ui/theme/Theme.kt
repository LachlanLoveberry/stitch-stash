package com.lachlan.stitchstash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StitchColors = lightColorScheme(
    primary = Rose,
    onPrimary = WarmBrown,
    primaryContainer = RoseLight,
    onPrimaryContainer = WarmBrown,
    secondary = Sage,
    onSecondary = WarmBrown,
    secondaryContainer = SageLight,
    onSecondaryContainer = WarmBrown,
    tertiary = SoftGold,
    onTertiary = WarmBrown,
    background = Cream,
    onBackground = WarmBrown,
    surface = Cream,
    onSurface = WarmBrown,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = WarmGrey,
    outline = WarmGrey,
)

@Composable
fun StitchStashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StitchColors,
        typography = StitchTypography,
        content = content,
    )
}
