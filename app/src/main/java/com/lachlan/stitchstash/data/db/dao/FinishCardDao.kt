package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lachlan.stitchstash.data.db.entities.FinishCard
import kotlinx.coroutines.flow.Flow

@Dao
interface FinishCardDao {
    @Query("SELECT * FROM finish_cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FinishCard>>

    @Query("SELECT * FROM finish_cards WHERE id = :id")
    suspend fun getById(id: Long): FinishCard?

    @Insert
    suspend fun insert(card: FinishCard): Long

    @Query("DELETE FROM finish_cards WHERE id = :id")
    suspend fun delete(id: Long)
}
