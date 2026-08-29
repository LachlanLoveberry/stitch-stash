package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lachlan.stitchstash.data.db.entities.MarketTodo
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketTodoDao {
    @Query("SELECT * FROM market_todos WHERE marketId = :marketId ORDER BY isDone ASC, createdAt ASC")
    fun observeForMarket(marketId: Long): Flow<List<MarketTodo>>

    @Insert
    suspend fun insert(todo: MarketTodo): Long

    @Update
    suspend fun update(todo: MarketTodo)

    @Query("DELETE FROM market_todos WHERE id = :id")
    suspend fun delete(id: Long)
}
