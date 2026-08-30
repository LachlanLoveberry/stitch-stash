package com.lachlan.stitchstash.work

import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.db.entities.AppSettings
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the guarded early-return branches of BackupWorker.doWork(), which run before any
 * network call. The upload path itself constructs DriveBackupService directly (not injected),
 * so it isn't reachable from a unit test without a real/faked Drive backend — that path belongs
 * in the androidTest/e2e phase instead.
 */
@RunWith(RobolectricTestRunner::class)
class BackupWorkerTest {

    private val app: StitchStashApp
        get() = ApplicationProvider.getApplicationContext()

    private fun buildWorker(accountName: String? = "user@example.com") =
        TestListenableWorkerBuilder<BackupWorker>(app)
            .setInputData(
                Data.Builder().apply {
                    if (accountName != null) putString(BackupWorker.KEY_ACCOUNT_NAME, accountName)
                }.build(),
            )
            .build()

    @Test
    fun `doWork succeeds without a network call when drive backup is disabled`() = runTest {
        app.repository.updateSettings(AppSettings(driveBackupEnabled = false))

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork succeeds without a network call when no drive folder is configured`() = runTest {
        app.repository.updateSettings(AppSettings(driveBackupEnabled = true, driveFolderId = null))

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork fails when account name is missing from input data`() = runTest {
        app.repository.updateSettings(AppSettings(driveBackupEnabled = true, driveFolderId = "folder-1"))

        val result = buildWorker(accountName = null).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
