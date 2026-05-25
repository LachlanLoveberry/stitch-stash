package com.lachlan.stitchstash.ui.patterns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.repository.StitchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstimateState(
    val pattern: Pattern? = null,
    val allPatterns: List<Pattern> = emptyList(),
)

class EstimatePatternViewModel(private val repo: StitchRepository) : ViewModel() {

    private val patternId = MutableStateFlow<Long?>(null)

    val state: StateFlow<EstimateState> = combine(
        patternId,
        repo.observePatterns(),
    ) { id, patterns ->
        EstimateState(
            pattern = patterns.firstOrNull { it.id == id },
            allPatterns = patterns,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstimateState())

    fun load(id: Long) { patternId.value = id }

    fun setBucket(bucket: String?) {
        val p = state.value.pattern ?: return
        viewModelScope.launch {
            repo.updatePattern(p.copy(estimateBucket = bucket, estimateHours = null, similarToPatternId = null))
        }
    }

    fun setHours(hours: Float?) {
        val p = state.value.pattern ?: return
        viewModelScope.launch {
            repo.updatePattern(p.copy(estimateHours = hours, estimateBucket = null, similarToPatternId = null))
        }
    }

    fun setSimilarTo(otherId: Long?) {
        val p = state.value.pattern ?: return
        viewModelScope.launch {
            repo.updatePattern(p.copy(similarToPatternId = otherId, estimateBucket = null, estimateHours = null))
        }
    }

    fun clearEstimate() {
        val p = state.value.pattern ?: return
        viewModelScope.launch {
            repo.updatePattern(
                p.copy(estimateBucket = null, estimateHours = null, similarToPatternId = null),
            )
        }
    }
}
