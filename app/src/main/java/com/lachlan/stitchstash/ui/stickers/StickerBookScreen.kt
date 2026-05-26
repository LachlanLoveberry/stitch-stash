package com.lachlan.stitchstash.ui.stickers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lachlan.stitchstash.domain.stickers.StickerCatalog
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.DrawerScaffold
import com.lachlan.stitchstash.ui.components.TopLevelDestination

@Composable
fun StickerBookScreen(
    onNavigate: (TopLevelDestination) -> Unit,
    viewModel: StickerBookViewModel = viewModel(factory = AppViewModelFactory),
) {
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val earnedTypes = stickers.map { it.type }.toSet()
    val earnedCounts = stickers.groupingBy { it.type }.eachCount()
    val allTypes = StickerCatalog.all()

    DrawerScaffold(
        title = "Stickers",
        currentRoute = TopLevelDestination.STICKERS.route,
        onNavigateTopLevel = onNavigate,
    ) {
        Text(
            "${stickers.size} earned · ${allTypes.size - earnedTypes.size} still to find",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(allTypes, key = { _, d -> d.type }) { index, def ->
                val earned = def.type in earnedTypes
                val count = earnedCounts[def.type] ?: 0
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(220, delayMillis = index * 35)) +
                        scaleIn(initialScale = 0.85f, animationSpec = tween(260, delayMillis = index * 35)),
                ) {
                    StickerTile(
                        emoji = def.emoji,
                        title = def.title,
                        earned = earned,
                        count = count,
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerTile(emoji: String, title: String, earned: Boolean, count: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (earned) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface,
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
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (earned) emoji else "?",
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (earned) title else "Locked",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Serif),
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
