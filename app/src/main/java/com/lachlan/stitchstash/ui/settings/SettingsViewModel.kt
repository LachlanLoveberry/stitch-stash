package com.lachlan.stitchstash.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.backup.BackupSerializer
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.drive.DriveBackupItem
import com.lachlan.stitchstash.data.drive.DriveBackupService
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.work.BackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupUiState(
    val signedInAccount: String? = null,
    val folderId: String? = null,
    val lastBackupAt: Long? = null,
    val driveEnabled: Boolean = false,
    val availableBackups: List<DriveBackupItem> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(private val repo: StitchRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repo.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _backupUi = MutableStateFlow(BackupUiState())
    val backupUi: StateFlow<BackupUiState> = _backupUi.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = repo.observeSettings().first()
            repo.updateSettings(transform(current))
        }
    }

    fun onSignedIn(context: Context, accountName: String) {
        _backupUi.value = _backupUi.value.copy(signedInAccount = accountName, message = null)
        viewModelScope.launch {
            val current = repo.observeSettings().first()
            repo.updateSettings(current.copy(driveBackupEnabled = true))
            _backupUi.value = _backupUi.value.copy(driveEnabled = true)
            refreshBackups(context, accountName, current.driveFolderId)
        }
    }

    fun setFolderId(context: Context, folderId: String) {
        viewModelScope.launch {
            val current = repo.observeSettings().first()
            repo.updateSettings(current.copy(driveFolderId = folderId))
            _backupUi.value = _backupUi.value.copy(folderId = folderId)
            _backupUi.value.signedInAccount?.let { refreshBackups(context, it, folderId) }
        }
    }

    fun refreshBackups(context: Context, accountName: String, folderId: String?) {
        if (folderId.isNullOrBlank()) return
        viewModelScope.launch {
            _backupUi.value = _backupUi.value.copy(busy = true)
            runCatching {
                DriveBackupService(context, accountName).listBackups(folderId)
            }.onSuccess { items ->
                _backupUi.value = _backupUi.value.copy(availableBackups = items, busy = false)
            }.onFailure {
                _backupUi.value = _backupUi.value.copy(
                    busy = false,
                    message = "Couldn't list backups: ${it.message}",
                )
            }
        }
    }

    fun backupNow(context: Context, accountName: String) {
        BackupWorker.enqueueNow(context, accountName)
        _backupUi.value = _backupUi.value.copy(message = "Backup queued")
    }

    fun restore(context: Context, accountName: String, fileId: String) {
        viewModelScope.launch {
            _backupUi.value = _backupUi.value.copy(busy = true, message = null)
            runCatching {
                val app = context.applicationContext as StitchStashApp
                val json = DriveBackupService(context, accountName).downloadBackup(fileId)
                BackupSerializer.import(app.database, json)
            }.onSuccess {
                _backupUi.value = _backupUi.value.copy(busy = false, message = "Restored.")
            }.onFailure {
                _backupUi.value = _backupUi.value.copy(busy = false, message = "Restore failed: ${it.message}")
            }
        }
    }

    fun defaultExportFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
        return "stitch-stash-$date.json"
    }

    fun exportToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupUi.value = _backupUi.value.copy(busy = true, message = null)
            runCatching {
                val app = context.applicationContext as StitchStashApp
                val json = BackupSerializer.export(app.database)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("Couldn't open the chosen file for writing")
            }.onSuccess {
                _backupUi.value = _backupUi.value.copy(busy = false, message = "Exported.")
            }.onFailure {
                _backupUi.value = _backupUi.value.copy(busy = false, message = "Export failed: ${it.message}")
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupUi.value = _backupUi.value.copy(busy = true, message = null)
            runCatching {
                val app = context.applicationContext as StitchStashApp
                val json = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                    ?: error("Couldn't open the chosen file")
                BackupSerializer.import(app.database, json)
            }.onSuccess { summary ->
                _backupUi.value = _backupUi.value.copy(
                    busy = false,
                    message = "Imported ${summary.patterns} patterns, ${summary.completions} completions, ${summary.markets} markets.",
                )
            }.onFailure {
                _backupUi.value = _backupUi.value.copy(busy = false, message = "Import failed: ${it.message}")
            }
        }
    }

    fun signOut(context: Context) {
        com.lachlan.stitchstash.data.drive.GoogleSignInHelper.signOut(context) {
            _backupUi.value = BackupUiState()
            viewModelScope.launch {
                val current = repo.observeSettings().first()
                repo.updateSettings(current.copy(driveBackupEnabled = false))
            }
        }
    }
}
