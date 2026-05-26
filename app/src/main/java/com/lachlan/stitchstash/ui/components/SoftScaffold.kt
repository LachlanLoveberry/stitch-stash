package com.lachlan.stitchstash.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Soft scaffold with proper safe-area insets. Respects status + nav bars so content
 * never sits under the camera notch or under the home indicator.
 */
@Composable
fun SoftScaffold(
    background: @Composable (Modifier) -> Unit = { mod ->
        Surface(modifier = mod, color = MaterialTheme.colorScheme.background) {}
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    Box(modifier = Modifier.fillMaxSize()) {
        background(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    PaddingValues(
                        top = systemBars.calculateTopPadding() + 12.dp,
                        bottom = systemBars.calculateBottomPadding() + 12.dp,
                        start = 20.dp,
                        end = 20.dp,
                    ),
                ),
            content = content,
        )
    }
}
