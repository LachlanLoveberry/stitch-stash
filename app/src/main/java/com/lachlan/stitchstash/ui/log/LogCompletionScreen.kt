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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.DetailScaffold
import com.lachlan.stitchstash.ui.components.DialogActionRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LocalDateSaver: Saver<LocalDate, Long> = Saver(
    save = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCompletionScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    onAddPattern: () -> Unit,
    onCreateCard: (Long) -> Unit,
    viewModel: LogCompletionViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Pattern/Colourway aren't Parcelable, so the selection is held by id and re-resolved
    // against `state` below — that id survives rotation via rememberSaveable, unlike a plain
    // `remember { mutableStateOf<Pattern?>(null) }` would.
    var selectedPatternId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedColourwayId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedPattern = state.patterns.firstOrNull { it.id == selectedPatternId }
    val selectedColourway = selectedPattern?.let { p ->
        state.colourwaysByPattern[p.id].orEmpty().firstOrNull { it.id == selectedColourwayId }
    }
    var date by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(LocalDate.now()) }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    var energyTag by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddColourway by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf<CelebrationData?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> if (uri != null) photoUri = uri }

    DetailScaffold(title = "I finished one ", onBack = onBack) {
        if (state.patterns.isEmpty()) {
            EmptyStateNoPatterns(onAddPattern)
            return@DetailScaffold
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("Pattern")
            state.patterns.forEach { p ->
                PatternRow(
                    pattern = p,
                    selected = selectedPattern?.id == p.id,
                    onClick = {
                        selectedPatternId = p.id
                        selectedColourwayId = null
                    },
                )
            }

            selectedPattern?.let { pattern ->
                val colourways = state.colourwaysByPattern[pattern.id].orEmpty()
                SectionLabel("Colourway")
                colourways.forEach { cw ->
                    ChoiceRow(
                        label = cw.name,
                        selected = selectedColourway?.id == cw.id,
                        onClick = { selectedColourwayId = cw.id },
                    )
                }
                TextButton(onClick = { showAddColourway = true }) {
                    Text("+ Add colourway")
                }
            }

            SectionLabel("Finished on")
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")))
            }

            SectionLabel("Photo (optional)")
            PhotoPicker(
                uri = photoUri,
                onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onClear = { photoUri = null },
            )

            SectionLabel("How did it feel? (optional)")
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
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
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

    if (showAddColourway) {
        selectedPattern?.let { pattern ->
            AddColourwayDialog(
                onDismiss = { showAddColourway = false },
                onConfirm = { name ->
                    viewModel.addColourway(pattern.id, name) { newId ->
                        selectedColourwayId = newId
                    }
                    showAddColourway = false
                },
            )
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
private fun AddColourwayDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add colourway") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            DialogActionRow {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = { onConfirm(name.trim()) },
                    enabled = name.isNotBlank(),
                ) { Text("Add") }
            }
        },
    )
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
    )
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
            Text(
                "Add a pattern first",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            )
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
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 0.dp else 1.dp,
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun PhotoPicker(uri: Uri?, onPick: () -> Unit, onClear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
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
