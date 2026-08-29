package com.lachlan.stitchstash.ui.marketprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.data.db.entities.MarketTodo
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.DrawerScaffold
import com.lachlan.stitchstash.ui.components.TopLevelDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketPrepScreen(
    onNavigate: (TopLevelDestination) -> Unit,
    viewModel: MarketPrepViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var newTodo by remember { mutableStateOf("") }

    DrawerScaffold(
        title = "Market prep",
        currentRoute = TopLevelDestination.MARKET_PREP.route,
        onNavigateTopLevel = onNavigate,
    ) {
        val market = state.market
        if (market == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Add an upcoming market to start a prep list — think beyond stock: signage, float, packaging, anything you don't want to scramble for.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@DrawerScaffold
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                market.name ?: "Your market",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            )
            Text(
                LocalDate.ofEpochDay(market.dateEpochDay).format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newTodo,
                onValueChange = { newTodo = it },
                placeholder = { Text("Add something to prep — signage, float, packaging...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                viewModel.addTodo(newTodo)
                newTodo = ""
            }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.todos.isEmpty()) {
            Text(
                "Nothing on the list yet — beyond how many pieces you're bringing, what else does this market need?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.todos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onToggle = { viewModel.toggle(todo) },
                        onDelete = { viewModel.delete(todo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: MarketTodo, onToggle: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = todo.isDone, onCheckedChange = { onToggle() })
            Text(
                todo.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                color = if (todo.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
