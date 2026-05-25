package com.lachlan.stitchstash.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lachlan.stitchstash.data.db.entities.Sticker
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers ORDER BY earnedAt DESC")
    fun observeAll(): Flow<List<Sticker>>

    @Query("SELECT * FROM stickers WHERE type = :type LIMIT 1")
    suspend fun firstOfType(type: String): Sticker?

    @Query("SELECT COUNT(*) FROM stickers WHERE type = :type")
    suspend fun countOfType(type: String): Int

    @Insert
    suspend fun insert(sticker: Sticker): Long

    @Query("DELETE FROM stickers WHERE id = :id")
    suspend fun delete(id: Long)
}
