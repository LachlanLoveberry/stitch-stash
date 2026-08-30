package com.lachlan.stitchstash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard footer for a two-(or more-)button dialog/screen action row. Buttons keep their
 * natural (content) width instead of being forced to share the row via `Modifier.weight(1f)` —
 * if the combined width doesn't fit, a button wraps to its own line instead of its label
 * wrapping inside a squeezed button and inflating row height. See CLAUDE.md "Button row
 * standard".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogActionRow(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, horizontalAlignment),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}
