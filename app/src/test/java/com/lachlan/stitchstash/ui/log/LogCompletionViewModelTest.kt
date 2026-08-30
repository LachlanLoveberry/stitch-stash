package com.lachlan.stitchstash.ui.log

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.model.ColourwayProgress
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LogCompletionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository
    private lateinit var patternsFlow: MutableStateFlow<List<Pattern>>
    private lateinit var withProgressFlow: MutableStateFlow<List<PatternWithProgress>>
    private lateinit var viewModel: LogCompletionViewModel

    private val pattern = Pattern(id = 1L, name = "Bunny")
    private val colourway = Colourway(id = 1L, patternId = 1L, name = "Pink", targetCount = 5)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        patternsFlow = MutableStateFlow(emptyList())
        withProgressFlow = MutableStateFlow(emptyList())
        every { repo.observePatterns() } returns patternsFlow
        every { repo.observePatternsWithProgress() } returns withProgressFlow
        viewModel = LogCompletionViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.state.value).isEqualTo(LogState())
    }

    @Test
    fun `state combines patterns with colourways by pattern id`() = runTest {
        val job = launch { viewModel.state.collect {} }
        patternsFlow.value = listOf(pattern)
        withProgressFlow.value = listOf(
            PatternWithProgress(
                pattern = pattern,
                colourways = listOf(ColourwayProgress(colourway, completedCount = 2)),
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.patterns).containsExactly(pattern)
        assertThat(state.colourwaysByPattern[pattern.id]).containsExactly(colourway)
        job.cancel()
    }

    @Test
    fun `logCompletion with no photo persists nothing and calls repo then invokes onCelebrate`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val date = LocalDate.of(2026, 6, 1)
        val result = StitchRepository.CompletionWithEarnings(
            completion = Completion(id = 9L, colourwayId = colourway.id, completedAtEpochDay = date.toEpochDay()),
            earned = emptyList(),
        )
        coEvery { repo.addCompletionAndEarnStickers(any()) } returns result

        var celebration: CelebrationData? = null
        viewModel.logCompletion(
            context = context,
            pattern = pattern,
            colourway = colourway,
            date = date,
            photoUri = null,
            notes = "  ",
            energyTag = "good",
            onCelebrate = { celebration = it },
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repo.addCompletionAndEarnStickers(
                match {
                    it.colourwayId == colourway.id &&
                        it.completedAtEpochDay == date.toEpochDay() &&
                        it.photoUri == null &&
                        it.notes == null &&
                        it.energyTag == "good"
                },
            )
        }
        assertThat(celebration).isNotNull()
        assertThat(celebration?.completionId).isEqualTo(9L)
        assertThat(celebration?.patternName).isEqualTo(pattern.name)
        assertThat(celebration?.colourwayName).isEqualTo(colourway.name)
        assertThat(celebration?.photoPath).isNull()
        assertThat(celebration?.stickers).isEmpty()
    }

    @Test
    fun `logCompletion keeps non-blank notes`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val date = LocalDate.of(2026, 6, 1)
        val result = StitchRepository.CompletionWithEarnings(
            completion = Completion(id = 1L, colourwayId = colourway.id, completedAtEpochDay = date.toEpochDay()),
            earned = emptyList(),
        )
        coEvery { repo.addCompletionAndEarnStickers(any()) } returns result

        viewModel.logCompletion(
            context = context,
            pattern = pattern,
            colourway = colourway,
            date = date,
            photoUri = null,
            notes = "Great day",
            energyTag = null,
            onCelebrate = {},
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repo.addCompletionAndEarnStickers(match { it.notes == "Great day" })
        }
    }
}
