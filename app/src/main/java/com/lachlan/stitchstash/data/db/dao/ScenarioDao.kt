package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lachlan.stitchstash.data.db.entities.Scenario
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios ORDER BY isActive DESC, createdAt DESC")
    fun observeAll(): Flow<List<Scenario>>

    @Query("SELECT * FROM scenarios WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<Scenario?>

    @Query("SELECT * FROM scenarios WHERE id = :id")
    suspend fun getById(id: Long): Scenario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scenario: Scenario): Long

    @Update
    suspend fun update(scenario: Scenario)

    @Query("UPDATE scenarios SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE scenarios SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("DELETE FROM scenarios WHERE id = :id")
    suspend fun delete(id: Long)
}
