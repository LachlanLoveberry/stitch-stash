package com.lachlan.stitchstash.ui.marketprep

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.MarketTodo
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
class MarketPrepViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: StitchRepository
    private lateinit var nextMarketFlow: MutableStateFlow<Market?>
    private lateinit var todosFlow: MutableStateFlow<List<MarketTodo>>
    private lateinit var viewModel: MarketPrepViewModel

    private val market = Market(id = 1L, name = "Spring Fair", dateEpochDay = LocalDate.of(2026, 6, 1).toEpochDay())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        nextMarketFlow = MutableStateFlow(null)
        todosFlow = MutableStateFlow(emptyList())
        every { repo.observeNextMarket() } returns nextMarketFlow
        every { repo.observeMarketTodos(any()) } returns todosFlow
        viewModel = MarketPrepViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no market and no todos when there is no upcoming market`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.state.value).isEqualTo(MarketPrepState())
    }

    @Test
    fun `state reflects the next market and its todos`() = runTest {
        val job = launch { viewModel.state.collect {} }
        val todo = MarketTodo(id = 1L, marketId = market.id, text = "Pack yarn")
        nextMarketFlow.value = market
        dispatcher.scheduler.advanceUntilIdle()
        todosFlow.value = listOf(todo)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value.market).isEqualTo(market)
        assertThat(viewModel.state.value.todos).containsExactly(todo)
        job.cancel()
    }

    @Test
    fun `addTodo trims text and calls repo when a market is loaded`() = runTest {
        val job = launch { viewModel.state.collect {} }
        nextMarketFlow.value = market
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.addTodo("  Pack yarn  ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.addMarketTodo(market.id, "Pack yarn") }
        job.cancel()
    }

    @Test
    fun `addTodo does nothing when text is blank`() = runTest {
        nextMarketFlow.value = market
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.addTodo("   ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repo.addMarketTodo(any(), any()) }
    }

    @Test
    fun `addTodo does nothing when there is no market loaded`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.addTodo("Pack yarn")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repo.addMarketTodo(any(), any()) }
    }

    @Test
    fun `toggle calls repo with the todo`() = runTest {
        val todo = MarketTodo(id = 1L, marketId = market.id, text = "Pack yarn")
        viewModel.toggle(todo)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.toggleMarketTodo(todo) }
    }

    @Test
    fun `delete calls repo with the todo id`() = runTest {
        val todo = MarketTodo(id = 7L, marketId = market.id, text = "Pack yarn")
        viewModel.delete(todo)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.deleteMarketTodo(7L) }
    }
}
