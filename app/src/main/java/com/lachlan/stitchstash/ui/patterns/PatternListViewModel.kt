package com.lachlan.stitchstash.ui.patterns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatternListViewModel(private val repo: StitchRepository) : ViewModel() {
    val patterns: StateFlow<List<PatternWithProgress>> =
        repo.observePatternsWithProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deletePattern(id: Long) {
        viewModelScope.launch { repo.deletePattern(id) }
    }
}
