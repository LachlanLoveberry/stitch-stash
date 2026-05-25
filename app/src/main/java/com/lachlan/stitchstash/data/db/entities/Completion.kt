package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = Colourway::class,
            parentColumns = ["id"],
            childColumns = ["colourwayId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("colourwayId"), Index("completedAtEpochDay")],
)
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val colourwayId: Long,
    val completedAtEpochDay: Long, // LocalDate.toEpochDay()
    val photoUri: String? = null,
    val notes: String? = null,
    val energyTag: String? = null, // "good" | "ok" | "low"
    val createdAt: Long = System.currentTimeMillis(),
)
