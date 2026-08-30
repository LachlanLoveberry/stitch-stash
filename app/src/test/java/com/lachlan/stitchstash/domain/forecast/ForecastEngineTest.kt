package com.lachlan.stitchstash.domain.forecast

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.domain.model.ColourwayProgress
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import org.junit.Test
import java.time.LocalDate

class ForecastEngineTest {

    private fun pattern(id: Long = 1L, estimateHours: Float? = null, estimateBucket: String? = null) =
        Pattern(id = id, name = "P$id", estimateHours = estimateHours, estimateBucket = estimateBucket)

    private fun colourway(id: Long, patternId: Long, targetCount: Int) =
        Colourway(id = id, patternId = patternId, name = "C$id", targetCount = targetCount)

    @Test
    fun `fractionComplete returns 0 when nothing planned`() {
        val result = ForecastEngine.fractionComplete(emptyList())
        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `fractionComplete computes ratio of completed to planned`() {
        val p = PatternWithProgress(
            pattern = pattern(),
            colourways = listOf(ColourwayProgress(colourway(1, 1, targetCount = 10), completedCount = 4)),
        )
        assertThat(ForecastEngine.fractionComplete(listOf(p))).isEqualTo(0.4f)
    }

    @Test
    fun `estimateHoursPerPiece prefers explicit hours over bucket`() {
        val result = ForecastEngine.estimateHoursPerPiece(
            pattern(estimateHours = 3f, estimateBucket = "big"),
            observedAvg = 99f,
            globalSeed = 99f,
        )
        assertThat(result).isEqualTo(3f)
    }

    @Test
    fun `estimateHoursPerPiece falls back to bucket midpoint`() {
        val result = ForecastEngine.estimateHoursPerPiece(
            pattern(estimateBucket = "quick"),
            observedAvg = null,
            globalSeed = 99f,
        )
        assertThat(result).isEqualTo(1.5f)
    }

    @Test
    fun `estimateHoursPerPiece falls back to observed average`() {
        val result = ForecastEngine.estimateHoursPerPiece(pattern(), observedAvg = 6f, globalSeed = 99f)
        assertThat(result).isEqualTo(6f)
    }

    @Test
    fun `estimateHoursPerPiece falls back to global seed as last resort`() {
        val result = ForecastEngine.estimateHoursPerPiece(pattern(), observedAvg = null, globalSeed = 5f)
        assertThat(result).isEqualTo(5f)
    }

    @Test
    fun `projectPiecesByDate clamps days until market to zero when market date already passed`() {
        val projection = ForecastEngine.projectPiecesByDate(
            patterns = emptyList(),
            marketDate = LocalDate.of(2020, 1, 1),
            weeklyHours = 10f,
            observedAvgHoursPerPiece = null,
            globalSeed = 4f,
            today = LocalDate.of(2026, 1, 1),
        )
        assertThat(projection.daysUntilMarket).isEqualTo(0)
        assertThat(projection.additionalPiecesAchievable).isEqualTo(0)
    }

    @Test
    fun `projectPiecesByDate treats zero weekly hours as no additional pieces`() {
        val p = PatternWithProgress(
            pattern = pattern(estimateHours = 1f),
            colourways = listOf(ColourwayProgress(colourway(1, 1, targetCount = 10), completedCount = 0)),
        )
        val projection = ForecastEngine.projectPiecesByDate(
            patterns = listOf(p),
            marketDate = LocalDate.of(2026, 6, 1),
            weeklyHours = 0f,
            observedAvgHoursPerPiece = null,
            globalSeed = 4f,
            today = LocalDate.of(2026, 1, 1),
        )
        assertThat(projection.additionalPiecesAchievable).isEqualTo(0)
    }

    @Test
    fun `projectPiecesByDate does not crash on over-completed colourway with negative remaining`() {
        // remaining = targetCount - completedCount can go negative when a colourway is over-logged.
        val p = PatternWithProgress(
            pattern = pattern(estimateHours = 1f),
            colourways = listOf(ColourwayProgress(colourway(1, 1, targetCount = 2), completedCount = 5)),
        )
        val projection = ForecastEngine.projectPiecesByDate(
            patterns = listOf(p),
            marketDate = LocalDate.of(2026, 6, 1),
            weeklyHours = 10f,
            observedAvgHoursPerPiece = null,
            globalSeed = 4f,
            today = LocalDate.of(2026, 1, 1),
        )
        assertThat(projection.additionalPiecesAchievable).isEqualTo(0)
    }

    @Test
    fun `trackingMessage handles zero target gracefully`() {
        val projection = ForecastEngine.Projection(0, 0f, 0, 0, 0)
        val message = ForecastEngine.trackingMessage(projection, target = 0)
        assertThat(message).contains("Add some patterns")
    }
}
