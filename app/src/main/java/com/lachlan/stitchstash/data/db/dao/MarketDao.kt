package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lachlan.stitchstash.data.db.entities.Market
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {
    @Query("SELECT * FROM markets WHERE isSkipped = 0 ORDER BY dateEpochDay ASC")
    fun observeUpcoming(): Flow<List<Market>>

    @Query("SELECT * FROM markets ORDER BY dateEpochDay ASC")
    fun observeAll(): Flow<List<Market>>

    @Query("SELECT * FROM markets WHERE isSkipped = 0 AND dateEpochDay >= :todayEpochDay ORDER BY dateEpochDay ASC LIMIT 1")
    fun observeNextUpcoming(todayEpochDay: Long): Flow<Market?>

    @Insert
    suspend fun insert(market: Market): Long

    @Update
    suspend fun update(market: Market)

    @Query("DELETE FROM markets WHERE id = :id")
    suspend fun delete(id: Long)
}
