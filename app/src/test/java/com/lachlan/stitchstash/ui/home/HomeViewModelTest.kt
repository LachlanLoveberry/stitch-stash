package com.lachlan.stitchstash.ui.home

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.db.entities.Sticker
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.model.ColourwayProgress
import com.lachlan.stitchstash.domain.model.CompletionWithContext
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repoMock(
        market: Market? = null,
        patterns: List<PatternWithProgress> = emptyList(),
        recent: List<CompletionWithContext> = emptyList(),
        settings: AppSettings = AppSettings(),
        stickers: List<Sticker> = emptyList(),
        pendingReflection: Market? = null,
    ): StitchRepository {
        val repo = mockk<StitchRepository>(relaxed = true)
        every { repo.observeNextMarket() } returns flowOf(market)
        every { repo.observePatternsWithProgress() } returns flowOf(patterns)
        every { repo.observeRecentCompletionsWithContext(limit = 5) } returns flowOf(recent)
        every { repo.observeSettings() } returns flowOf(settings)
        every { repo.observeStickers() } returns flowOf(stickers)
        every { repo.observeMarketNeedingReflection() } returns flowOf(pendingReflection)
        return repo
    }

    private fun stashOf(target: Int, completed: Int): PatternWithProgress = PatternWithProgress(
        pattern = Pattern(id = 1L, name = "P", estimateHours = 2f),
        colourways = listOf(
            ColourwayProgress(
                Colourway(id = 1L, patternId = 1L, name = "C", targetCount = target),
                completedCount = completed,
            ),
        ),
    )

    @Test
    fun `state surfaces market patterns and progress from repo flows`() = runTest(dispatcher) {
        val market = Market(id = 1L, name = "Fair", dateEpochDay = LocalDate.now().plusDays(30).toEpochDay())
        val pattern = stashOf(target = 10, completed = 4)
        val settings = AppSettings(weeklyHours = 8f, targetPieces = 20, avgHoursPerPieceSeed = 3f)
        val repo = repoMock(market = market, patterns = listOf(pattern), settings = settings)

        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.market).isEqualTo(market)
        assertThat(state.patterns).containsExactly(pattern)
        assertThat(state.piecesCompleted).isEqualTo(4)
        assertThat(state.piecesPlanned).isEqualTo(10)
        assertThat(state.fractionComplete).isEqualTo(0.4f)
        assertThat(state.projection).isNotNull()
        assertThat(state.weeklyHours).isEqualTo(8f)
        assertThat(state.targetPieces).isEqualTo(20)
    }

    @Test
    fun `state has no projection and a fallback message when there is no next market`() = runTest(dispatcher) {
        val repo = repoMock(market = null)

        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.projection).isNull()
        assertThat(state.trackingMessage).isEqualTo("Add a market date in settings to see how things are tracking.")
    }

    @Test
    fun `state surfaces recent and unique sticker counts, capped at six recent`() = runTest(dispatcher) {
        val stickers = (1..8).map { Sticker(id = it.toLong(), type = if (it % 2 == 0) "even" else "odd") }
        val repo = repoMock(stickers = stickers)

        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.recentStickers).hasSize(6)
        assertThat(state.totalStickerCount).isEqualTo(8)
        assertThat(state.uniqueStickerCount).isEqualTo(2)
    }

    @Test
    fun `state respects forecastVisible and stickerBookEnabled settings flags`() = runTest(dispatcher) {
        val repo = repoMock(settings = AppSettings(forecastVisible = false, stickerBookEnabled = false))

        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.forecastVisible).isFalse()
        assertThat(state.stickerBookEnabled).isFalse()
    }

    @Test
    fun `pendingMarketReflection surfaces market needing reflection`() = runTest(dispatcher) {
        val pending = Market(id = 2L, name = "Winter Fair", dateEpochDay = 10L)
        val repo = repoMock(pendingReflection = pending)

        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.pendingMarketReflection.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.pendingMarketReflection.value).isEqualTo(pending)
    }

    @Test
    fun `deleteCompletion delegates to repo`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = HomeViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.deleteCompletion(5L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.deleteCompletion(5L) }
    }

    @Test
    fun `onMarketReflectionDidNotGo dismisses the reflection`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = HomeViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onMarketReflectionDidNotGo(marketId = 9L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.dismissMarketReflection(9L) }
    }

    @Test
    fun `onMarketReflectionSaved forwards details as attended`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = HomeViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onMarketReflectionSaved(marketId = 9L, howItWent = "great", howItFelt = "happy", whatLearned = "prep more")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.saveMarketReflection(9L, attended = true, "great", "happy", "prep more") }
    }

    @Test
    fun `onMarketReflectionSkipped saves attended with null fields`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = HomeViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onMarketReflectionSkipped(marketId = 9L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.saveMarketReflection(9L, attended = true, howItWent = null, howItFelt = null, whatLearned = null) }
    }
}
