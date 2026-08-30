package com.lachlan.stitchstash.ui.onboarding

import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.repository.StitchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        viewModel = OnboardingViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeOnboarding with market date adds a market`() = runTest {
        var doneCalled = false
        val date = LocalDate.of(2026, 6, 1)

        viewModel.completeOnboarding(
            marketName = "Spring Fair",
            marketDate = date,
            weeklyHours = 5f,
            targetPieces = 20,
            onDone = { doneCalled = true },
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.ensureSettingsRow() }
        coVerify { repo.addMarket("Spring Fair", date) }
        coVerify {
            repo.updateSettings(
                match {
                    it.weeklyHours == 5f && it.targetPieces == 20 && it.onboardingComplete
                },
            )
        }
        assert(doneCalled)
    }

    @Test
    fun `completeOnboarding with null market date does not add a market`() = runTest {
        viewModel.completeOnboarding(
            marketName = null,
            marketDate = null,
            weeklyHours = null,
            targetPieces = null,
            onDone = {},
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repo.addMarket(any(), any()) }
        coVerify {
            repo.updateSettings(match { it.onboardingComplete })
        }
    }

    @Test
    fun `completeOnboarding treats blank market name as null`() = runTest {
        val date = LocalDate.of(2026, 6, 1)
        viewModel.completeOnboarding(
            marketName = "   ",
            marketDate = date,
            weeklyHours = null,
            targetPieces = null,
            onDone = {},
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.addMarket(null, date) }
    }

    @Test
    fun `skipOnboarding marks onboarding complete with default settings`() = runTest {
        var doneCalled = false
        coEvery { repo.updateSettings(any()) } returns Unit

        viewModel.skipOnboarding { doneCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.ensureSettingsRow() }
        coVerify { repo.updateSettings(AppSettings(onboardingComplete = true)) }
        assert(doneCalled)
    }
}
