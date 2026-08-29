package com.lachlan.stitchstash.ui.patterns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.ConfirmDeleteDialog
import com.lachlan.stitchstash.ui.components.DrawerScaffold
import com.lachlan.stitchstash.ui.components.TopLevelDestination

@Composable
fun PatternListScreen(
    onAddPattern: () -> Unit,
    onNavigate: (TopLevelDestination) -> Unit,
    onEditEstimate: (Long) -> Unit,
    viewModel: PatternListViewModel = viewModel(factory = AppViewModelFactory),
) {
    val patterns by viewModel.patterns.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<PatternWithProgress?>(null) }

    DrawerScaffold(
        title = "Patterns",
        currentRoute = TopLevelDestination.PATTERNS.route,
        onNavigateTopLevel = onNavigate,
    ) {
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
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
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
                itemsIndexed(patterns, key = { _, p -> p.pattern.id }) { index, item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(220, delayMillis = index * 40)) +
                            slideInVertically(animationSpec = tween(260, delayMillis = index * 40)),
                    ) {
                        PatternCard(
                            item,
                            onClick = { onEditEstimate(item.pattern.id) },
                            onLongClick = { pendingDelete = item },
                        )
                    }
                }
            }
            Text(
                "Long-press a pattern to delete it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        pendingDelete?.let { item ->
            ConfirmDeleteDialog(
                itemLabel = "\"${item.pattern.name}\"",
                message = "This removes the pattern, its colourways, and their logged completions. This can't be undone.",
                onConfirm = {
                    viewModel.deletePattern(item.pattern.id)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null },
            )
        }

        Button(
            onClick = onAddPattern,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add pattern", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PatternCard(item: PatternWithProgress, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
            Spacer(Modifier.height(10.dp))
            Text(
                item.pattern.name,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    " ${item.totalCompleted} / ${item.totalTarget} done",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
