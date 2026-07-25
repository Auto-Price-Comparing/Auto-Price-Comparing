package com.team.pricecompare.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_snapshots")
data class StoreSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val storeName: String,
    val rating: Double,
    val monthlySales: Int,
    val deliveryFee: Double,
    val minOrder: Double,
    val capturedAt: Long,
)
