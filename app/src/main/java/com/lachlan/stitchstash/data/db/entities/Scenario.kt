package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scenarios")
data class Scenario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val marketId: Long? = null,
    val customDateEpochDay: Long? = null,
    val targetPieces: Int? = null,
    val weeklyHours: Float? = null,
    /** Which two fields are user-locked. The third is solved. */
    val lockedKey: String, // "DATE_PIECES" | "HOURS_DATE" | "HOURS_PIECES"
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
