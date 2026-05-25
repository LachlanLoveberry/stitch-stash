package com.lachlan.stitchstash.ui.finishcard

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold
import java.io.File

@Composable
fun FinishCardScreen(
    completionId: Long,
    onBack: () -> Unit,
    viewModel: FinishCardViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val state by viewModel.preview.collectAsStateWithLifecycle()

    LaunchedEffect(completionId) {
        viewModel.prepare(context, completionId)
    }

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Finish card", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1080f / 1350f),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (state.imagePath != null) {
                    AsyncImage(
                        model = state.imagePath,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
                if (state.rendering) {
                    CircularProgressIndicator()
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Border style", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BorderStyle.values().forEach { style ->
                FilterChip(
                    selected = state.border == style,
                    onClick = { viewModel.changeBorder(context, completionId, style) },
                    label = { Text(style.label) },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val path = state.imagePath ?: return@OutlinedButton
                    viewModel.saveCurrent(completionId)
                    sharePng(context, path)
                },
                enabled = state.imagePath != null && !state.rendering,
                modifier = Modifier.weight(1f),
            ) { Text("Share") }
            Button(
                onClick = { viewModel.saveCurrent(completionId) },
                enabled = state.imagePath != null && !state.rendering && !state.saved,
                modifier = Modifier.weight(1f),
            ) { Text(if (state.saved) "Saved " else "Save to gallery") }
        }
    }
}

private fun sharePng(context: android.content.Context, path: String) {
    val file = File(path)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share finish card"))
}
