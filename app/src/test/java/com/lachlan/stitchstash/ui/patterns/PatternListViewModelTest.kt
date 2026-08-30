package com.lachlan.stitchstash.ui.patterns

import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatternListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository

    private val progress = PatternWithProgress(
        pattern = Pattern(id = 1L, name = "Pattern One"),
        colourways = emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial patterns state reflects repository flow`() = runTest {
        every { repo.observePatternsWithProgress() } returns flowOf(listOf(progress))
        val viewModel = PatternListViewModel(repo)
        val job = launch { viewModel.patterns.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        assert(viewModel.patterns.value == listOf(progress))
        job.cancel()
    }

    @Test
    fun `initial patterns state is empty when repository has nothing`() = runTest {
        every { repo.observePatternsWithProgress() } returns flowOf(emptyList())
        val viewModel = PatternListViewModel(repo)
        val job = launch { viewModel.patterns.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        assert(viewModel.patterns.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `deletePattern calls repository with given id`() = runTest {
        every { repo.observePatternsWithProgress() } returns flowOf(emptyList())
        val viewModel = PatternListViewModel(repo)

        viewModel.deletePattern(5L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.deletePattern(5L) }
    }
}
