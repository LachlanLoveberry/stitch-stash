package com.lachlan.stitchstash.data.repository

import com.lachlan.stitchstash.data.db.StitchStashDatabase
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.FinishCard
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.db.entities.Scenario
import com.lachlan.stitchstash.data.db.entities.Sticker
import com.lachlan.stitchstash.domain.model.ColourwayProgress
import com.lachlan.stitchstash.domain.model.CompletionWithContext
import com.lachlan.stitchstash.domain.model.PatternWithProgress
import com.lachlan.stitchstash.domain.stickers.StickerCatalog
import com.lachlan.stitchstash.domain.stickers.StickerContext
import com.lachlan.stitchstash.domain.stickers.StickerEarner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class StitchRepository(private val db: StitchStashDatabase) {

    // ---- Settings ----------------------------------------------------------

    fun observeSettings(): Flow<AppSettings> =
        db.appSettingsDao().observe().map { it ?: AppSettings() }

    suspend fun ensureSettingsRow() {
        if (db.appSettingsDao().get() == null) {
            db.appSettingsDao().upsert(AppSettings())
        }
    }

    suspend fun updateSettings(settings: AppSettings) = db.appSettingsDao().upsert(settings)

    // ---- Markets -----------------------------------------------------------

    fun observeNextMarket(today: LocalDate = LocalDate.now()): Flow<Market?> =
        db.marketDao().observeNextUpcoming(today.toEpochDay())

    fun observeAllMarkets(): Flow<List<Market>> = db.marketDao().observeAll()

    suspend fun addMarket(name: String, date: LocalDate): Long =
        db.marketDao().insert(Market(name = name, dateEpochDay = date.toEpochDay()))

    suspend fun updateMarket(market: Market) = db.marketDao().update(market)

    // ---- Patterns ----------------------------------------------------------

    fun observePatterns(): Flow<List<Pattern>> = db.patternDao().observeAll()

    suspend fun addPattern(pattern: Pattern): Long = db.patternDao().insert(pattern)

    suspend fun updatePattern(pattern: Pattern) = db.patternDao().update(pattern)

    suspend fun getPattern(id: Long): Pattern? = db.patternDao().getById(id)

    suspend fun deletePattern(id: Long) = db.patternDao().delete(id)

    // ---- Colourways --------------------------------------------------------

    fun observeColourwaysForPattern(patternId: Long): Flow<List<Colourway>> =
        db.colourwayDao().observeForPattern(patternId)

    suspend fun addColourway(colourway: Colourway): Long = db.colourwayDao().insert(colourway)

    suspend fun updateColourway(colourway: Colourway) = db.colourwayDao().update(colourway)

    // ---- Completions -------------------------------------------------------

    fun observeAllCompletions(): Flow<List<Completion>> = db.completionDao().observeAll()

    fun observeRecentCompletions(limit: Int = 5): Flow<List<Completion>> =
        db.completionDao().observeRecent(limit)

    fun observeTotalCompletionCount(): Flow<Int> = db.completionDao().observeTotalCount()

    suspend fun addCompletion(completion: Completion): Long = db.completionDao().insert(completion)

    suspend fun deleteCompletion(id: Long) = db.completionDao().delete(id)

    /**
     * Inserts a completion and evaluates which stickers it earns. Returns the new
     * completion id together with any earned stickers (already persisted).
     */
    suspend fun addCompletionAndEarnStickers(completion: Completion): CompletionWithEarnings {
        val previousTotal = db.completionDao().getTotalCount()
        val priorMostRecentDay: Long? = if (previousTotal > 0) {
            // Most recent before this insert
            db.completionDao().getMostRecentBefore(-1L)
        } else null

        val id = db.completionDao().insert(completion)
        val inserted = completion.copy(id = id)

        val totalAfter = previousTotal + 1
        val colourway = db.colourwayDao().getById(completion.colourwayId)
        val patternId = colourway?.patternId ?: return CompletionWithEarnings(inserted, emptyList())

        val patternColourways = db.colourwayDao().getForPattern(patternId)
        val countForPattern = db.completionDao().getCountForPattern(patternId)
        val isFirstOfThisPattern = countForPattern == 1
        val pattern = db.patternDao().getById(patternId)
        val cameFromRibblr = !pattern?.ribblrUrl.isNullOrBlank()

        // Pattern fully complete? Sum of completions per colourway ≥ targetCount for all.
        val patternFullyCompletedNow = run {
            val completionCountsPerColourway = patternColourways.associate { cw ->
                cw.id to db.completionDao().getCountForColourway(cw.id)
            }
            patternColourways.all { (completionCountsPerColourway[it.id] ?: 0) >= it.targetCount }
        }

        val firstRibblrAlreadyEarned =
            db.stickerDao().firstOfType(StickerCatalog.FIRST_RIBBLR) != null

        val gap = priorMostRecentDay?.let { LocalDate.ofEpochDay(it) }?.let {
            StickerEarner.daysBetween(it, LocalDate.ofEpochDay(completion.completedAtEpochDay))
        }

        val context = StickerContext(
            newCompletion = inserted,
            patternId = patternId,
            isFirstEver = previousTotal == 0,
            isFirstOfThisPattern = isFirstOfThisPattern,
            patternFullyCompletedNow = patternFullyCompletedNow,
            totalCount = totalAfter,
            previousTotalCount = previousTotal,
            daysSincePreviousCompletion = gap,
            cameFromRibblr = cameFromRibblr,
            firstRibblrAlreadyEarned = firstRibblrAlreadyEarned,
            welcomeBackAlreadyEarnedForGap = false,
        )

        val earned = StickerEarner.evaluate(context)
        earned.forEach { db.stickerDao().insert(it) }
        return CompletionWithEarnings(inserted, earned)
    }

    // ---- Stickers ----------------------------------------------------------

    fun observeStickers(): Flow<List<Sticker>> = db.stickerDao().observeAll()

    // ---- Scenarios ---------------------------------------------------------

    fun observeScenarios(): Flow<List<Scenario>> = db.scenarioDao().observeAll()
    fun observeActiveScenario(): Flow<Scenario?> = db.scenarioDao().observeActive()
    suspend fun upsertScenario(scenario: Scenario): Long = db.scenarioDao().upsert(scenario)
    suspend fun deleteScenario(id: Long) = db.scenarioDao().delete(id)
    suspend fun activateScenario(id: Long) {
        db.scenarioDao().clearActive()
        db.scenarioDao().setActive(id)
    }

    // ---- Finish cards ------------------------------------------------------

    fun observeFinishCards(): Flow<List<FinishCard>> = db.finishCardDao().observeAll()
    suspend fun addFinishCard(card: FinishCard): Long = db.finishCardDao().insert(card)
    suspend fun deleteFinishCard(id: Long) = db.finishCardDao().delete(id)

    data class CompletionWithEarnings(
        val completion: Completion,
        val earned: List<Sticker>,
    )

    // ---- Composite views ---------------------------------------------------

    /** All patterns with their colourways + completion counts. Live-updating. */
    fun observePatternsWithProgress(): Flow<List<PatternWithProgress>> =
        combine(
            db.patternDao().observeAll(),
            db.colourwayDao().observeAll(),
            db.completionDao().observeAll(),
        ) { patterns, colourways, completions ->
            val byColourway = completions.groupingBy { it.colourwayId }.eachCount()
            patterns.map { pattern ->
                val patternColourways = colourways.filter { it.patternId == pattern.id }
                PatternWithProgress(
                    pattern = pattern,
                    colourways = patternColourways.map { cw ->
                        ColourwayProgress(cw, byColourway[cw.id] ?: 0)
                    },
                )
            }
        }

    /** Recent completions hydrated with pattern + colourway context. */
    fun observeRecentCompletionsWithContext(limit: Int = 5): Flow<List<CompletionWithContext>> =
        combine(
            db.completionDao().observeRecent(limit),
            db.colourwayDao().observeAll(),
            db.patternDao().observeAll(),
        ) { completions, colourways, patterns ->
            val colourwayById = colourways.associateBy { it.id }
            val patternById = patterns.associateBy { it.id }
            completions.mapNotNull { completion ->
                val cw = colourwayById[completion.colourwayId] ?: return@mapNotNull null
                val pattern = patternById[cw.patternId] ?: return@mapNotNull null
                CompletionWithContext(completion, pattern, cw)
            }
        }
}
