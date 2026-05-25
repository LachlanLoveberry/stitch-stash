package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lachlan.stitchstash.data.db.entities.Colourway
import kotlinx.coroutines.flow.Flow

@Dao
interface ColourwayDao {
    @Query("SELECT * FROM colourways ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Colourway>>

    @Query("SELECT * FROM colourways WHERE patternId = :patternId ORDER BY createdAt ASC")
    fun observeForPattern(patternId: Long): Flow<List<Colourway>>

    @Query("SELECT * FROM colourways WHERE patternId = :patternId ORDER BY createdAt ASC")
    suspend fun getForPattern(patternId: Long): List<Colourway>

    @Query("SELECT * FROM colourways WHERE id = :id")
    suspend fun getById(id: Long): Colourway?

    @Insert
    suspend fun insert(colourway: Colourway): Long

    @Update
    suspend fun update(colourway: Colourway)

    @Query("DELETE FROM colourways WHERE id = :id")
    suspend fun delete(id: Long)
}
