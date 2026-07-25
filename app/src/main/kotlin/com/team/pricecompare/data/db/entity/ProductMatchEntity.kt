package com.team.pricecompare.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_matches",
    indices = [Index("nameA"), Index("nameB")]
)
data class ProductMatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformA: String,
    val nameA: String,
    val platformB: String,
    val nameB: String,
    val confirmedAt: Long,
)
