package com.lachlan.stitchstash.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelFactory),
) {
    var step by remember { mutableIntStateOf(0) }
    var marketName by remember { mutableStateOf("") }
    var marketDate by remember { mutableStateOf<LocalDate?>(null) }
    var weeklyHours by remember { mutableStateOf<Float?>(null) }
    var targetPieces by remember { mutableStateOf<Int?>(null) }

    SoftScaffold {
        LinearProgressIndicator(
            progress = { (step + 1) / 3f },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))

        when (step) {
            0 -> MarketStep(
                marketName = marketName,
                marketDate = marketDate,
                onNameChange = { marketName = it },
                onDateChange = { marketDate = it },
            )
            1 -> HoursStep(
                weeklyHours = weeklyHours ?: 4f,
                onHoursChange = { weeklyHours = it },
            )
            2 -> TargetStep(
                targetPieces = targetPieces ?: 0,
                onTargetChange = { targetPieces = it },
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = {
                    if (step < 2) step += 1
                    else viewModel.completeOnboarding(
                        marketName, marketDate, weeklyHours, targetPieces, onComplete,
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Skip for now")
            }
            Button(
                onClick = {
                    if (step < 2) step += 1
                    else viewModel.completeOnboarding(
                        marketName, marketDate, weeklyHours, targetPieces, onComplete,
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (step < 2) "Next" else "Finish")
            }
        }
    }
}

@Composable
private fun MarketStep(
    marketName: String,
    marketDate: LocalDate?,
    onNameChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("When's the next market?", style = MaterialTheme.typography.headlineLarge)
        Text(
            "We'll point the home screen at this. You can change or skip any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = marketName,
            onValueChange = onNameChange,
            label = { Text("Market name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(marketDate?.toString() ?: "Pick a date")
        }
    }

    if (showDatePicker) {
        DatePickerDialogWrapper(
            initial = marketDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChange(it)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun HoursStep(weeklyHours: Float, onHoursChange: (Float) -> Unit) {
    var showKeypadInput by remember { mutableStateOf(false) }
    val underlineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Realistic crochet hours per week?", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Think a normal week, not a perfect one. We'll tune as you log real time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "${(weeklyHours * 2).roundToInt() / 2f} hours",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showKeypadInput = true }
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val y = size.height - strokeWidth
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth,
                    )
                }
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
        )

        Slider(
            value = weeklyHours,
            onValueChange = onHoursChange,
            valueRange = 0.5f..20f,
            steps = 38,
        )
    }

    if (showKeypadInput) {
        HoursKeypadDialog(
            initialHours = weeklyHours,
            onDismiss = { showKeypadInput = false },
            onConfirm = {
                onHoursChange(it)
                showKeypadInput = false
            },
        )
    }
}

@Composable
private fun HoursKeypadDialog(
    initialHours: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var text by remember { mutableStateOf(initialHours.let { if (it == it.roundToInt().toFloat()) it.roundToInt().toString() else it.toString() }) }
    val parsed = text.toFloatOrNull()
    val isValid = parsed != null && parsed in 0.5f..20f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hours per week") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Hours (0.5–20)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = text.isNotEmpty() && !isValid,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it.coerceIn(0.5f, 20f)) } },
                enabled = isValid,
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TargetStep(targetPieces: Int, onTargetChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("How many pieces would you love to bring?", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Your ideal — not a quota. Skip if you'd rather see what naturally comes together.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = { if (targetPieces > 0) onTargetChange(targetPieces - 1) },
            ) { Text("-") }
            Text(
                text = if (targetPieces == 0) "—" else targetPieces.toString(),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            FilledTonalIconButton(onClick = { onTargetChange(targetPieces + 1) }) { Text("+") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onConfirm(date)
                } else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
