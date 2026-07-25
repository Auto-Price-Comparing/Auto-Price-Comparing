package com.team.pricecompare.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.team.pricecompare.data.db.dao.DealDao
import com.team.pricecompare.data.db.dao.ItemPriceDao
import com.team.pricecompare.data.db.dao.StoreSnapshotDao
import com.team.pricecompare.data.db.entity.DealEntity
import com.team.pricecompare.data.db.entity.ItemPriceEntity
import com.team.pricecompare.data.db.entity.StoreSnapshotEntity

@Database(
    entities = [
        StoreSnapshotEntity::class,
        ItemPriceEntity::class,
        DealEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeSnapshotDao(): StoreSnapshotDao
    abstract fun itemPriceDao(): ItemPriceDao
    abstract fun dealDao(): DealDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pricecompare.db"
                ).build().also { INSTANCE = it }
            }
    }
}
