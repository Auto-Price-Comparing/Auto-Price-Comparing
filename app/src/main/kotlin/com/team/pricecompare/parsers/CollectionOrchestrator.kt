package com.team.pricecompare.parsers

import com.team.pricecompare.data.StoreInfo
import kotlinx.coroutines.flow.Flow

sealed class CollectionState {
    data class InProgress(val platform: String, val step: String) : CollectionState()
    data class Completed(val stores: List<StoreInfo>) : CollectionState()
    data class Failed(val platform: String, val reason: String) : CollectionState()
}

interface CollectionOrchestrator {
    fun collect(storeName: String): Flow<CollectionState>
}
