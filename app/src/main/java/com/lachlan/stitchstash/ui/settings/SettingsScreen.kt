package com.lachlan.stitchstash.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.lachlan.stitchstash.data.drive.GoogleSignInHelper
import com.lachlan.stitchstash.ui.AppViewModelFactory
import com.lachlan.stitchstash.ui.components.SoftScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMarkets: () -> Unit,
    onOpenStickers: () -> Unit,
    onOpenCards: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupUi by viewModel.backupUi.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val cached = GoogleSignInHelper.currentAccountName(context)
        if (cached != null) viewModel.onSignedIn(context, cached)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.getResult(com.google.android.gms.common.api.ApiException::class.java) }
            .getOrNull()?.email?.let { viewModel.onSignedIn(context, it) }
    }

    var folderInput by remember { mutableStateOf("") }
    var showFolderDialog by remember { mutableStateOf(false) }

    SoftScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard("Plan") {
                LabelledRow(
                    "Weekly hours target",
                    "${(settings.weeklyHours * 2).roundToInt() / 2f} h/wk",
                )
                Slider(
                    value = settings.weeklyHours,
                    onValueChange = { value -> viewModel.update { it.copy(weeklyHours = value) } },
                    valueRange = 0.5f..30f,
                    steps = 58,
                )
                LabelledRow(
                    "Target pieces",
                    if (settings.targetPieces == 0) "—" else settings.targetPieces.toString(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = {
                        viewModel.update { it.copy(targetPieces = (it.targetPieces - 1).coerceAtLeast(0)) }
                    }) { Text("-") }
                    Spacer(Modifier.weight(1f))
                    FilledTonalIconButton(onClick = {
                        viewModel.update { it.copy(targetPieces = it.targetPieces + 1) }
                    }) { Text("+") }
                }
            }

            SectionCard("Display") {
                ToggleRow(
                    label = "Show forecast on home",
                    description = "Hide entirely if you'd like a quiet day.",
                    checked = settings.forecastVisible,
                    onChange = { v -> viewModel.update { it.copy(forecastVisible = v) } },
                )
                ToggleRow(
                    label = "Sticker book",
                    description = null,
                    checked = settings.stickerBookEnabled,
                    onChange = { v -> viewModel.update { it.copy(stickerBookEnabled = v) } },
                )
                ToggleRow(
                    label = "Weekly recap notification",
                    description = "Off by default — opt in when you want it.",
                    checked = settings.weeklyRecapEnabled,
                    onChange = { v -> viewModel.update { it.copy(weeklyRecapEnabled = v) } },
                )
            }

            SectionCard("Markets, stickers & cards") {
                ListRow("Markets", onOpenMarkets)
                ListRow("Sticker book", onOpenStickers)
                ListRow("Finish card gallery", onOpenCards)
            }

            SectionCard("Google Drive backup") {
                if (backupUi.signedInAccount == null) {
                    Text(
                        "Sign in with Google to back up your data. Stitch Stash will only see files it created.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { signInLauncher.launch(GoogleSignInHelper.signInIntent(context)) }) {
                        Text("Sign in with Google")
                    }
                } else {
                    Text("Signed in as ${backupUi.signedInAccount}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Drive folder ID: ${settings.driveFolderId ?: "not set"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showFolderDialog = true }) { Text("Set folder") }
                        Button(
                            onClick = { viewModel.backupNow(context, backupUi.signedInAccount!!) },
                            enabled = settings.driveFolderId != null,
                        ) { Text("Back up now") }
                    }
                    settings.lastBackupAt?.let {
                        Text(
                            "Last backup: ${formatTime(it)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Available backups", style = MaterialTheme.typography.titleLarge)
                    if (backupUi.busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    backupUi.availableBackups.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        formatTime(item.createdAtMillis),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = {
                                    viewModel.restore(context, backupUi.signedInAccount!!, item.id)
                                }) { Text("Restore") }
                            }
                        }
                    }
                    backupUi.message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedButton(onClick = { viewModel.signOut(context) }) { Text("Sign out") }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    viewModel.exportToDevice(context) { path ->
                        // path saved; could surface a share intent here
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export a copy to this device")
                }
            }
        }
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Drive folder ID") },
            text = {
                Column {
                    Text(
                        "Open the shared backup folder in Drive on the web — the URL contains the folder ID after /folders/.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderInput,
                        onValueChange = { folderInput = it },
                        label = { Text("Folder ID") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setFolderId(context, folderInput.trim())
                        showFolderDialog = false
                    },
                    enabled = folderInput.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showFolderDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun LabelledRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ToggleRow(label: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ListRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text("›")
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))
