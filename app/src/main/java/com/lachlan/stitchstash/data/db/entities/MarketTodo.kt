package com.lachlan.stitchstash.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "market_todos", indices = [Index("marketId")])
data class MarketTodo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marketId: Long,
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
