package com.lachlan.stitchstash.data.backup

import com.lachlan.stitchstash.BuildConfig
import com.lachlan.stitchstash.data.db.StitchStashDatabase
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.FinishCard
import com.lachlan.stitchstash.data.db.entities.Market
import com.lachlan.stitchstash.data.db.entities.MarketTodo
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.db.entities.Scenario
import com.lachlan.stitchstash.data.db.entities.Sticker
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

object BackupSerializer {

    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun export(db: StitchStashDatabase): String {
        val patterns = db.patternDao().observeAll().first().map { it.toDto() }
        val colourways = db.colourwayDao().observeAll().first().map { it.toDto() }
        val completions = db.completionDao().observeAll().first().map { it.toDto() }
        val markets = db.marketDao().observeAll().first().map { it.toDto() }
        val marketTodos = db.marketTodoDao().getAll().map { it.toDto() }
        val stickers = db.stickerDao().observeAll().first().map { it.toDto() }
        val scenarios = db.scenarioDao().observeAll().first().map { it.toDto() }
        val finishCards = db.finishCardDao().observeAll().first().map { it.toDto() }
        val settings = (db.appSettingsDao().get() ?: AppSettings()).toDto()

        val backup = BackupFile(
            exportedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            patterns = patterns,
            colourways = colourways,
            completions = completions,
            markets = markets,
            marketTodos = marketTodos,
            stickers = stickers,
            scenarios = scenarios,
            finishCards = finishCards,
            settings = settings,
        )
        return json.encodeToString(BackupFile.serializer(), backup)
    }

    suspend fun import(db: StitchStashDatabase, rawJson: String): RestoreSummary {
        val backup = json.decodeFromString(BackupFile.serializer(), rawJson)
        // Naive replace strategy: assumes a fresh DB. For restore over existing data,
        // wipe local first (a Phase 6+ TODO).
        // Insert in dependency order; primary-key collisions use REPLACE via raw insert.
        backup.patterns.forEach { db.patternDao().insert(it.toEntity()) }
        backup.colourways.forEach { db.colourwayDao().insert(it.toEntity()) }
        backup.completions.forEach { db.completionDao().insert(it.toEntity()) }
        backup.markets.forEach { db.marketDao().insert(it.toEntity()) }
        backup.marketTodos.forEach { db.marketTodoDao().insert(it.toEntity()) }
        backup.stickers.forEach { db.stickerDao().insert(it.toEntity()) }
        backup.scenarios.forEach { db.scenarioDao().upsert(it.toEntity()) }
        backup.finishCards.forEach { db.finishCardDao().insert(it.toEntity()) }
        db.appSettingsDao().upsert(backup.settings.toEntity())

        return RestoreSummary(
            patterns = backup.patterns.size,
            colourways = backup.colourways.size,
            completions = backup.completions.size,
            stickers = backup.stickers.size,
            scenarios = backup.scenarios.size,
            finishCards = backup.finishCards.size,
            markets = backup.markets.size,
            marketTodos = backup.marketTodos.size,
        )
    }
}

data class RestoreSummary(
    val patterns: Int,
    val colourways: Int,
    val completions: Int,
    val stickers: Int,
    val scenarios: Int,
    val finishCards: Int,
    val markets: Int,
    val marketTodos: Int,
)

// ---- Mappers ---------------------------------------------------------------

private fun Pattern.toDto() = PatternDto(id, name, coverImageUri, ribblrUrl, designer, estimateHours, estimateBucket, similarToPatternId, createdAt)
private fun Colourway.toDto() = ColourwayDto(id, patternId, name, swatchHex, targetCount, createdAt)
private fun Completion.toDto() = CompletionDto(id, colourwayId, completedAtEpochDay, photoUri, notes, energyTag, createdAt)
private fun Market.toDto() = MarketDto(id, name, dateEpochDay, isSkipped, createdAt, reflectionPrompted, attended, howItWent, howItFelt, whatLearned)
private fun MarketTodo.toDto() = MarketTodoDto(id, marketId, text, isDone, createdAt)
private fun Sticker.toDto() = StickerDto(id, type, earnedAt, relatedCompletionId, relatedPatternId)
private fun Scenario.toDto() = ScenarioDto(id, name, marketId, customDateEpochDay, targetPieces, weeklyHours, lockedKey, isActive, createdAt)
private fun FinishCard.toDto() = FinishCardDto(id, completionId, imagePath, borderStyle, createdAt)
private fun AppSettings.toDto() = SettingsDto(weeklyHours, targetPieces, forecastVisible, weeklyRecapEnabled, stickerBookEnabled, finishCardBorderStyle, avgHoursPerPieceSeed, driveBackupEnabled, driveFolderId, lastBackupAt, onboardingComplete)

private fun PatternDto.toEntity() = Pattern(id, name, coverImageUri, ribblrUrl, designer, estimateHours, estimateBucket, similarToPatternId, createdAt)
private fun ColourwayDto.toEntity() = Colourway(id, patternId, name, swatchHex, targetCount, createdAt)
private fun CompletionDto.toEntity() = Completion(id, colourwayId, completedAtEpochDay, photoUri, notes, energyTag, createdAt)
private fun MarketDto.toEntity() = Market(id, name, dateEpochDay, isSkipped, createdAt, reflectionPrompted, attended, howItWent, howItFelt, whatLearned)
private fun MarketTodoDto.toEntity() = MarketTodo(id, marketId, text, isDone, createdAt)
private fun StickerDto.toEntity() = Sticker(id, type, earnedAt, relatedCompletionId, relatedPatternId)
private fun ScenarioDto.toEntity() = Scenario(id, name, marketId, customDateEpochDay, targetPieces, weeklyHours, lockedKey, isActive, createdAt)
private fun FinishCardDto.toEntity() = FinishCard(id, completionId, imagePath, borderStyle, createdAt)
private fun SettingsDto.toEntity() = AppSettings(
    weeklyHours = weeklyHours,
    targetPieces = targetPieces,
    forecastVisible = forecastVisible,
    weeklyRecapEnabled = weeklyRecapEnabled,
    stickerBookEnabled = stickerBookEnabled,
    finishCardBorderStyle = finishCardBorderStyle,
    avgHoursPerPieceSeed = avgHoursPerPieceSeed,
    driveBackupEnabled = driveBackupEnabled,
    driveFolderId = driveFolderId,
    lastBackupAt = lastBackupAt,
    onboardingComplete = onboardingComplete,
)
