package com.lachlan.stitchstash.ui.patterns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.AddItemModal
import com.lachlan.stitchstash.ui.components.ConfirmDeleteDialog
import com.lachlan.stitchstash.ui.components.DetailScaffold
import com.lachlan.stitchstash.ui.components.LabeledSliderField
import kotlin.math.roundToInt

@Composable
fun EstimatePatternScreen(
    patternId: Long,
    onBack: () -> Unit,
    viewModel: EstimatePatternViewModel = viewModel(factory = AppViewModelFactory),
) {
    LaunchedEffect(patternId) { viewModel.load(patternId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pattern = state.pattern
    var showAddColourway by remember { mutableStateOf(false) }

    DetailScaffold(title = "How long?", onBack = onBack) {
        if (pattern == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@DetailScaffold
        }
        Text(
            pattern.name,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Pick whichever feels easiest. You can change or skip any time.",
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
            BucketSection(pattern.estimateBucket, viewModel::setBucket)
            SimilarSection(pattern.similarToPatternId, state.allPatterns.filter { it.id != pattern.id }, viewModel::setSimilarTo)
            HoursSection(pattern.estimateHours, viewModel::setHours)
            TextButton(onClick = viewModel::clearEstimate) { Text("Clear estimate (use overall average)") }
            ColourwaysSection(
                state.colourways,
                onAdd = { showAddColourway = true },
                onDelete = viewModel::deleteColourway,
            )
        }
    }

    if (showAddColourway) {
        AddColourwayModal(
            onAdd = { name, targetCount ->
                viewModel.addColourway(name, targetCount)
                showAddColourway = false
            },
            onDismiss = { showAddColourway = false },
        )
    }
}

@Composable
private fun ColourwaysSection(colourways: List<Colourway>, onAdd: () -> Unit, onDelete: (Long) -> Unit) {
    var pendingDelete by remember { mutableStateOf<Colourway?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Colourways",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            )
            if (colourways.isEmpty()) {
                Text(
                    "No colourways yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                colourways.forEach { cw ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${cw.name} · ${cw.targetCount} to make",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { pendingDelete = cw }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete ${cw.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onAdd) { Text("+ Add colourway") }
        }
    }

    pendingDelete?.let { cw ->
        ConfirmDeleteDialog(
            itemLabel = "\"${cw.name}\"",
            message = "This removes the colourway and its logged completions. This can't be undone.",
            onConfirm = {
                onDelete(cw.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun AddColourwayModal(onAdd: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var targetCount by remember { mutableStateOf(1) }

    AddItemModal(
        title = "Add colourway",
        onDismiss = onDismiss,
        onConfirm = { onAdd(name, targetCount) },
        confirmEnabled = name.isNotBlank(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Colourway (e.g. Pink / Cream)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("How many to make: ", style = MaterialTheme.typography.bodyMedium)
                FilledTonalIconButton(onClick = { if (targetCount > 1) targetCount -= 1 }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease how many to make")
                }
                Text(
                    text = targetCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = { targetCount += 1 }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase how many to make")
                }
            }
        }
    }
}

@Composable
private fun BucketSection(current: String?, onPick: (String?) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Rough bucket",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            )
            listOf(
                "quick" to "Quick (under 3h)",
                "evening" to "An evening (3–6h)",
                "project" to "A project (6–12h)",
                "big" to "A big one (12h+)",
            ).forEach { (key, label) ->
                BucketChoice(label, current == key) { onPick(if (current == key) null else key) }
            }
        }
    }
}

@Composable
private fun BucketChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(label, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun SimilarSection(
    currentId: Long?,
    others: List<com.lachlan.stitchstash.data.db.entities.Pattern>,
    onPick: (Long?) -> Unit,
) {
    if (others.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Similar to…",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            )
            others.forEach { p ->
                BucketChoice(p.name, currentId == p.id) { onPick(if (currentId == p.id) null else p.id) }
            }
        }
    }
}

@Composable
private fun HoursSection(current: Float?, onSet: (Float?) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Or set hours directly",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            )
            var value by remember(current) { mutableStateOf(current ?: 4f) }
            LabeledSliderField(
                label = "Hours per piece",
                value = value,
                onValueChange = { value = it },
                valueRange = 0.5f..30f,
                steps = 58,
                valueText = { "${(it * 2).roundToInt() / 2f}h" },
                onValueChangeFinished = { onSet(value) },
            )
        }
    }
}
