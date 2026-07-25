package com.team.pricecompare.engine.strategy

import com.team.pricecompare.data.UserDealInput
import com.team.pricecompare.data.repo.FixtureProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyRecommenderTest {

    @Test
    fun picksLowerFinalPrice() {
        val stores = listOf(FixtureProvider.meituanStore, FixtureProvider.flashStore)
        val s = StrategyRecommender.recommend(stores)
        assertEquals("meituan", s.bestPlatform)
        assertEquals(53.5, s.bestFinalPrice, 1e-6)
        assertEquals(2, s.perPlatform.size)
        assertTrue(s.reason.contains("美团"))
    }

    @Test
    fun emptyStoresReturnsNoData() {
        val s = StrategyRecommender.recommend(emptyList())
        assertNull(s.bestPlatform)
        assertTrue(s.reason.contains("无可比"))
    }

    @Test
    fun redPacketRecalculatesBest() {
        val stores = listOf(FixtureProvider.meituanStore)
        val s = StrategyRecommender.recommend(stores, UserDealInput(redPacket = 10.0))
        // 63.5 满减 10 → 53.5；再减红包 10 → 43.5
        assertEquals(43.5, s.bestFinalPrice, 1e-6)
    }
}
