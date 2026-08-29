package com.lachlan.stitchstash.ui.marketprep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.MarketTodo
import com.lachlan.stitchstash.data.repository.StitchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MarketPrepState(
    val market: Market? = null,
    val todos: List<MarketTodo> = emptyList(),
)

class MarketPrepViewModel(private val repo: StitchRepository) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<MarketPrepState> = repo.observeNextMarket()
        .flatMapLatest { market ->
            if (market == null) {
                flowOf(MarketPrepState())
            } else {
                repo.observeMarketTodos(market.id).combine(flowOf(market)) { todos, m ->
                    MarketPrepState(market = m, todos = todos)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarketPrepState())

    fun addTodo(text: String) {
        val marketId = state.value.market?.id ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.addMarketTodo(marketId, trimmed) }
    }

    fun toggle(todo: MarketTodo) {
        viewModelScope.launch { repo.toggleMarketTodo(todo) }
    }

    fun delete(todo: MarketTodo) {
        viewModelScope.launch { repo.deleteMarketTodo(todo.id) }
    }
}
