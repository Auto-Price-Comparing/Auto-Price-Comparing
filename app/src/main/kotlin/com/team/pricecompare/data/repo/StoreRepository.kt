package com.team.pricecompare.data.repo

import com.team.pricecompare.data.ItemPrice
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.db.AppDatabase
import com.team.pricecompare.data.db.entity.ItemPriceEntity
import com.team.pricecompare.data.db.entity.StoreSnapshotEntity
import com.team.pricecompare.engine.SnapshotPricer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class HistoryPoint(val capturedAt: Long, val subtotal: Double)

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

    suspend fun recordAll(stores: List<StoreInfo>) {
        stores.forEach { persist(it) }
    }

    fun historyFor(storeName: String, limit: Int = 30): Flow<List<HistoryPoint>> =
        db.storeSnapshotDao().observeByName(storeName, limit).map { snaps ->
            snaps.map { snap ->
                val items = db.itemPriceDao().findBySnapshot(snap.id)
                    .map { ItemPrice(it.name, it.price, it.packageFee) }
                HistoryPoint(snap.capturedAt, SnapshotPricer.subtotal(snap.deliveryFee, items))
            }
        }

    suspend fun seedIfEmpty() {
        if (db.storeSnapshotDao().count() > 0) return
        val base = FixtureProvider.meituanStore
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        for (i in 0..6) {
            val drift = 1.0 + (i - 3) * 0.02
            val demo = base.copy(
                capturedAt = now - (6 - i) * dayMs,
                items = base.items.map { it.copy(price = round2(it.price * drift)) },
            )
            persist(demo)
        }
    }

    private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

    companion object {
        private const val DEDUP_WINDOW_MS = 5000L
        @Volatile private var INSTANCE: StoreRepository? = null

        fun get(db: AppDatabase): StoreRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: StoreRepository(db).also { INSTANCE = it }
            }
    }
}
