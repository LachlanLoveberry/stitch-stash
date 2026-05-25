package com.lachlan.stitchstash.ui.log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCompletionScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onAddPattern: () -> Unit,
    onCreateCard: (Long) -> Unit,
    viewModel: LogCompletionViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selectedPattern by remember { mutableStateOf<Pattern?>(null) }
    var selectedColourway by remember { mutableStateOf<Colourway?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var notes by remember { mutableStateOf("") }
    var energyTag by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf<CelebrationData?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> if (uri != null) photoUri = uri }

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Text("I finished one ", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(48.dp))
        }

        if (state.patterns.isEmpty()) {
            EmptyStateNoPatterns(onAddPattern)
            return@SoftScaffold
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Pattern", style = MaterialTheme.typography.titleLarge)
            state.patterns.forEach { p ->
                PatternRow(
                    pattern = p,
                    selected = selectedPattern?.id == p.id,
                    onClick = {
                        selectedPattern = p
                        selectedColourway = null
                    },
                )
            }

            selectedPattern?.let { pattern ->
                val colourways = state.colourwaysByPattern[pattern.id].orEmpty()
                if (colourways.isNotEmpty()) {
                    Text("Colourway", style = MaterialTheme.typography.titleLarge)
                    colourways.forEach { cw ->
                        ChoiceRow(
                            label = cw.name,
                            selected = selectedColourway?.id == cw.id,
                            onClick = { selectedColourway = cw },
                        )
                    }
                }
            }

            Text("Finished on", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")))
            }

            Text("Photo (optional)", style = MaterialTheme.typography.titleLarge)
            PhotoPicker(
                uri = photoUri,
                onPick = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClear = { photoUri = null },
            )

            Text("How did it feel? (optional)", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("good" to " Good", "ok" to " Okay", "low" to " Low").forEach { (key, label) ->
                    FilterChip(
                        selected = energyTag == key,
                        onClick = { energyTag = if (energyTag == key) null else key },
                        label = { Text(label) },
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )
        }

        Button(
            onClick = {
                val pattern = selectedPattern ?: return@Button
                val cw = selectedColourway ?: return@Button
                viewModel.logCompletion(
                    context = context,
                    pattern = pattern,
                    colourway = cw,
                    date = date,
                    photoUri = photoUri,
                    notes = notes,
                    energyTag = energyTag,
                    onCelebrate = { celebration = it },
                )
            },
            enabled = selectedColourway != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("Celebrate this one ")
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    celebration?.let { data ->
        CelebrationDialog(
            data = data,
            onCreateCard = {
                celebration = null
                onCreateCard(data.completionId)
            },
            onDismiss = {
                celebration = null
                onDone()
            },
        )
    }
}

@Composable
private fun ColumnScope.EmptyStateNoPatterns(onAddPattern: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Add a pattern first", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Once you have at least one pattern with a colourway, you can log a finish here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddPattern) { Text("Add a pattern") }
        }
    }
}

@Composable
private fun PatternRow(pattern: Pattern, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                if (pattern.coverImageUri != null) {
                    AsyncImage(model = pattern.coverImageUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Outlined.Image, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(pattern.name, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun PhotoPicker(uri: Uri?, onPick: () -> Unit, onClear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                )
                FilledTonalButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) { Text("Change") }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onPick) { Text("Add a photo") }
                }
            }
        }
    }
}
