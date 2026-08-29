package com.lachlan.stitchstash.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.ui.finishcard.FinishCardViewModel
import com.lachlan.stitchstash.ui.home.HomeViewModel
import com.lachlan.stitchstash.ui.log.LogCompletionViewModel
import com.lachlan.stitchstash.ui.marketprep.MarketPrepViewModel
import com.lachlan.stitchstash.ui.markets.MarketsViewModel
import com.lachlan.stitchstash.ui.onboarding.OnboardingViewModel
import com.lachlan.stitchstash.ui.patterns.AddPatternViewModel
import com.lachlan.stitchstash.ui.patterns.EstimatePatternViewModel
import com.lachlan.stitchstash.ui.patterns.PatternListViewModel
import com.lachlan.stitchstash.ui.plan.PlanViewModel
import com.lachlan.stitchstash.ui.settings.SettingsViewModel
import com.lachlan.stitchstash.ui.stickers.StickerBookViewModel

private fun CreationExtras.repository(): StitchRepository =
    (this[APPLICATION_KEY] as StitchStashApp).repository

val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { OnboardingViewModel(repository()) }
    initializer { HomeViewModel(repository()) }
    initializer { PatternListViewModel(repository()) }
    initializer { AddPatternViewModel(repository()) }
    initializer { EstimatePatternViewModel(repository()) }
    initializer { LogCompletionViewModel(repository()) }
    initializer { StickerBookViewModel(repository()) }
    initializer { FinishCardViewModel(repository()) }
    initializer { PlanViewModel(repository()) }
    initializer { MarketsViewModel(repository()) }
    initializer { MarketPrepViewModel(repository()) }
    initializer { SettingsViewModel(repository()) }
}
