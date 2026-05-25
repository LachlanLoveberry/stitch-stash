package com.lachlan.stitchstash.ui.markets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    onBack: () -> Unit,
    viewModel: MarketsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val markets by viewModel.markets.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Markets", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { showAdd = true }) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))

        if (markets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No markets yet. Add one to see how your pace lines up.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                markets.forEach { market ->
                    MarketRow(market, viewModel)
                }
            }
        }
    }

    if (showAdd) {
        AddMarketDialog(
            onAdd = { name, date ->
                viewModel.add(name, date)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun MarketRow(market: Market, viewModel: MarketsViewModel) {
    val date = LocalDate.ofEpochDay(market.dateEpochDay)
    var showDate by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                market.name,
                style = MaterialTheme.typography.titleLarge,
                textDecoration = if (market.isSkipped) TextDecoration.LineThrough else null,
            )
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }) { Text("Reschedule") }
                TextButton(onClick = { viewModel.toggleSkipped(market) }) {
                    Text(if (market.isSkipped) "Unskip" else "Skip")
                }
            }
        }
    }
    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        viewModel.reschedule(
                            market,
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(),
                        )
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMarketDialog(onAdd: (String, LocalDate) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add market") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedButton(onClick = { showPicker = true }) {
                    Text(date?.toString() ?: "Pick date")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { date?.let { onAdd(name, it) } },
                enabled = name.isNotBlank() && date != null,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
