package com.lachlan.stitchstash.ui.markets

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.repository.StitchRepository
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
class MarketsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository
    private lateinit var marketsFlow: MutableStateFlow<List<Market>>
    private lateinit var viewModel: MarketsViewModel

    private fun market(id: Long = 1L, name: String? = "Spring Fair", date: LocalDate = LocalDate.of(2026, 6, 1)) =
        Market(id = id, name = name, dateEpochDay = date.toEpochDay())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        marketsFlow = MutableStateFlow(emptyList())
        every { repo.observeAllMarkets() } returns marketsFlow
        viewModel = MarketsViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty list`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.markets.value).isEmpty()
    }

    @Test
    fun `state reflects markets emitted by repo`() = runTest {
        val job = launch { viewModel.markets.collect {} }
        val m = market()
        marketsFlow.value = listOf(m)
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.markets.value).containsExactly(m)
        job.cancel()
    }

    @Test
    fun `add trims name and calls repo`() = runTest {
        val date = LocalDate.of(2026, 7, 1)
        viewModel.add("  Summer Fair  ", date)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.addMarket("Summer Fair", date) }
    }

    @Test
    fun `add with blank name passes null`() = runTest {
        val date = LocalDate.of(2026, 7, 1)
        viewModel.add("   ", date)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.addMarket(null, date) }
    }

    @Test
    fun `toggleSkipped flips isSkipped`() = runTest {
        val m = market()
        viewModel.toggleSkipped(m)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.updateMarket(m.copy(isSkipped = true)) }
    }

    @Test
    fun `rename trims new name`() = runTest {
        val m = market()
        viewModel.rename(m, "  New Name  ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.updateMarket(m.copy(name = "New Name")) }
    }

    @Test
    fun `rename with blank name sets null`() = runTest {
        val m = market()
        viewModel.rename(m, "   ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.updateMarket(m.copy(name = null)) }
    }

    @Test
    fun `reschedule updates dateEpochDay`() = runTest {
        val m = market()
        val newDate = LocalDate.of(2026, 9, 1)
        viewModel.reschedule(m, newDate)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.updateMarket(m.copy(dateEpochDay = newDate.toEpochDay())) }
    }

    @Test
    fun `delete calls repo with market id`() = runTest {
        val m = market(id = 42L)
        viewModel.delete(m)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.deleteMarket(42L) }
    }
}
