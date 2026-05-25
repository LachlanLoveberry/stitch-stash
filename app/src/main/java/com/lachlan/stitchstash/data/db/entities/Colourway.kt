package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "colourways",
    foreignKeys = [
        ForeignKey(
            entity = Pattern::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("patternId")],
)
data class Colourway(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patternId: Long,
    val name: String,
    val swatchHex: String? = null,
    val targetCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
)
