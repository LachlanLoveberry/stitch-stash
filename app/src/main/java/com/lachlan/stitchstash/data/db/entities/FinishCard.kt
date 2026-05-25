package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved/shareable celebration card generated for a completion.
 * The PNG file lives on disk at imagePath.
 */
@Entity(tableName = "finish_cards")
data class FinishCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completionId: Long,
    val imagePath: String,
    val borderStyle: String,
    val createdAt: Long = System.currentTimeMillis(),
)
