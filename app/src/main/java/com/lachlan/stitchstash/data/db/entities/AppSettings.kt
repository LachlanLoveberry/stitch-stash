package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val weeklyHours: Float = 4f,
    val targetPieces: Int = 0,
    val forecastVisible: Boolean = true,
    val weeklyRecapEnabled: Boolean = false,
    val stickerBookEnabled: Boolean = true,
    val finishCardBorderStyle: String = "floral",
    val avgHoursPerPieceSeed: Float = 4f,
    val driveBackupEnabled: Boolean = false,
    val driveFolderId: String? = null,
    val lastBackupAt: Long? = null,
    val onboardingComplete: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
