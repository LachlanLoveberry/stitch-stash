package com.lachlan.stitchstash.ui.patterns

import android.content.Context
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.repository.StitchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddPatternViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository
    private lateinit var viewModel: AddPatternViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        context = mockk(relaxed = true)
        viewModel = AddPatternViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial import state is Idle`() = runTest {
        assert(viewModel.importState.value is RibblrImportState.Idle)
    }

    @Test
    fun `save with valid name adds pattern and trims name`() = runTest {
        coEvery { repo.addPattern(any()) } returns 42L

        var doneCalled = false
        viewModel.save(
            context = context,
            name = "  My Pattern  ",
            coverUri = null,
            preloadedCoverPath = null,
            ribblrUrl = null,
            designer = null,
            colourwayInputs = emptyList(),
            onDone = { doneCalled = true },
        )
        dispatcher.scheduler.advanceUntilIdle()

        val slot = slot<Pattern>()
        coVerify { repo.addPattern(capture(slot)) }
        assert(slot.captured.name == "My Pattern")
        assert(doneCalled)
    }

    @Test
    fun `save with blank name does nothing`() = runTest {
        var doneCalled = false
        viewModel.save(
            context = context,
            name = "   ",
            coverUri = null,
            preloadedCoverPath = null,
            ribblrUrl = null,
            designer = null,
            colourwayInputs = emptyList(),
            onDone = { doneCalled = true },
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repo.addPattern(any()) }
        assert(!doneCalled)
    }

    @Test
    fun `save filters out blank colourway names and adds valid ones with coerced target`() = runTest {
        coEvery { repo.addPattern(any()) } returns 7L

        viewModel.save(
            context = context,
            name = "Pattern",
            coverUri = null,
            preloadedCoverPath = null,
            ribblrUrl = null,
            designer = null,
            colourwayInputs = listOf(
                ColourwayInput(name = "  ", targetCount = 5),
                ColourwayInput(name = "Red", targetCount = 0),
            ),
            onDone = {},
        )
        dispatcher.scheduler.advanceUntilIdle()

        val slot = slot<Colourway>()
        coVerify(exactly = 1) { repo.addColourway(capture(slot)) }
        assert(slot.captured.name == "Red")
        assert(slot.captured.patternId == 7L)
        assert(slot.captured.targetCount == 1)
    }

    @Test
    fun `importFromRibblr with blank url does nothing`() = runTest {
        viewModel.importFromRibblr(context, "   ") {}
        dispatcher.scheduler.advanceUntilIdle()

        assert(viewModel.importState.value is RibblrImportState.Idle)
    }

    @Test
    fun `dismissError resets state to Idle`() = runTest {
        viewModel.dismissError()
        assert(viewModel.importState.value is RibblrImportState.Idle)
    }
}
