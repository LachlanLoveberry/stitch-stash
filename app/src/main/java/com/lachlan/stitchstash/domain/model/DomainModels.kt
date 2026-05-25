package com.lachlan.stitchstash.domain.model

import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Pattern

/** A pattern bundled with its colourways and the count of completions per colourway. */
data class PatternWithProgress(
    val pattern: Pattern,
    val colourways: List<ColourwayProgress>,
) {
    val totalTarget: Int get() = colourways.sumOf { it.colourway.targetCount }
    val totalCompleted: Int get() = colourways.sumOf { it.completedCount.coerceAtMost(it.colourway.targetCount) }
    val isFullyComplete: Boolean get() = colourways.all { it.completedCount >= it.colourway.targetCount }
}

data class ColourwayProgress(
    val colourway: Colourway,
    val completedCount: Int,
) {
    val remaining: Int get() = (colourway.targetCount - completedCount).coerceAtLeast(0)
}

data class CompletionWithContext(
    val completion: Completion,
    val pattern: Pattern,
    val colourway: Colourway,
)
