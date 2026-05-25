package com.lachlan.stitchstash.domain.forecast

import com.lachlan.stitchstash.domain.model.PatternWithProgress
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pick-two-of-three solver. Given any two of {marketDate, targetPieces, weeklyHours},
 * compute the third given the current stash + estimate fallbacks.
 */
object ScenarioSolver {

    enum class LockedKey(val storage: String) {
        DATE_PIECES("DATE_PIECES"),
        HOURS_DATE("HOURS_DATE"),
        HOURS_PIECES("HOURS_PIECES"),
        ;

        companion object {
            fun from(s: String) = values().firstOrNull { it.storage == s } ?: HOURS_DATE
        }
    }

    sealed interface SolveResult {
        data class HoursPerWeek(val value: Float) : SolveResult
        data class AchievablePieces(val value: Int) : SolveResult
        data class FinishDate(val value: LocalDate) : SolveResult
        data object NotEnoughInput : SolveResult
    }

    data class Inputs(
        val marketDate: LocalDate?,
        val targetPieces: Int?,
        val weeklyHours: Float?,
        val locked: LockedKey,
    )

    fun solve(
        inputs: Inputs,
        patterns: List<PatternWithProgress>,
        observedAvgHoursPerPiece: Float?,
        globalSeed: Float,
        today: LocalDate = LocalDate.now(),
    ): SolveResult {
        val piecesAlreadyDone = ForecastEngine.piecesCompleted(patterns)
        val piecesPlanned = ForecastEngine.piecesPlanned(patterns)

        return when (inputs.locked) {
            LockedKey.DATE_PIECES -> {
                val date = inputs.marketDate ?: return SolveResult.NotEnoughInput
                val target = inputs.targetPieces ?: return SolveResult.NotEnoughInput
                val toFinish = max(0, target - piecesAlreadyDone)
                val weeks = weeksBetween(today, date)
                if (weeks <= 0f || toFinish == 0) return SolveResult.HoursPerWeek(0f)
                val hoursNeeded = totalHoursForNextN(patterns, toFinish, observedAvgHoursPerPiece, globalSeed)
                SolveResult.HoursPerWeek(hoursNeeded / weeks)
            }
            LockedKey.HOURS_DATE -> {
                val date = inputs.marketDate ?: return SolveResult.NotEnoughInput
                val hours = inputs.weeklyHours ?: return SolveResult.NotEnoughInput
                val weeks = weeksBetween(today, date)
                val available = max(0f, hours * weeks)
                val achievable = piecesCountForBudget(patterns, available, observedAvgHoursPerPiece, globalSeed)
                SolveResult.AchievablePieces(piecesAlreadyDone + achievable)
            }
            LockedKey.HOURS_PIECES -> {
                val target = inputs.targetPieces ?: return SolveResult.NotEnoughInput
                val hours = inputs.weeklyHours ?: return SolveResult.NotEnoughInput
                if (hours <= 0f) return SolveResult.NotEnoughInput
                val toFinish = max(0, target - piecesAlreadyDone)
                if (toFinish == 0) return SolveResult.FinishDate(today)
                val hoursNeeded = totalHoursForNextN(patterns, toFinish, observedAvgHoursPerPiece, globalSeed)
                val weeksNeeded = hoursNeeded / hours
                val days = (weeksNeeded * 7).roundToInt().toLong()
                SolveResult.FinishDate(today.plusDays(days))
            }
        }
    }

    private fun weeksBetween(a: LocalDate, b: LocalDate): Float =
        java.time.temporal.ChronoUnit.DAYS.between(a, b).coerceAtLeast(0).toFloat() / 7f

    /** Walk remaining stash pieces in order, summing the next N piece estimates. */
    private fun totalHoursForNextN(
        patterns: List<PatternWithProgress>,
        n: Int,
        observedAvg: Float?,
        globalSeed: Float,
    ): Float {
        var remaining = n
        var hours = 0f
        for (p in patterns) {
            val perPiece = ForecastEngine.estimateHoursPerPiece(p.pattern, observedAvg, globalSeed)
            for (cw in p.colourways) {
                val take = minOf(cw.remaining, remaining)
                hours += take * perPiece
                remaining -= take
                if (remaining == 0) return hours
            }
        }
        // Ran out of stash; remainder uses fallback estimate
        if (remaining > 0) {
            hours += remaining * (observedAvg ?: globalSeed)
        }
        return hours
    }

    private fun piecesCountForBudget(
        patterns: List<PatternWithProgress>,
        budgetHours: Float,
        observedAvg: Float?,
        globalSeed: Float,
    ): Int {
        var left = budgetHours
        var count = 0
        for (p in patterns) {
            val perPiece = ForecastEngine.estimateHoursPerPiece(p.pattern, observedAvg, globalSeed)
            for (cw in p.colourways) {
                repeat(cw.remaining) {
                    if (left >= perPiece) {
                        left -= perPiece
                        count += 1
                    } else return count
                }
            }
        }
        return count
    }
}
