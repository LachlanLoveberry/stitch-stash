package com.lachlan.stitchstash.data.drive

import android.accounts.Account
import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Thin wrapper over the Drive REST API using a Google account credential.
 * Operates under the `drive.file` scope — the app only sees files it created.
 *
 * Caller is responsible for obtaining the signed-in account name (via Credential Manager).
 */
class DriveBackupService(
    private val context: Context,
    private val accountName: String,
) {
    private val drive: Drive by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE),
        ).apply {
            selectedAccount = Account(accountName, "com.google")
        }
        Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Stitch Stash")
            .build()
    }

    suspend fun uploadBackup(folderId: String, fileName: String, json: String): String =
        withContext(Dispatchers.IO) {
            val metadata = File().apply {
                name = fileName
                parents = listOf(folderId)
                mimeType = "application/json"
            }
            val content = ByteArrayContent("application/json", json.toByteArray(Charsets.UTF_8))
            val uploaded = drive.files().create(metadata, content)
                .setFields("id")
                .execute()
            uploaded.id
        }

    suspend fun listBackups(folderId: String): List<DriveBackupItem> = withContext(Dispatchers.IO) {
        val files = drive.files().list()
            .setQ("'$folderId' in parents and mimeType = 'application/json' and trashed = false")
            .setOrderBy("createdTime desc")
            .setFields("files(id, name, createdTime, size)")
            .execute()
        files.files.orEmpty().map {
            DriveBackupItem(
                id = it.id,
                name = it.name,
                createdAtMillis = it.createdTime?.value ?: 0L,
                sizeBytes = it.getSize() ?: 0L,
            )
        }
    }

    suspend fun downloadBackup(fileId: String): String = withContext(Dispatchers.IO) {
        val out = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(out)
        String(out.toByteArray(), Charsets.UTF_8)
    }

    suspend fun deleteBackup(fileId: String) = withContext(Dispatchers.IO) {
        drive.files().delete(fileId).execute()
    }

    /** Prune older backups, keeping the most recent `keep` files. */
    suspend fun pruneOlderThan(folderId: String, keep: Int) = withContext(Dispatchers.IO) {
        val items = listBackups(folderId)
        items.drop(keep).forEach { runCatching { deleteBackup(it.id) } }
    }
}

data class DriveBackupItem(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
)
