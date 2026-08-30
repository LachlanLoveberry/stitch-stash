package com.lachlan.stitchstash.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Standard pattern for a numeric value that's adjustable both by dragging a slider and by
 * tapping the displayed value to type an exact number. Every slider-backed numeric input in
 * the app should use this instead of a bare Slider — see CLAUDE.md "Numeric input standard".
 */
@Composable
fun LabeledSliderField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    valueText: (Float) -> String = { "${(it * 2).roundToInt() / 2f}" },
    onValueChangeFinished: (() -> Unit)? = null,
) {
    var showInput by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showInput = true }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueText(value),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    }

    if (showInput) {
        NumericInputDialog(
            title = label,
            initialValue = value,
            valueRange = valueRange,
            onDismiss = { showInput = false },
            onConfirm = {
                onValueChange(it)
                onValueChangeFinished?.invoke()
                showInput = false
            },
        )
    }
}

@Composable
private fun NumericInputDialog(
    title: String,
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var text by remember {
        mutableStateOf(
            initialValue.let { if (it == it.roundToInt().toFloat()) it.roundToInt().toString() else it.toString() },
        )
    }
    val parsed = text.toFloatOrNull()
    val isValid = parsed != null && parsed in valueRange

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Value (${valueRange.start}–${valueRange.endInclusive})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = text.isNotEmpty() && !isValid,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it.coerceIn(valueRange)) } },
                enabled = isValid,
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
