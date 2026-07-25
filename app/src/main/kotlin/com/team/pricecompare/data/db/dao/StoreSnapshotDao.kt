package com.team.pricecompare.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.team.pricecompare.data.db.entity.StoreSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSnapshotDao {
    @Insert
    suspend fun insert(snapshot: StoreSnapshotEntity): Long

    @Query("SELECT * FROM store_snapshots ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<StoreSnapshotEntity>>

    @Query("SELECT * FROM store_snapshots WHERE storeName = :name ORDER BY capturedAt DESC LIMIT :limit")
    fun observeByName(name: String, limit: Int): Flow<List<StoreSnapshotEntity>>

    @Query("SELECT COUNT(*) FROM store_snapshots")
    suspend fun count(): Int
}
