package com.lachlan.stitchstash.data.backup

import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION = 2

@Serializable
data class BackupFile(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val patterns: List<PatternDto>,
    val colourways: List<ColourwayDto>,
    val completions: List<CompletionDto>,
    val markets: List<MarketDto>,
    val marketTodos: List<MarketTodoDto> = emptyList(),
    val stickers: List<StickerDto>,
    val scenarios: List<ScenarioDto>,
    val finishCards: List<FinishCardDto>,
    val settings: SettingsDto,
)

@Serializable
data class PatternDto(
    val id: Long,
    val name: String,
    val coverImageUri: String?,
    val ribblrUrl: String?,
    val designer: String?,
    val estimateHours: Float?,
    val estimateBucket: String?,
    val similarToPatternId: Long?,
    val createdAt: Long,
)

@Serializable
data class ColourwayDto(
    val id: Long,
    val patternId: Long,
    val name: String,
    val swatchHex: String?,
    val targetCount: Int,
    val createdAt: Long,
)

@Serializable
data class CompletionDto(
    val id: Long,
    val colourwayId: Long,
    val completedAtEpochDay: Long,
    val photoUri: String?,
    val notes: String?,
    val energyTag: String?,
    val createdAt: Long,
)

@Serializable
data class MarketDto(
    val id: Long,
    val name: String?,
    val dateEpochDay: Long,
    val isSkipped: Boolean,
    val createdAt: Long,
    val reflectionPrompted: Boolean = false,
    val attended: Boolean? = null,
    val howItWent: String? = null,
    val howItFelt: String? = null,
    val whatLearned: String? = null,
)

@Serializable
data class MarketTodoDto(
    val id: Long,
    val marketId: Long,
    val text: String,
    val isDone: Boolean,
    val createdAt: Long,
)

@Serializable
data class StickerDto(
    val id: Long,
    val type: String,
    val earnedAt: Long,
    val relatedCompletionId: Long?,
    val relatedPatternId: Long?,
)

@Serializable
data class ScenarioDto(
    val id: Long,
    val name: String,
    val marketId: Long?,
    val customDateEpochDay: Long?,
    val targetPieces: Int?,
    val weeklyHours: Float?,
    val lockedKey: String,
    val isActive: Boolean,
    val createdAt: Long,
)

@Serializable
data class FinishCardDto(
    val id: Long,
    val completionId: Long,
    val imagePath: String,
    val borderStyle: String,
    val createdAt: Long,
)

@Serializable
data class SettingsDto(
    val weeklyHours: Float,
    val targetPieces: Int,
    val forecastVisible: Boolean,
    val weeklyRecapEnabled: Boolean,
    val stickerBookEnabled: Boolean,
    val finishCardBorderStyle: String,
    val avgHoursPerPieceSeed: Float,
    val driveBackupEnabled: Boolean,
    val driveFolderId: String?,
    val lastBackupAt: Long?,
    val onboardingComplete: Boolean,
)
