package com.lachlan.stitchstash.ui.stickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.domain.stickers.StickerCatalog
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold

@Composable
fun StickerBookScreen(
    onBack: () -> Unit,
    viewModel: StickerBookViewModel = viewModel(factory = AppViewModelFactory),
) {
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val earnedTypes = stickers.map { it.type }.toSet()
    val earnedCounts = stickers.groupingBy { it.type }.eachCount()
    val allTypes = StickerCatalog.all()

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Sticker book", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${stickers.size} earned · ${allTypes.size - earnedTypes.size} still to find",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(allTypes, key = { it.type }) { def ->
                val earned = def.type in earnedTypes
                val count = earnedCounts[def.type] ?: 0
                StickerTile(
                    emoji = def.emoji,
                    title = def.title,
                    description = def.description,
                    earned = earned,
                    count = count,
                )
            }
        }
    }
}

@Composable
private fun StickerTile(
    emoji: String,
    title: String,
    description: String,
    earned: Boolean,
    count: Int,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (earned) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (earned) 0.dp else 0.dp,
        modifier = Modifier.aspectRatio(0.85f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = CircleShape,
                color = if (earned) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (earned) emoji else "?",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (earned) title else "Locked",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
                if (earned && count > 1) {
                    Text(
                        "×$count",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
