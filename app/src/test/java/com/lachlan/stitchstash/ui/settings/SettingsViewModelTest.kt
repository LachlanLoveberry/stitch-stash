package com.lachlan.stitchstash.ui.settings

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.work.BackupWorker
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        settingsFlow = MutableStateFlow(AppSettings())
        every { repo.observeSettings() } returns settingsFlow
        viewModel = SettingsViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial settings state is the default AppSettings`() = runTest {
        val job = launch { viewModel.settings.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.settings.value).isEqualTo(AppSettings())
        job.cancel()
    }

    @Test
    fun `initial backupUi state is default`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.backupUi.value).isEqualTo(BackupUiState())
    }

    @Test
    fun `update applies transform to current settings and calls repo`() = runTest {
        settingsFlow.value = AppSettings(weeklyHours = 4f, targetPieces = 3)

        viewModel.update { it.copy(weeklyHours = 10f) }
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.updateSettings(match { it.weeklyHours == 10f && it.targetPieces == 3 }) }
    }

    @Test
    fun `update does not pre-validate and passes invalid values straight to repo`() = runTest {
        // The ViewModel has no field validation of its own; repo.updateSettings is the
        // single enforcement point (it throws on NaN/Infinity/negative values). Verify the
        // ViewModel forwards the bad value rather than silently dropping the update, so the
        // repo guard is actually reachable instead of being dead code behind earlier validation.
        viewModel.update { it.copy(weeklyHours = Float.NaN) }
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.updateSettings(match { it.weeklyHours.isNaN() }) }
    }

    @Test
    fun `onSignedIn updates account, enables drive backup, and refreshes backups`() = runTest {
        settingsFlow.value = AppSettings(driveFolderId = null)
        val context = mockk<android.content.Context>(relaxed = true)

        viewModel.onSignedIn(context, "user@example.com")
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.backupUi.value.signedInAccount).isEqualTo("user@example.com")
        assertThat(viewModel.backupUi.value.driveEnabled).isTrue()
        coVerify { repo.updateSettings(match { it.driveBackupEnabled }) }
    }

    @Test
    fun `setFolderId updates settings and local state`() = runTest {
        settingsFlow.value = AppSettings()
        val context = mockk<android.content.Context>(relaxed = true)

        viewModel.setFolderId(context, "folder-123")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.updateSettings(match { it.driveFolderId == "folder-123" }) }
        assertThat(viewModel.backupUi.value.folderId).isEqualTo("folder-123")
    }

    @Test
    fun `backupNow sets a queued message without touching repo settings`() = runTest {
        mockkObject(BackupWorker.Companion)
        every { BackupWorker.enqueueNow(any(), any()) } just Runs
        try {
            val context = mockk<android.content.Context>(relaxed = true)

            viewModel.backupNow(context, "user@example.com")
            dispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.backupUi.value.message).isEqualTo("Backup queued")
        } finally {
            unmockkObject(BackupWorker.Companion)
        }
    }

    @Test
    fun `defaultExportFileName produces a json file name`() {
        val name = viewModel.defaultExportFileName()
        assertThat(name).startsWith("stitch-stash-")
        assertThat(name).endsWith(".json")
    }
}
