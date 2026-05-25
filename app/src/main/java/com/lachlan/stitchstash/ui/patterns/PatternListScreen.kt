package com.lachlan.stitchstash.ui.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold

@Composable
fun PatternListScreen(
    onAddPattern: () -> Unit,
    onBack: () -> Unit,
    onEditEstimate: (Long) -> Unit,
    viewModel: PatternListViewModel = viewModel(factory = AppViewModelFactory),
) {
    val patterns by viewModel.patterns.collectAsStateWithLifecycle()

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Patterns", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(16.dp))

        if (patterns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No patterns yet",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add the ones you're planning to make.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(patterns, key = { it.pattern.id }) { item ->
                    PatternCard(item, onClick = { onEditEstimate(item.pattern.id) })
                }
            }
        }

        Button(
            onClick = onAddPattern,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add pattern")
        }
    }
}

@Composable
private fun PatternCard(item: PatternWithProgress, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                if (item.pattern.coverImageUri != null) {
                    AsyncImage(
                        model = item.pattern.coverImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.pattern.name, style = MaterialTheme.typography.titleLarge, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.totalCompleted} / ${item.totalTarget} done",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
