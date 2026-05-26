package com.lachlan.stitchstash.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.domain.model.CompletionWithContext
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.DrawerScaffold
import com.lachlan.stitchstash.ui.components.ProgressRing
import com.lachlan.stitchstash.ui.components.TopLevelDestination
import com.lachlan.stitchstash.ui.components.UpdateBanner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onLogFinish: () -> Unit,
    onNavigate: (TopLevelDestination) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DrawerScaffold(
        title = "Stitch Stash",
        currentRoute = TopLevelDestination.HOME.route,
        onNavigateTopLevel = onNavigate,
    ) {
        UpdateBanner()

        AnimatedVisibility(
            visible = state.market != null,
            enter = fadeIn() + slideInVertically(),
        ) {
            state.market?.let { market ->
                val date = LocalDate.ofEpochDay(market.dateEpochDay)
                val daysLeft = state.projection?.daysUntilMarket ?: 0
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            " ${market.name}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Serif,
                        )
                        Text(
                            "$daysLeft days · ${date.format(DateTimeFormatter.ofPattern("MMM d"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
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

        AnimatedVisibility(
            visible = state.forecastVisible,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
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
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Recent wins",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
            ),
        )
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = state.recentCompletions.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Text(
                "Your finished pieces will land here — every one's worth celebrating. ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = state.recentCompletions.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            RecentWinsStrip(state.recentCompletions)
        }

        Spacer(Modifier.weight(1f))

        BigCelebrateButton(onClick = onLogFinish)
    }
}

@Composable
private fun BigCelebrateButton(onClick: () -> Unit) {
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "ctaPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(top = 4.dp)
            .scale(pulse),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("I finished one ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecentWinsStrip(items: List<CompletionWithContext>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, item -> item.completion.id }) { index, item ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = index * 60)) +
                    slideInVertically(animationSpec = tween(300, delayMillis = index * 60)),
            ) {
                RecentWinCard(item)
            }
        }
    }
}

@Composable
private fun RecentWinCard(item: CompletionWithContext) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp),
    ) {
        val image = item.completion.photoUri ?: item.pattern.coverImageUri
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(84.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (image != null) {
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.pattern.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            item.colourway.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
