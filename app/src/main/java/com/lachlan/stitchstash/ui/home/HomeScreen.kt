package com.lachlan.stitchstash.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.domain.model.CompletionWithContext
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.ProgressRing
import com.lachlan.stitchstash.ui.components.SoftScaffold
import com.lachlan.stitchstash.ui.components.UpdateBanner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onLogFinish: () -> Unit,
    onOpenPatterns: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Stitch Stash", style = MaterialTheme.typography.headlineMedium)
            Row {
                TextButton(onClick = onOpenPatterns) { Text("Patterns") }
                TextButton(onClick = onOpenPlan) { Text("Plan") }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
            }
        }

        UpdateBanner()

        Spacer(Modifier.height(8.dp))

        state.market?.let { market ->
            val date = LocalDate.ofEpochDay(market.dateEpochDay)
            val daysLeft = state.projection?.daysUntilMarket ?: 0
            Text(
                "Next: ${market.name} · ${date.format(DateTimeFormatter.ofPattern("MMM d"))} ($daysLeft days)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ProgressRing(
                fraction = state.fractionComplete,
                completed = state.piecesCompleted,
                planned = state.piecesPlanned,
            )
        }

        if (state.forecastVisible) {
            Spacer(Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.trackingMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Recent wins",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        if (state.recentCompletions.isEmpty()) {
            Text(
                "Your finished pieces will land here. ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            RecentWinsStrip(state.recentCompletions)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onLogFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("I finished one")
        }
    }
}

@Composable
private fun RecentWinsStrip(items: List<CompletionWithContext>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.completion.id }) { item ->
            RecentWinCard(item)
        }
    }
}

@Composable
private fun RecentWinCard(item: CompletionWithContext) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp),
    ) {
        val image = item.completion.photoUri ?: item.pattern.coverImageUri
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
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
        Spacer(Modifier.height(6.dp))
        Text(
            item.pattern.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
        Text(
            item.colourway.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
