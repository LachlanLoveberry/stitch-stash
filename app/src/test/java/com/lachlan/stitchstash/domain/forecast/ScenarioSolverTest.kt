package com.lachlan.stitchstash.domain.forecast

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.domain.model.ColourwayProgress
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import org.junit.Test
import java.time.LocalDate

class ScenarioSolverTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun stashOf(remaining: Int, hoursPerPiece: Float = 2f): List<PatternWithProgress> = listOf(
        PatternWithProgress(
            pattern = Pattern(id = 1L, name = "P", estimateHours = hoursPerPiece),
            colourways = listOf(
                ColourwayProgress(
                    Colourway(id = 1L, patternId = 1L, name = "C", targetCount = remaining),
                    completedCount = 0,
                ),
            ),
        ),
    )

    @Test
    fun `DATE_PIECES returns NotEnoughInput when date missing`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = null, targetPieces = 10, weeklyHours = null, locked = ScenarioSolver.LockedKey.DATE_PIECES),
            emptyList(), null, 4f, today,
        )
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.NotEnoughInput)
    }

    @Test
    fun `DATE_PIECES with zero weeks until market returns zero hours per week instead of dividing by zero`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = today, targetPieces = 10, weeklyHours = null, locked = ScenarioSolver.LockedKey.DATE_PIECES),
            stashOf(10), null, 4f, today,
        )
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.HoursPerWeek(0f))
    }

    @Test
    fun `DATE_PIECES with nothing left to finish returns zero hours per week`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = today.plusWeeks(4), targetPieces = 0, weeklyHours = null, locked = ScenarioSolver.LockedKey.DATE_PIECES),
            emptyList(), null, 4f, today,
        )
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.HoursPerWeek(0f))
    }

    @Test
    fun `DATE_PIECES computes hours per week using stash and fallback estimate`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = today.plusDays(14), targetPieces = 5, weeklyHours = null, locked = ScenarioSolver.LockedKey.DATE_PIECES),
            emptyList(), null, 4f, today,
        )
        // 5 pieces * 4h fallback = 20h over 2 weeks = 10h/week
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.HoursPerWeek(10f))
    }

    @Test
    fun `HOURS_DATE returns NotEnoughInput when hours missing`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = today, targetPieces = null, weeklyHours = null, locked = ScenarioSolver.LockedKey.HOURS_DATE),
            emptyList(), null, 4f, today,
        )
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.NotEnoughInput)
    }

    @Test
    fun `HOURS_PIECES guards non-positive hours`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = null, targetPieces = 5, weeklyHours = 0f, locked = ScenarioSolver.LockedKey.HOURS_PIECES),
            emptyList(), null, 4f, today,
        )
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.NotEnoughInput)
    }

    @Test
    fun `HOURS_PIECES returns today when nothing left to finish`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = null, targetPieces = 0, weeklyHours = 5f, locked = ScenarioSolver.LockedKey.HOURS_PIECES),
            emptyList(), null, 4f, today,
        )
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.FinishDate(today))
    }

    @Test
    fun `HOURS_PIECES computes a finish date from stash and weekly hours`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = null, targetPieces = 5, weeklyHours = 10f, locked = ScenarioSolver.LockedKey.HOURS_PIECES),
            emptyList(), null, 4f, today,
        )
        // 5 pieces * 4h = 20h / 10h per week = 2 weeks = 14 days
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.FinishDate(today.plusDays(14)))
    }

    @Test
    fun `HOURS_DATE with n less than or equal to zero market date still returns achievable pieces`() {
        val result = ScenarioSolver.solve(
            ScenarioSolver.Inputs(marketDate = today, targetPieces = null, weeklyHours = 10f, locked = ScenarioSolver.LockedKey.HOURS_DATE),
            stashOf(5), null, 4f, today,
        )
        assertThat(result).isEqualTo(ScenarioSolver.SolveResult.AchievablePieces(0))
    }

    @Test
    fun `LockedKey from unknown storage string falls back to HOURS_DATE`() {
        assertThat(ScenarioSolver.LockedKey.from("garbage")).isEqualTo(ScenarioSolver.LockedKey.HOURS_DATE)
    }
}
