package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "markets")
data class Market(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val dateEpochDay: Long,
    val isSkipped: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val reflectionPrompted: Boolean = false,
    val attended: Boolean? = null,
    val howItWent: String? = null,
    val howItFelt: String? = null,
    val whatLearned: String? = null,
)
