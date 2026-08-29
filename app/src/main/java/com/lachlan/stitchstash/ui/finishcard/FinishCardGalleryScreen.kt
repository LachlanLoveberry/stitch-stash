package com.lachlan.stitchstash.ui.finishcard

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.data.db.entities.FinishCard
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.ConfirmDeleteDialog
import com.lachlan.stitchstash.ui.components.DrawerScaffold
import com.lachlan.stitchstash.ui.components.TopLevelDestination
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinishCardGalleryScreen(
    onNavigate: (TopLevelDestination) -> Unit,
    viewModel: FinishCardViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val cards by viewModel.savedCards.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<FinishCard?>(null) }

    DrawerScaffold(
        title = "Finish cards",
        currentRoute = TopLevelDestination.CARDS.route,
        onNavigateTopLevel = onNavigate,
    ) {
        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No saved cards yet. After you finish a piece, tap 'Make card' to keep one here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(cards, key = { it.id }) { card ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1080f / 1350f)
                            .combinedClickable(
                                onClick = { share(context, card.imagePath) },
                                onLongClick = { pendingDelete = card },
                            ),
                    ) {
                        AsyncImage(
                            model = card.imagePath,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { card ->
        ConfirmDeleteDialog(
            itemLabel = "this finish card",
            onConfirm = {
                viewModel.deleteCard(card.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

private fun share(context: android.content.Context, path: String) {
    val file = File(path)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
