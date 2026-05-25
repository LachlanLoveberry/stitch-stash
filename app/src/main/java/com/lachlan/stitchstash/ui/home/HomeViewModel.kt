package com.lachlan.stitchstash.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.forecast.ForecastEngine
import com.lachlan.stitchstash.domain.model.CompletionWithContext
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeState(
    val market: Market? = null,
    val patterns: List<PatternWithProgress> = emptyList(),
    val recentCompletions: List<CompletionWithContext> = emptyList(),
    val fractionComplete: Float = 0f,
    val piecesCompleted: Int = 0,
    val piecesPlanned: Int = 0,
    val projection: ForecastEngine.Projection? = null,
    val trackingMessage: String = "Add some patterns and start logging — the picture builds itself.",
    val weeklyHours: Float = 4f,
    val targetPieces: Int = 0,
    val forecastVisible: Boolean = true,
)

class HomeViewModel(repo: StitchRepository) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        repo.observeNextMarket(),
        repo.observePatternsWithProgress(),
        repo.observeRecentCompletionsWithContext(limit = 5),
        repo.observeSettings(),
    ) { market, patterns, recent, settings ->
        val today = LocalDate.now()
        val piecesCompleted = ForecastEngine.piecesCompleted(patterns)
        val piecesPlanned = ForecastEngine.piecesPlanned(patterns)
        val fraction = ForecastEngine.fractionComplete(patterns)

        val projection = market?.let {
            ForecastEngine.projectPiecesByDate(
                patterns = patterns,
                marketDate = LocalDate.ofEpochDay(it.dateEpochDay),
                weeklyHours = settings.weeklyHours,
                observedAvgHoursPerPiece = null,
                globalSeed = settings.avgHoursPerPieceSeed,
                today = today,
            )
        }

        val target = if (settings.targetPieces > 0) settings.targetPieces else piecesPlanned
        val message = if (projection != null) {
            ForecastEngine.trackingMessage(projection, target)
        } else {
            "Add a market date in settings to see how things are tracking."
        }

        HomeState(
            market = market,
            patterns = patterns,
            recentCompletions = recent,
            fractionComplete = fraction,
            piecesCompleted = piecesCompleted,
            piecesPlanned = piecesPlanned,
            projection = projection,
            trackingMessage = message,
            weeklyHours = settings.weeklyHours,
            targetPieces = settings.targetPieces,
            forecastVisible = settings.forecastVisible,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
