package com.lachlan.stitchstash.ui.patterns

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.DetailScaffold

@Composable
fun AddPatternScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddPatternViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var designer by remember { mutableStateOf<String?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var preloadedCoverPath by remember { mutableStateOf<String?>(null) }
    var ribblrUrl by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var colourways by remember {
        mutableStateOf(
            listOf(
                ColourwayInput(name = "", targetCount = 1),
                ColourwayInput(name = "", targetCount = 1),
            ),
        )
    }
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            coverUri = uri
            preloadedCoverPath = null
        }
    }

    DetailScaffold(title = "New pattern", onBack = onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = { showImportDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import from Ribblr URL") }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Pattern name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            designer?.let {
                Text(
                    "by $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CoverImagePicker(
                preloadedPath = preloadedCoverPath,
                pickedUri = coverUri,
                onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onClear = {
                    coverUri = null
                    preloadedCoverPath = null
                },
            )

            Text(
                "Colourways",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            )

            colourways.forEachIndexed { index, input ->
                ColourwayRow(
                    input = input,
                    onChange = { updated ->
                        colourways = colourways.toMutableList().also { it[index] = updated }
                    },
                    onRemove = if (colourways.size > 1) {
                        { colourways = colourways.toMutableList().also { it.removeAt(index) } }
                    } else null,
                )
            }

            TextButton(onClick = { colourways = colourways + ColourwayInput() }) {
                Text("+ Another colourway")
            }
        }

        Button(
            onClick = {
                viewModel.save(context, name, coverUri, preloadedCoverPath, ribblrUrl, designer, colourways, onDone)
            },
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 12.dp),
        ) { Text("Save") }
    }

    if (showImportDialog) {
        RibblrImportDialog(
            importState = importState,
            onImport = { url ->
                viewModel.importFromRibblr(context, url) { result ->
                    name = result.name
                    designer = result.designer
                    preloadedCoverPath = result.localCoverPath
                    coverUri = null
                    ribblrUrl = result.sourceUrl
                    showImportDialog = false
                }
            },
            onDismiss = {
                viewModel.dismissError()
                showImportDialog = false
            },
        )
    }
}

@Composable
private fun RibblrImportDialog(
    importState: RibblrImportState,
    onImport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Ribblr URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "We'll grab the cover image and name. You can edit everything after.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("https://ribblr.com/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importState !is RibblrImportState.Loading,
                )
                when (importState) {
                    is RibblrImportState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Fetching…", style = MaterialTheme.typography.bodyMedium)
                    }
                    is RibblrImportState.Error -> Text(
                        "Couldn't read that page (${importState.message}). You can still add the pattern manually.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(url) },
                enabled = url.isNotBlank() && importState !is RibblrImportState.Loading,
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CoverImagePicker(
    preloadedPath: String?,
    pickedUri: Uri?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val model: Any? = preloadedPath ?: pickedUri
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                )
                FilledTonalButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) { Text("Change") }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("Pick a cover photo (optional)")
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onPick) { Text("Choose from gallery") }
                }
            }
        }
    }
}

@Composable
private fun ColourwayRow(
    input: ColourwayInput,
    onChange: (ColourwayInput) -> Unit,
    onRemove: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input.name,
                onValueChange = { onChange(input.copy(name = it)) },
                label = { Text("Colourway (e.g. Pink / Cream)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("How many to make: ", style = MaterialTheme.typography.bodyMedium)
                FilledTonalIconButton(onClick = {
                    if (input.targetCount > 1) onChange(input.copy(targetCount = input.targetCount - 1))
                }) { Text("-") }
                Text(
                    text = input.targetCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = {
                    onChange(input.copy(targetCount = input.targetCount + 1))
                }) { Text("+") }
                Spacer(Modifier.weight(1f))
                if (onRemove != null) {
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}
