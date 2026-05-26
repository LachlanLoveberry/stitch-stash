package com.lachlan.stitchstash.ui.patterns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.DetailScaffold
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
            Text(
                "${(value * 2).roundToInt() / 2f}h per piece",
                style = MaterialTheme.typography.headlineMedium,
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = 0.5f..30f,
                steps = 58,
                onValueChangeFinished = { onSet(value) },
            )
        }
    }
}
