package com.lachlan.stitchstash.ui.log

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.db.entities.Sticker
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.data.storage.ImageStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LogState(
    val patterns: List<Pattern> = emptyList(),
    val colourwaysByPattern: Map<Long, List<Colourway>> = emptyMap(),
)

data class CelebrationData(
    val completionId: Long,
    val patternName: String,
    val colourwayName: String,
    val photoPath: String?,
    val stickers: List<Sticker>,
)

class LogCompletionViewModel(private val repo: StitchRepository) : ViewModel() {

    val state: StateFlow<LogState> = combine(
        repo.observePatterns(),
        repo.observePatternsWithProgress(),
    ) { patterns, withProgress ->
        LogState(
            patterns = patterns,
            colourwaysByPattern = withProgress.associate { it.pattern.id to it.colourways.map { c -> c.colourway } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogState())

    fun logCompletion(
        context: Context,
        pattern: Pattern,
        colourway: Colourway,
        date: LocalDate,
        photoUri: Uri?,
        notes: String?,
        energyTag: String?,
        onCelebrate: (CelebrationData) -> Unit,
    ) {
        viewModelScope.launch {
            val storedPath = photoUri?.let { ImageStorage.persist(context, it) }
            val result = repo.addCompletionAndEarnStickers(
                Completion(
                    colourwayId = colourway.id,
                    completedAtEpochDay = date.toEpochDay(),
                    photoUri = storedPath,
                    notes = notes?.takeIf { it.isNotBlank() },
                    energyTag = energyTag,
                ),
            )
            onCelebrate(
                CelebrationData(
                    completionId = result.completion.id,
                    patternName = pattern.name,
                    colourwayName = colourway.name,
                    photoPath = storedPath,
                    stickers = result.earned,
                ),
            )
        }
    }
}
