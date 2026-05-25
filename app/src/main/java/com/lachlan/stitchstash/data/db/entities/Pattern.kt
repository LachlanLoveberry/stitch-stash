package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patterns")
data class Pattern(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverImageUri: String? = null,
    val ribblrUrl: String? = null,
    val designer: String? = null,
    val estimateHours: Float? = null,
    val estimateBucket: String? = null, // "quick" | "evening" | "project" | "big"
    val similarToPatternId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
