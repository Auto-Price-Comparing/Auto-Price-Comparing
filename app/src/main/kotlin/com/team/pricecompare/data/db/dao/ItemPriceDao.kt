package com.team.pricecompare.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import com.team.pricecompare.data.db.entity.ItemPriceEntity

@Dao
interface ItemPriceDao {
    @Insert
    suspend fun insert(item: ItemPriceEntity): Long

    @Insert
    suspend fun insertAll(items: List<ItemPriceEntity>): List<Long>
}
