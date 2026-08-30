package com.lachlan.stitchstash.ui.plan

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.db.entities.Scenario
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.forecast.ScenarioSolver
import com.lachlan.stitchstash.domain.model.ColourwayProgress
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import io.mockk.coEvery
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stashOf(remaining: Int): List<PatternWithProgress> = listOf(
        PatternWithProgress(
            pattern = Pattern(id = 1L, name = "P", estimateHours = 2f),
            colourways = listOf(
                ColourwayProgress(
                    Colourway(id = 1L, patternId = 1L, name = "C", targetCount = remaining),
                    completedCount = 0,
                ),
            ),
        ),
    )

    private fun repoMock(
        patterns: List<PatternWithProgress> = emptyList(),
        markets: List<Market> = emptyList(),
        scenarios: List<Scenario> = emptyList(),
        settings: AppSettings = AppSettings(),
    ): StitchRepository {
        val repo = mockk<StitchRepository>(relaxed = true)
        every { repo.observePatternsWithProgress() } returns flowOf(patterns)
        every { repo.observeAllMarkets() } returns flowOf(markets)
        every { repo.observeScenarios() } returns flowOf(scenarios)
        every { repo.observeSettings() } returns flowOf(settings)
        return repo
    }

    @Test
    fun `state surfaces patterns markets scenarios and global seed from repo`() = runTest(dispatcher) {
        val patterns = stashOf(10)
        val markets = listOf(Market(id = 1L, name = "Spring Fair", dateEpochDay = 100L))
        val scenarios = listOf(Scenario(id = 5L, name = "Plan A", lockedKey = "HOURS_DATE", isActive = true))
        val repo = repoMock(patterns, markets, scenarios, AppSettings(avgHoursPerPieceSeed = 6f))

        val vm = PlanViewModel(repo)
        val job = launch { vm.state.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.patterns).isEqualTo(patterns)
        assertThat(state.markets).isEqualTo(markets)
        assertThat(state.scenarios).isEqualTo(scenarios)
        assertThat(state.activeScenarioId).isEqualTo(5L)
        assertThat(state.globalSeedHours).isEqualTo(6f)
        job.cancel()
    }

    @Test
    fun `state exposes no active scenario id when none is active`() = runTest(dispatcher) {
        val scenarios = listOf(Scenario(id = 1L, name = "Plan A", lockedKey = "HOURS_DATE", isActive = false))
        val repo = repoMock(scenarios = scenarios)

        val vm = PlanViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.state.value.activeScenarioId).isNull()
    }

    @Test
    fun `solve wires locked key, market date, target pieces and weekly hours into ScenarioSolver`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = PlanViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val snapshot = PlanState(patterns = stashOf(10), globalSeedHours = 4f)
        val today = LocalDate.now()
        val marketDate = today.plusDays(70) // exactly 10 weeks out
        val draft = ScenarioDraft(
            date = marketDate,
            targetPieces = null,
            weeklyHours = 5f,
            lockedKey = ScenarioSolver.LockedKey.HOURS_DATE.storage,
        )

        val result = vm.solve(draft, snapshot)

        // HOURS_DATE locked -> solver computes achievable pieces; 10 weeks * 5h = 50h / 2h per piece = 25,
        // capped by remaining stash of 10.
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.AchievablePieces(10))
    }

    @Test
    fun `solve returns NotEnoughInput when locked inputs are missing`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = PlanViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val draft = ScenarioDraft(
            date = null,
            targetPieces = 10,
            weeklyHours = null,
            lockedKey = ScenarioSolver.LockedKey.DATE_PIECES.storage,
        )

        val result = vm.solve(draft, PlanState())

        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.NotEnoughInput)
    }

    @Test
    fun `saveScenario upserts scenario built from draft and invokes callback with new id`() = runTest(dispatcher) {
        val repo = repoMock()
        coEvery { repo.upsertScenario(any()) } returns 77L
        val vm = PlanViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val date = LocalDate.of(2026, 12, 1)
        val draft = ScenarioDraft(
            id = null,
            name = "Winter Market",
            marketId = null,
            date = date,
            targetPieces = 20,
            weeklyHours = 6f,
            lockedKey = ScenarioSolver.LockedKey.DATE_PIECES.storage,
        )
        val scenarioSlot = slot<Scenario>()
        coEvery { repo.upsertScenario(capture(scenarioSlot)) } returns 77L

        var savedId: Long? = null
        vm.saveScenario(draft) { savedId = it }
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(savedId).isEqualTo(77L)
        assertThat(scenarioSlot.captured.name).isEqualTo("Winter Market")
        assertThat(scenarioSlot.captured.customDateEpochDay).isEqualTo(date.toEpochDay())
        assertThat(scenarioSlot.captured.targetPieces).isEqualTo(20)
        assertThat(scenarioSlot.captured.weeklyHours).isEqualTo(6f)
        assertThat(scenarioSlot.captured.lockedKey).isEqualTo("DATE_PIECES")
    }

    @Test
    fun `saveScenario defaults blank name to Plan and omits custom date when a market is chosen`() = runTest(dispatcher) {
        val repo = repoMock()
        val scenarioSlot = slot<Scenario>()
        coEvery { repo.upsertScenario(capture(scenarioSlot)) } returns 1L
        val vm = PlanViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val draft = ScenarioDraft(
            name = "",
            marketId = 9L,
            date = LocalDate.of(2026, 12, 1),
            lockedKey = ScenarioSolver.LockedKey.HOURS_DATE.storage,
        )
        vm.saveScenario(draft) {}
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(scenarioSlot.captured.name).isEqualTo("Plan")
        assertThat(scenarioSlot.captured.marketId).isEqualTo(9L)
        assertThat(scenarioSlot.captured.customDateEpochDay).isNull()
    }

    @Test
    fun `activate delegates to repo`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = PlanViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.activate(3L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.activateScenario(3L) }
    }

    @Test
    fun `delete delegates to repo`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = PlanViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.delete(3L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.deleteScenario(3L) }
    }
}
