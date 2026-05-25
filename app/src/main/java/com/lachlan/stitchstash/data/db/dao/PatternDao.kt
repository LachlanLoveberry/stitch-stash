package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lachlan.stitchstash.data.db.entities.Pattern
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    @Query("SELECT * FROM patterns ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Pattern>>

    @Query("SELECT * FROM patterns WHERE id = :id")
    fun observeById(id: Long): Flow<Pattern?>

    @Query("SELECT * FROM patterns WHERE id = :id")
    suspend fun getById(id: Long): Pattern?

    @Insert
    suspend fun insert(pattern: Pattern): Long

    @Update
    suspend fun update(pattern: Pattern)

    @Query("DELETE FROM patterns WHERE id = :id")
    suspend fun delete(id: Long)
}
