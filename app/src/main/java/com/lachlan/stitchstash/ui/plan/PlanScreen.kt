package com.lachlan.stitchstash.ui.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.domain.forecast.ScenarioSolver
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.TopLevelDestination
import com.lachlan.stitchstash.ui.components.DrawerScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Three-question playground. Each question is a sentence with editable chips;
 * the answer is the big bold word in the middle. No locks, no explanation needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onNavigate: (TopLevelDestination) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedQuestion by remember { mutableStateOf(Question.HOW_MANY) }
    var draft by remember { mutableStateOf(ScenarioDraft(name = "Main plan")) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Sync from active scenario once
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
        selectedQuestion = Question.fromLockedKey(active.lockedKey)
    }

    // Mirror the selected question into the draft so the solver picks the right axis
    LaunchedEffect(selectedQuestion) {
        draft = draft.copy(lockedKey = selectedQuestion.lockedKey.storage)
    }

    val solveResult = remember(draft, state) { viewModel.solve(draft, state) }

    DrawerScaffold(
        title = "Plan",
        currentRoute = TopLevelDestination.PLAN.route,
        onNavigateTopLevel = onNavigate,
        actions = {
            TextButton(onClick = { showSaveDialog = true }) { Text("Save") }
        },
    ) {
        QuestionTabs(
            selected = selectedQuestion,
            onSelect = { selectedQuestion = it },
        )

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedQuestion,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(220),
                ) togetherWith fadeOut(animationSpec = tween(160))
            },
            label = "questionContent",
            modifier = Modifier.weight(1f),
        ) { question ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                when (question) {
                    Question.HOW_MANY -> HowManyCard(draft, solveResult) { draft = it }
                    Question.HOW_LONG -> HowLongCard(draft, solveResult) { draft = it }
                    Question.WHEN -> WhenCard(draft, solveResult) { draft = it }
                }

                Spacer(Modifier.height(24.dp))
                SavedScenariosSection(state.scenarios, viewModel)
            }
        }
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

private enum class Question(
    val title: String,
    val short: String,
    val lockedKey: ScenarioSolver.LockedKey,
) {
    HOW_MANY("How many can I make?", "How many?", ScenarioSolver.LockedKey.HOURS_DATE),
    HOW_LONG("How much per week?", "How long?", ScenarioSolver.LockedKey.DATE_PIECES),
    WHEN("When will it be ready?", "When?", ScenarioSolver.LockedKey.HOURS_PIECES),
    ;

    companion object {
        fun fromLockedKey(key: String): Question =
            values().firstOrNull { it.lockedKey.storage == key } ?: HOW_MANY
    }
}

@Composable
private fun QuestionTabs(selected: Question, onSelect: (Question) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Question.values().forEach { q ->
            QuestionTab(
                label = q.short,
                selected = selected == q,
                onClick = { onSelect(q) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuestionTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsStateCompat(
        target = if (selected) MaterialTheme.colorScheme.primary
        else androidx.compose.ui.graphics.Color.Transparent,
    )
    val contentColor by animateColorAsStateCompat(
        target = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabScale",
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ---------- Question cards ---------- //

@Composable
private fun HowManyCard(
    draft: ScenarioDraft,
    result: ScenarioSolver.SolveResult,
    onChange: (ScenarioDraft) -> Unit,
) {
    val answer = (result as? ScenarioSolver.SolveResult.AchievablePieces)?.value ?: 0
    QuestionFrame(title = "How many pieces by market day?") {
        SentenceLine {
            Text("Working ", style = MaterialTheme.typography.titleLarge)
            EditableChip(
                text = "${formatHours(draft.weeklyHours)} h/week",
                onClick = { },
                inline = true,
            ) { dismiss ->
                HoursPicker(
                    initial = draft.weeklyHours ?: 4f,
                    onConfirm = {
                        onChange(draft.copy(weeklyHours = it))
                        dismiss()
                    },
                    onCancel = dismiss,
                )
            }
            Text(", ", style = MaterialTheme.typography.titleLarge)
        }
        SentenceLine {
            Text("by ", style = MaterialTheme.typography.titleLarge)
            DateChip(date = draft.date) { onChange(draft.copy(date = it)) }
            Text("…", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(28.dp))
        AnswerBubble {
            BigNumber(answer)
            Text(
                "pieces",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun HowLongCard(
    draft: ScenarioDraft,
    result: ScenarioSolver.SolveResult,
    onChange: (ScenarioDraft) -> Unit,
) {
    val hours = (result as? ScenarioSolver.SolveResult.HoursPerWeek)?.value ?: 0f
    QuestionFrame(title = "How many hours per week do I need?") {
        SentenceLine {
            Text("To bring ", style = MaterialTheme.typography.titleLarge)
            NumberChip(value = draft.targetPieces ?: 0, suffix = "pieces") {
                onChange(draft.copy(targetPieces = it))
            }
        }
        SentenceLine {
            Text("by ", style = MaterialTheme.typography.titleLarge)
            DateChip(date = draft.date) { onChange(draft.copy(date = it)) }
            Text("…", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(28.dp))
        AnswerBubble {
            Text(
                formatHours(hours),
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "hours / week",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun WhenCard(
    draft: ScenarioDraft,
    result: ScenarioSolver.SolveResult,
    onChange: (ScenarioDraft) -> Unit,
) {
    val finish = (result as? ScenarioSolver.SolveResult.FinishDate)?.value
    QuestionFrame(title = "When will my stash be ready?") {
        SentenceLine {
            Text("Making ", style = MaterialTheme.typography.titleLarge)
            NumberChip(value = draft.targetPieces ?: 0, suffix = "pieces") {
                onChange(draft.copy(targetPieces = it))
            }
        }
        SentenceLine {
            Text("at ", style = MaterialTheme.typography.titleLarge)
            EditableChip(
                text = "${formatHours(draft.weeklyHours)} h/week",
                onClick = { },
                inline = true,
            ) { dismiss ->
                HoursPicker(
                    initial = draft.weeklyHours ?: 4f,
                    onConfirm = {
                        onChange(draft.copy(weeklyHours = it))
                        dismiss()
                    },
                    onCancel = dismiss,
                )
            }
            Text("…", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(28.dp))
        AnswerBubble {
            Text(
                finish?.format(DateTimeFormatter.ofPattern("MMM d")) ?: "—",
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                finish?.format(DateTimeFormatter.ofPattern("yyyy")) ?: "",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
        }
    }
}

// ---------- Sub-components ---------- //

@Composable
private fun QuestionFrame(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SentenceLine(content: @Composable () -> Unit) {
    FlowRowCompat(verticalAlignment = Alignment.CenterVertically) { content() }
}

/** Lightweight FlowRow polyfill that wraps editable chips inline with text. */
@Composable
private fun FlowRowCompat(
    verticalAlignment: Alignment.Vertical,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Composable
private fun AnswerBubble(content: @Composable ColumnScope.() -> Unit) {
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .scale(pulse)
                .padding(vertical = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
private fun BigNumber(value: Int) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 500),
        label = "bigNumber",
    )
    Text(
        animated.toString(),
        style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun EditableChip(
    text: String,
    onClick: () -> Unit,
    inline: Boolean = false,
    sheet: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable {
            open = true
            onClick()
        },
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if (open) sheet { open = false }
}

@Composable
private fun NumberChip(value: Int, suffix: String, onChange: (Int) -> Unit) {
    EditableChip(
        text = "$value $suffix",
        onClick = { },
    ) { dismiss ->
        NumberPickerSheet(
            initial = value,
            label = suffix,
            onConfirm = { onChange(it); dismiss() },
            onCancel = dismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateChip(date: LocalDate?, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable { open = true },
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                date?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "pick a date",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun HoursPicker(
    initial: Float,
    onConfirm: (Float) -> Unit,
    onCancel: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Hours per week") },
        text = {
            Column {
                Text(
                    "${formatHours(value)} hours",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0.5f..30f,
                    steps = 58,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

@Composable
private fun NumberPickerSheet(
    initial: Int,
    label: String,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("How many $label?") },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalIconButton(onClick = { if (value > 0) value -= 1 }) { Text("-") }
                Text(
                    text = if (value == 0) "—" else value.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                FilledTonalIconButton(onClick = { value += 1 }) { Text("+") }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

@Composable
private fun SavedScenariosSection(
    scenarios: List<com.lachlan.stitchstash.data.db.entities.Scenario>,
    viewModel: PlanViewModel,
) {
    Text(
        "Saved plans",
        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
    )
    Spacer(Modifier.height(8.dp))
    if (scenarios.isEmpty()) {
        Text(
            "Tap Save above to keep this configuration.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        scenarios.forEach { sc ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (sc.isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
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

private fun summarise(sc: com.lachlan.stitchstash.data.db.entities.Scenario): String {
    val parts = mutableListOf<String>()
    sc.customDateEpochDay?.let {
        parts += LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM d"))
    }
    sc.targetPieces?.let { parts += "$it pieces" }
    sc.weeklyHours?.let { parts += "${formatHours(it)}h/wk" }
    return parts.joinToString(" · ")
}

@Composable
private fun SaveScenarioDialog(initialName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save plan") },
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

private fun formatHours(h: Float?): String =
    if (h == null) "—" else "${(h * 2).roundToInt() / 2f}"

// Material 3 doesn't expose animateColorAsState directly here — small wrapper
@Composable
private fun animateColorAsStateCompat(target: androidx.compose.ui.graphics.Color) =
    androidx.compose.animation.animateColorAsState(targetValue = target, animationSpec = tween(220), label = "color")
