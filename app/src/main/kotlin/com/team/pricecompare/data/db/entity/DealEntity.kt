package com.team.pricecompare.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deals")
data class DealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val finalPrice: Double,
    val breakdownCsv: String,
    val capturedAt: Long,
)
