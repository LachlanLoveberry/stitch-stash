package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lachlan.stitchstash.data.db.entities.Completion
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions ORDER BY completedAtEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<Completion>>

    @Query("SELECT * FROM completions ORDER BY completedAtEpochDay DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE colourwayId = :colourwayId ORDER BY completedAtEpochDay DESC")
    fun observeForColourway(colourwayId: Long): Flow<List<Completion>>

    @Query("SELECT COUNT(*) FROM completions")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM completions")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM completions WHERE colourwayId IN (SELECT id FROM colourways WHERE patternId = :patternId)")
    suspend fun getCountForPattern(patternId: Long): Int

    @Query("SELECT COUNT(*) FROM completions WHERE colourwayId = :colourwayId")
    suspend fun getCountForColourway(colourwayId: Long): Int

    @Query("SELECT MAX(completedAtEpochDay) FROM completions WHERE id != :excludeId")
    suspend fun getMostRecentBefore(excludeId: Long): Long?

    @Insert
    suspend fun insert(completion: Completion): Long

    @Update
    suspend fun update(completion: Completion)

    @Query("DELETE FROM completions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM completions WHERE id = :id")
    suspend fun getById(id: Long): Completion?
}
