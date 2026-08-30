package com.lachlan.stitchstash.domain.forecast

import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure forecast math. No Android, no DB — easy to test in isolation.
 *
 * Phase 1 surfaces a single calm projection: "at your pace, how many pieces by market day?"
 * The full three-row lock-and-solve playground arrives in Phase 4.
 */
object ForecastEngine {

    /** Pieces remaining = sum of (targetCount - completedCount) across colourways. */
    fun piecesRemaining(patterns: List<PatternWithProgress>): Int =
        patterns.sumOf { p -> p.colourways.sumOf { it.remaining } }

    /** Total pieces in the stash plan = sum of targetCounts. */
    fun piecesPlanned(patterns: List<PatternWithProgress>): Int =
        patterns.sumOf { p -> p.colourways.sumOf { it.colourway.targetCount } }

    /** Pieces already completed (capped at target so over-completes don't inflate). */
    fun piecesCompleted(patterns: List<PatternWithProgress>): Int =
        patterns.sumOf { it.totalCompleted }

    /** Percentage of planned stash done, 0..1. Returns 0 if no plan yet. */
    fun fractionComplete(patterns: List<PatternWithProgress>): Float {
        val planned = piecesPlanned(patterns)
        if (planned == 0) return 0f
        return piecesCompleted(patterns).toFloat() / planned.toFloat()
    }

    /** Estimated hours for a single piece — fallback chain. */
    fun estimateHoursPerPiece(
        pattern: Pattern,
        observedAvg: Float?,
        globalSeed: Float,
    ): Float {
        pattern.estimateHours?.let { return it }
        pattern.estimateBucket?.let { return bucketMidpoint(it) }
        observedAvg?.let { return it }
        return globalSeed
    }

    fun bucketMidpoint(bucket: String): Float = when (bucket) {
        "quick" -> 1.5f
        "evening" -> 4.5f
        "project" -> 9f
        "big" -> 16f
        else -> 4f
    }

    /**
     * Projection: given a market date, weekly hours, and the remaining stash, how many pieces
     * can realistically be finished before the market?
     */
    fun projectPiecesByDate(
        patterns: List<PatternWithProgress>,
        marketDate: LocalDate,
        weeklyHours: Float,
        observedAvgHoursPerPiece: Float?,
        globalSeed: Float,
        today: LocalDate = LocalDate.now(),
    ): Projection {
        val daysUntil = max(0, ChronoUnit.DAYS.between(today, marketDate).toInt())
        val weeksUntil = daysUntil / 7f
        val availableHours = weeklyHours * weeksUntil

        // Walk remaining pieces in pattern order, greedily consuming hours
        var hoursLeft = availableHours
        var piecesAchievable = 0
        for (p in patterns) {
            val hoursPerPiece = estimateHoursPerPiece(p.pattern, observedAvgHoursPerPiece, globalSeed)
            for (cw in p.colourways) {
                repeat(cw.remaining.coerceAtLeast(0)) {
                    if (hoursLeft >= hoursPerPiece) {
                        hoursLeft -= hoursPerPiece
                        piecesAchievable += 1
                    }
                }
            }
        }

        val totalPlanned = piecesPlanned(patterns)
        val alreadyDone = piecesCompleted(patterns)
        val projectedFinishCount = alreadyDone + piecesAchievable

        return Projection(
            daysUntilMarket = daysUntil,
            availableHours = availableHours,
            additionalPiecesAchievable = piecesAchievable,
            projectedFinishCount = projectedFinishCount,
            totalPlanned = totalPlanned,
        )
    }

    /** A calm, never-red status sentence. */
    fun trackingMessage(projection: Projection, target: Int): String {
        if (target <= 0) {
            return "Add some patterns and start logging — the picture builds itself."
        }
        val proj = projection.projectedFinishCount
        return when {
            proj >= target -> "On pace to bring all ${target} — cruising. "
            proj >= (target * 0.85f).roundToInt() -> "Right in the pocket — about $proj by then."
            proj >= (target * 0.6f).roundToInt() -> "Comfortably tracking ~$proj. Plenty for a lovely stall."
            else -> "Looking like ~$proj by then. The next market would give you more room."
        }
    }

    data class Projection(
        val daysUntilMarket: Int,
        val availableHours: Float,
        val additionalPiecesAchievable: Int,
        val projectedFinishCount: Int,
        val totalPlanned: Int,
    )
}
