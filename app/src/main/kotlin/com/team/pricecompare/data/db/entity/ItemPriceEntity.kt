package com.team.pricecompare.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_prices",
    foreignKeys = [
        ForeignKey(
            entity = StoreSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("snapshotId")]
)
data class ItemPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val name: String,
    val price: Double,
    val packageFee: Double,
)
