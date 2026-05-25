package com.lachlan.stitchstash.ui.finishcard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.FinishCard
import com.lachlan.stitchstash.data.repository.StitchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardPreviewState(
    val rendering: Boolean = false,
    val imagePath: String? = null,
    val border: BorderStyle = BorderStyle.FLORAL,
    val patternName: String = "",
    val colourwayName: String = "",
    val pieceNumber: Int? = null,
    val photoPath: String? = null,
    val saved: Boolean = false,
)

class FinishCardViewModel(private val repo: StitchRepository) : ViewModel() {

    private val _preview = MutableStateFlow(CardPreviewState())
    val preview: StateFlow<CardPreviewState> = _preview.asStateFlow()

    val savedCards: StateFlow<List<FinishCard>> =
        repo.observeFinishCards()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun prepare(context: Context, completionId: Long) {
        viewModelScope.launch {
            val completions = repo.observeRecentCompletionsWithContext(limit = 50).first()
            val ctx = completions.firstOrNull { it.completion.id == completionId } ?: return@launch
            val pieceNumber = repo.observeTotalCompletionCount().first()
            val border = BorderStyle.from(repo.observeSettings().first().finishCardBorderStyle)

            _preview.value = CardPreviewState(
                rendering = true,
                border = border,
                patternName = ctx.pattern.name,
                colourwayName = ctx.colourway.name,
                pieceNumber = pieceNumber,
                photoPath = ctx.completion.photoUri ?: ctx.pattern.coverImageUri,
            )
            renderAndUpdate(context, completionId)
        }
    }

    fun changeBorder(context: Context, completionId: Long, border: BorderStyle) {
        _preview.value = _preview.value.copy(border = border, saved = false, rendering = true)
        viewModelScope.launch {
            repo.updateSettings(
                repo.observeSettings().first().copy(finishCardBorderStyle = border.key),
            )
            renderAndUpdate(context, completionId)
        }
    }

    private suspend fun renderAndUpdate(context: Context, completionId: Long) {
        val current = _preview.value
        val path = FinishCardRenderer.render(
            context,
            FinishCardSpec(
                photoPath = current.photoPath,
                patternName = current.patternName,
                colourwayName = current.colourwayName,
                pieceNumber = current.pieceNumber,
                border = current.border,
            ),
        )
        _preview.value = current.copy(imagePath = path, rendering = false)
    }

    fun saveCurrent(completionId: Long) {
        val current = _preview.value
        val path = current.imagePath ?: return
        viewModelScope.launch {
            repo.addFinishCard(
                FinishCard(
                    completionId = completionId,
                    imagePath = path,
                    borderStyle = current.border.key,
                ),
            )
            _preview.value = current.copy(saved = true)
        }
    }
}
