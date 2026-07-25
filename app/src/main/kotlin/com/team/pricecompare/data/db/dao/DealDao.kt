package com.team.pricecompare.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.team.pricecompare.data.db.entity.DealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DealDao {
    @Insert
    suspend fun insert(deal: DealEntity): Long

    @Query("SELECT * FROM deals ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<DealEntity>>
}
