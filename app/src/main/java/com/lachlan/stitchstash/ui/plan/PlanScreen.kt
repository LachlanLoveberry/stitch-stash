package com.lachlan.stitchstash.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.domain.forecast.ScenarioSolver
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(ScenarioDraft(name = "Main plan")) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Pre-fill from the active scenario when it loads
    LaunchedEffect(state.activeScenarioId) {
        val active = state.scenarios.firstOrNull { it.isActive } ?: return@LaunchedEffect
        draft = ScenarioDraft(
            id = active.id,
            name = active.name,
            marketId = active.marketId,
            date = active.customDateEpochDay?.let { LocalDate.ofEpochDay(it) }
                ?: state.markets.firstOrNull { it.id == active.marketId }
                    ?.let { LocalDate.ofEpochDay(it.dateEpochDay) },
            targetPieces = active.targetPieces,
            weeklyHours = active.weeklyHours,
            lockedKey = active.lockedKey,
        )
    }

    val solveResult = remember(draft, state) { viewModel.solve(draft, state) }

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Plan", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { showSaveDialog = true }) { Text("Save") }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Lock any two — the third tells you what'd take to make it work.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val lockedKey = ScenarioSolver.LockedKey.from(draft.lockedKey)

            // Market date row
            val dateLocked = lockedKey != ScenarioSolver.LockedKey.HOURS_PIECES
            LockableRow(
                label = "Market date",
                locked = dateLocked,
                onToggleLock = { draft = draft.cycleLockExcluding(LockField.DATE) },
                isResult = !dateLocked,
            ) {
                if (dateLocked) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(draft.date?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Pick a date")
                    }
                } else {
                    ResultChip(
                        when (solveResult) {
                            is ScenarioSolver.SolveResult.FinishDate ->
                                "≈ ${solveResult.value.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                            else -> "Set the other two"
                        },
                    )
                }
            }

            // Target pieces row
            val piecesLocked = lockedKey != ScenarioSolver.LockedKey.HOURS_DATE
            LockableRow(
                label = "Pieces I'd love to bring",
                locked = piecesLocked,
                onToggleLock = { draft = draft.cycleLockExcluding(LockField.PIECES) },
                isResult = !piecesLocked,
            ) {
                if (piecesLocked) {
                    NumberStepper(
                        value = draft.targetPieces ?: 0,
                        onChange = { draft = draft.copy(targetPieces = it) },
                    )
                } else {
                    ResultChip(
                        when (solveResult) {
                            is ScenarioSolver.SolveResult.AchievablePieces -> "≈ ${solveResult.value} pieces"
                            else -> "Set the other two"
                        },
                    )
                }
            }

            // Hours row
            val hoursLocked = lockedKey != ScenarioSolver.LockedKey.DATE_PIECES
            LockableRow(
                label = "Hours / week",
                locked = hoursLocked,
                onToggleLock = { draft = draft.cycleLockExcluding(LockField.HOURS) },
                isResult = !hoursLocked,
            ) {
                if (hoursLocked) {
                    HoursSlider(
                        value = draft.weeklyHours ?: 4f,
                        onChange = { draft = draft.copy(weeklyHours = it) },
                    )
                } else {
                    ResultChip(
                        when (solveResult) {
                            is ScenarioSolver.SolveResult.HoursPerWeek ->
                                "≈ ${(solveResult.value * 2).roundToInt() / 2f} h/week"
                            else -> "Set the other two"
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Saved scenarios", style = MaterialTheme.typography.titleLarge)

            if (state.scenarios.isEmpty()) {
                Text(
                    "Tap Save above to keep this configuration as a scenario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.scenarios.forEach { sc ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sc.isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.activate(sc.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sc.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    summarise(sc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.delete(sc.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.date?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        draft = draft.copy(
                            date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(),
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }

    if (showSaveDialog) {
        SaveScenarioDialog(
            initialName = draft.name,
            onSave = { name ->
                viewModel.saveScenario(draft.copy(name = name)) { id ->
                    draft = draft.copy(id = id, name = name)
                    viewModel.activate(id)
                }
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

private fun summarise(sc: com.lachlan.stitchstash.data.db.entities.Scenario): String {
    val parts = mutableListOf<String>()
    sc.customDateEpochDay?.let {
        parts += LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM d"))
    }
    sc.targetPieces?.let { parts += "$it pieces" }
    sc.weeklyHours?.let { parts += "${(it * 2).roundToInt() / 2f}h/wk" }
    return parts.joinToString(" · ")
}

private enum class LockField { DATE, PIECES, HOURS }

private fun ScenarioDraft.cycleLockExcluding(toUnlock: LockField): ScenarioDraft {
    // Exactly two of three must be locked. Toggling means: the currently-result field
    // becomes locked, and the toggled one becomes the result.
    val currentlyResult = when (ScenarioSolver.LockedKey.from(lockedKey)) {
        ScenarioSolver.LockedKey.DATE_PIECES -> LockField.HOURS
        ScenarioSolver.LockedKey.HOURS_DATE -> LockField.PIECES
        ScenarioSolver.LockedKey.HOURS_PIECES -> LockField.DATE
    }
    if (toUnlock == currentlyResult) return this // nothing to do
    val newResult = toUnlock
    val newLocked = when (newResult) {
        LockField.DATE -> ScenarioSolver.LockedKey.HOURS_PIECES
        LockField.PIECES -> ScenarioSolver.LockedKey.HOURS_DATE
        LockField.HOURS -> ScenarioSolver.LockedKey.DATE_PIECES
    }
    return copy(lockedKey = newLocked.storage)
}

@Composable
private fun LockableRow(
    label: String,
    locked: Boolean,
    onToggleLock: () -> Unit,
    isResult: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isResult) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable(onClick = onToggleLock)
                        .padding(end = 8.dp),
                )
                Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (isResult) {
                    Text(
                        "result",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ResultChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NumberStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalIconButton(onClick = { if (value > 0) onChange(value - 1) }) { Text("-") }
        Text(
            text = if (value == 0) "—" else value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

@Composable
private fun HoursSlider(value: Float, onChange: (Float) -> Unit) {
    Column {
        Text(
            "${(value * 2).roundToInt() / 2f} hours",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0.5f..30f,
            steps = 58,
        )
    }
}

@Composable
private fun SaveScenarioDialog(initialName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save scenario") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
