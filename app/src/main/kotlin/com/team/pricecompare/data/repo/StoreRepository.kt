package com.team.pricecompare.data.repo

import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.db.AppDatabase
import com.team.pricecompare.data.db.entity.ItemPriceEntity
import com.team.pricecompare.data.db.entity.StoreSnapshotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface StoreInfoEmitter {
    fun push(store: StoreInfo)
}

class StoreRepository private constructor(
    private val db: AppDatabase,
) : StoreInfoEmitter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastPersist = ConcurrentHashMap<String, Long>()

    private val _stores = MutableStateFlow<List<StoreInfo>>(emptyList())
    val stores: Flow<List<StoreInfo>> = _stores.asStateFlow()

    init {
        _stores.value = FixtureProvider.all()
    }

    override fun push(store: StoreInfo) {
        val current = _stores.value.toMutableList()
        current.removeAll { it.platform == store.platform }
        current.add(store)
        _stores.value = current

        val key = "${store.platform}|${store.storeName}"
        val now = System.currentTimeMillis()
        val last = lastPersist[key] ?: 0L
        if (now - last >= DEDUP_WINDOW_MS) {
            lastPersist[key] = now
            scope.launch { persist(store) }
        }
    }

    suspend fun persist(store: StoreInfo) {
        val snapshotId = db.storeSnapshotDao().insert(
            StoreSnapshotEntity(
                platform = store.platform,
                storeName = store.storeName,
                rating = store.rating,
                monthlySales = store.monthlySales,
                deliveryFee = store.deliveryFee,
                minOrder = store.minOrder,
                capturedAt = store.capturedAt,
            )
        )
        db.itemPriceDao().insertAll(
            store.items.map {
                ItemPriceEntity(
                    snapshotId = snapshotId,
                    name = it.name,
                    price = it.price,
                    packageFee = it.packageFee,
                )
            }
        )
    }

    companion object {
        private const val DEDUP_WINDOW_MS = 5000L
        @Volatile private var INSTANCE: StoreRepository? = null

        fun get(db: AppDatabase): StoreRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: StoreRepository(db).also { INSTANCE = it }
            }
    }
}
