package com.team.pricecompare.data.repo

import com.team.pricecompare.parsers.CollectionOrchestrator
import com.team.pricecompare.parsers.CollectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeCollectionOrchestrator : CollectionOrchestrator {

    override fun collect(storeName: String): Flow<CollectionState> = flow {
        val platforms = listOf("meituan", "flash")
        for (platform in platforms) {
            emit(CollectionState.InProgress(platform, "拉起 App…"))
            delay(800)
            emit(CollectionState.InProgress(platform, "搜索店铺…"))
            delay(800)
            emit(CollectionState.InProgress(platform, "抓取菜单…"))
            delay(800)
        }
        emit(CollectionState.Completed(FixtureProvider.all()))
    }
}
