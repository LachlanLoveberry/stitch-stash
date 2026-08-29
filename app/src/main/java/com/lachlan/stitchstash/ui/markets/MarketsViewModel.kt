package com.lachlan.stitchstash.ui.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.repository.StitchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MarketsViewModel(private val repo: StitchRepository) : ViewModel() {

    val markets: StateFlow<List<Market>> =
        repo.observeAllMarkets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, date: LocalDate) {
        viewModelScope.launch { repo.addMarket(name.trim().takeIf { it.isNotBlank() }, date) }
    }

    fun toggleSkipped(market: Market) {
        viewModelScope.launch { repo.updateMarket(market.copy(isSkipped = !market.isSkipped)) }
    }

    fun rename(market: Market, newName: String) {
        viewModelScope.launch {
            repo.updateMarket(market.copy(name = newName.trim().takeIf { it.isNotBlank() }))
        }
    }

    fun reschedule(market: Market, newDate: LocalDate) {
        viewModelScope.launch { repo.updateMarket(market.copy(dateEpochDay = newDate.toEpochDay())) }
    }

    fun delete(market: Market) {
        viewModelScope.launch { repo.deleteMarket(market.id) }
    }
}
