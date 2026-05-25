package com.lachlan.stitchstash.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.backup.BackupSerializer
import com.lachlan.stitchstash.data.drive.DriveBackupService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as StitchStashApp
        val settings = app.repository.observeSettings().first()
        if (!settings.driveBackupEnabled) return Result.success()
        val folderId = settings.driveFolderId ?: return Result.success()
        val accountName = inputData.getString(KEY_ACCOUNT_NAME) ?: return Result.failure()

        return runCatching {
            val json = BackupSerializer.export(app.database)
            val service = DriveBackupService(applicationContext, accountName)
            val date = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
            service.uploadBackup(folderId, "stitch-stash-$date.json", json)
            service.pruneOlderThan(folderId, keep = 30)
            app.repository.updateSettings(settings.copy(lastBackupAt = System.currentTimeMillis()))
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_ACCOUNT_NAME = "account_name"
        private const val UNIQUE = "stitch_stash_backup"

        fun enqueueDebounced(context: Context, accountName: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(constraints)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setInputData(androidx.work.Data.Builder().putString(KEY_ACCOUNT_NAME, accountName).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.REPLACE, request)
        }

        fun enqueueNow(context: Context, accountName: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(constraints)
                .setInputData(androidx.work.Data.Builder().putString(KEY_ACCOUNT_NAME, accountName).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE + "_now", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
