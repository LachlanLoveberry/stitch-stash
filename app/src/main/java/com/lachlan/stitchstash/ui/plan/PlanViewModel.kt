package com.lachlan.stitchstash.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.Scenario
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.forecast.ScenarioSolver
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanState(
    val patterns: List<PatternWithProgress> = emptyList(),
    val markets: List<Market> = emptyList(),
    val scenarios: List<Scenario> = emptyList(),
    val activeScenarioId: Long? = null,
    val globalSeedHours: Float = 4f,
)

class PlanViewModel(private val repo: StitchRepository) : ViewModel() {

    val state: StateFlow<PlanState> = combine(
        repo.observePatternsWithProgress(),
        repo.observeAllMarkets(),
        repo.observeScenarios(),
        repo.observeSettings(),
    ) { patterns, markets, scenarios, settings ->
        PlanState(
            patterns = patterns,
            markets = markets,
            scenarios = scenarios,
            activeScenarioId = scenarios.firstOrNull { it.isActive }?.id,
            globalSeedHours = settings.avgHoursPerPieceSeed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanState())

    fun solve(scenario: ScenarioDraft, snapshot: PlanState): ScenarioSolver.SolveResult {
        val inputs = ScenarioSolver.Inputs(
            marketDate = scenario.date,
            targetPieces = scenario.targetPieces,
            weeklyHours = scenario.weeklyHours,
            locked = ScenarioSolver.LockedKey.from(scenario.lockedKey),
        )
        return ScenarioSolver.solve(
            inputs = inputs,
            patterns = snapshot.patterns,
            observedAvgHoursPerPiece = null,
            globalSeed = snapshot.globalSeedHours,
            today = LocalDate.now(),
        )
    }

    fun saveScenario(draft: ScenarioDraft, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.upsertScenario(
                Scenario(
                    id = draft.id ?: 0L,
                    name = draft.name.ifBlank { "Plan" },
                    marketId = draft.marketId,
                    customDateEpochDay = if (draft.marketId == null) draft.date?.toEpochDay() else null,
                    targetPieces = draft.targetPieces,
                    weeklyHours = draft.weeklyHours,
                    lockedKey = draft.lockedKey,
                ),
            )
            onSaved(id)
        }
    }

    fun activate(id: Long) {
        viewModelScope.launch { repo.activateScenario(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteScenario(id) }
    }
}

data class ScenarioDraft(
    val id: Long? = null,
    val name: String = "",
    val marketId: Long? = null,
    val date: LocalDate? = null,
    val targetPieces: Int? = null,
    val weeklyHours: Float? = null,
    val lockedKey: String = ScenarioSolver.LockedKey.HOURS_DATE.storage,
)
