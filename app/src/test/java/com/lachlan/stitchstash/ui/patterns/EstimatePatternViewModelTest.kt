package com.lachlan.stitchstash.ui.patterns

import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.repository.StitchRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
class EstimatePatternViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository
    private lateinit var viewModel: EstimatePatternViewModel

    private val pattern1 = Pattern(id = 1L, name = "Pattern One")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        every { repo.observePatterns() } returns flowOf(listOf(pattern1))
        every { repo.observeColourwaysForPattern(1L) } returns flowOf(emptyList())
        viewModel = EstimatePatternViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        assert(viewModel.state.value.pattern == null)
        assert(viewModel.state.value.allPatterns.isEmpty())
        assert(viewModel.state.value.colourways.isEmpty())
    }

    @Test
    fun `load selects pattern from allPatterns`() = runTest {
        val job = launch { viewModel.state.collect {} }
        viewModel.load(1L)
        dispatcher.scheduler.advanceUntilIdle()

        assert(viewModel.state.value.pattern == pattern1)
        job.cancel()
    }

    @Test
    fun `addColourway trims name and coerces negative targetCount to zero`() = runTest {
        viewModel.load(1L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.addColourway("  Blue  ", -5)
        dispatcher.scheduler.advanceUntilIdle()

        val slot = slot<Colourway>()
        coVerify { repo.addColourway(capture(slot)) }
        assert(slot.captured.name == "Blue")
        assert(slot.captured.patternId == 1L)
        assert(slot.captured.targetCount == 0)
    }

    @Test
    fun `addColourway with blank name after trim does nothing`() = runTest {
        viewModel.load(1L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.addColourway("   ", 5)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repo.addColourway(any()) }
    }

    @Test
    fun `addColourway does nothing when no pattern is loaded`() = runTest {
        viewModel.addColourway("Green", 3)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repo.addColourway(any()) }
    }

    @Test
    fun `addColourway preserves positive targetCount unchanged`() = runTest {
        viewModel.load(1L)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.addColourway("Yellow", 8)
        dispatcher.scheduler.advanceUntilIdle()

        val slot = slot<Colourway>()
        coVerify { repo.addColourway(capture(slot)) }
        assert(slot.captured.targetCount == 8)
    }
}
