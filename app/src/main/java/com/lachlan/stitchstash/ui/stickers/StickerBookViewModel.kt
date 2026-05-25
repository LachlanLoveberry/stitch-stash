package com.lachlan.stitchstash.ui.stickers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.Sticker
import com.lachlan.stitchstash.data.repository.StitchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StickerBookViewModel(repo: StitchRepository) : ViewModel() {
    val stickers: StateFlow<List<Sticker>> =
        repo.observeStickers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
