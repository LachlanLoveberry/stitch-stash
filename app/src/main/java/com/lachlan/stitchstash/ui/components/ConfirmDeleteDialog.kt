package com.lachlan.stitchstash.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Shared confirmation dialog for destructive delete actions. Every entity that can be
 * created in this app (pattern, colourway, completion, market, scenario, finish card)
 * should route its delete through this so the confirmation UX stays consistent.
 */
@Composable
fun ConfirmDeleteDialog(
    itemLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Delete $itemLabel?",
    message: String = "This can't be undone.",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
