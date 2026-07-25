package com.team.pricecompare.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.team.pricecompare.data.db.entity.ProductMatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductMatchDao {
    @Insert
    suspend fun insert(match: ProductMatchEntity): Long

    @Query("SELECT * FROM product_matches")
    fun observeAll(): Flow<List<ProductMatchEntity>>

    @Query("SELECT * FROM product_matches")
    suspend fun all(): List<ProductMatchEntity>
}
