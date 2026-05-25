package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stickers")
data class Sticker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,        // matches a key in StickerCatalog
    val earnedAt: Long = System.currentTimeMillis(),
    val relatedCompletionId: Long? = null,
    val relatedPatternId: Long? = null,
)
