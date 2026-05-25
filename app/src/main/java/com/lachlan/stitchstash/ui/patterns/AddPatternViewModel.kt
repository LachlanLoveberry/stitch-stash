package com.lachlan.stitchstash.ui.patterns

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.data.ribblr.RibblrScraper
import com.lachlan.stitchstash.data.storage.ImageStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddPatternViewModel(private val repo: StitchRepository) : ViewModel() {

    private val _importState = MutableStateFlow<RibblrImportState>(RibblrImportState.Idle)
    val importState: StateFlow<RibblrImportState> = _importState.asStateFlow()

    fun importFromRibblr(context: Context, url: String, onResult: (RibblrImportResult) -> Unit) {
        if (url.isBlank()) return
        _importState.value = RibblrImportState.Loading
        viewModelScope.launch {
            val result = RibblrScraper.import(context, url.trim())
            result.fold(
                onSuccess = { data ->
                    _importState.value = RibblrImportState.Idle
                    onResult(
                        RibblrImportResult(
                            name = data.name ?: "",
                            designer = data.designer,
                            localCoverPath = data.localCoverPath,
                            sourceUrl = data.sourceUrl,
                        ),
                    )
                },
                onFailure = {
                    _importState.value = RibblrImportState.Error(
                        it.message ?: "Couldn't fetch that page",
                    )
                },
            )
        }
    }

    fun dismissError() {
        _importState.value = RibblrImportState.Idle
    }

    fun save(
        context: Context,
        name: String,
        coverUri: Uri?,
        preloadedCoverPath: String?,
        ribblrUrl: String?,
        designer: String?,
        colourwayInputs: List<ColourwayInput>,
        onDone: () -> Unit,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val resolvedCover = preloadedCoverPath
                ?: coverUri?.let { ImageStorage.persist(context, it) }
            val patternId = repo.addPattern(
                Pattern(
                    name = name.trim(),
                    coverImageUri = resolvedCover,
                    ribblrUrl = ribblrUrl?.takeIf { it.isNotBlank() },
                    designer = designer?.takeIf { !it.isNullOrBlank() },
                ),
            )
            colourwayInputs
                .filter { it.name.isNotBlank() }
                .forEach { input ->
                    repo.addColourway(
                        Colourway(
                            patternId = patternId,
                            name = input.name.trim(),
                            swatchHex = input.swatchHex,
                            targetCount = input.targetCount.coerceAtLeast(1),
                        ),
                    )
                }
            onDone()
        }
    }
}

data class ColourwayInput(
    val name: String = "",
    val swatchHex: String? = null,
    val targetCount: Int = 1,
)

data class RibblrImportResult(
    val name: String,
    val designer: String?,
    val localCoverPath: String?,
    val sourceUrl: String,
)

sealed interface RibblrImportState {
    data object Idle : RibblrImportState
    data object Loading : RibblrImportState
    data class Error(val message: String) : RibblrImportState
}
