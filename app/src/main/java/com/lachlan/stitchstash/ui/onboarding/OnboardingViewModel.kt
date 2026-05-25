package com.lachlan.stitchstash.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.repository.StitchRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

class OnboardingViewModel(private val repo: StitchRepository) : ViewModel() {

    fun completeOnboarding(
        marketName: String?,
        marketDate: LocalDate?,
        weeklyHours: Float?,
        targetPieces: Int?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            repo.ensureSettingsRow()
            if (marketDate != null) {
                val name = marketName?.takeIf { it.isNotBlank() } ?: "Upcoming market"
                repo.addMarket(name, marketDate)
            }
            val current = AppSettings()
            repo.updateSettings(
                current.copy(
                    weeklyHours = weeklyHours ?: current.weeklyHours,
                    targetPieces = targetPieces ?: current.targetPieces,
                    onboardingComplete = true,
                ),
            )
            onDone()
        }
    }

    fun skipOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.ensureSettingsRow()
            repo.updateSettings(AppSettings(onboardingComplete = true))
            onDone()
        }
    }
}
