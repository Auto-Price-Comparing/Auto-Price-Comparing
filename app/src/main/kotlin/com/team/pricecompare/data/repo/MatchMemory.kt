package com.team.pricecompare.data.repo

import com.team.pricecompare.data.db.AppDatabase
import com.team.pricecompare.data.db.entity.ProductMatchEntity
import com.team.pricecompare.engine.match.NamePair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchMemory private constructor(
    private val db: AppDatabase,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _confirmed = MutableStateFlow<Set<NamePair>>(emptySet())
    val confirmed: StateFlow<Set<NamePair>> = _confirmed.asStateFlow()

    init {
        scope.launch { _confirmed.value = db.productMatchDao().all().map { NamePair(it.nameA, it.nameB) }.toSet() }
    }

    fun confirm(nameA: String, nameB: String, platformA: String, platformB: String) {
        scope.launch {
            db.productMatchDao().insert(
                ProductMatchEntity(
                    platformA = platformA,
                    nameA = nameA,
                    platformB = platformB,
                    nameB = nameB,
                    confirmedAt = System.currentTimeMillis(),
                )
            )
            _confirmed.value = _confirmed.value + NamePair(nameA, nameB)
        }
    }

    companion object {
        @Volatile private var INSTANCE: MatchMemory? = null

        fun get(db: AppDatabase): MatchMemory =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MatchMemory(db).also { INSTANCE = it }
            }
    }
}
