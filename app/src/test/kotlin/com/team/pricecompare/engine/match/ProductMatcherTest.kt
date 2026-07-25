package com.team.pricecompare.engine.match

import com.team.pricecompare.data.ItemPrice
import com.team.pricecompare.data.StoreInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMatcherTest {

    private fun store(platform: String, items: List<ItemPrice>): StoreInfo =
        StoreInfo(platform, "S", 4.0, 0, 0.0, 0.0, emptyList(), items, 0L)

    private val a = store("meituan", listOf(
        ItemPrice("辣椒炒肉盖饭", 18.0, 1.0),
        ItemPrice("红烧肉饭", 22.0, 1.0),
        ItemPrice("可乐", 4.0, 0.0),
    ))
    private val b = store("flash", listOf(
        ItemPrice("辣椒炒肉盖饭", 17.0, 1.0),
        ItemPrice("红烧肉盖饭", 21.0, 1.0),
        ItemPrice("紫菜蛋花汤", 6.0, 0.0),
    ))

    @Test
    fun exactNamesAutoMatch() {
        val matches = ProductMatcher.match(a, b)
        val m = matches.first { it.itemA?.name == "辣椒炒肉盖饭" }
        assertEquals("辣椒炒肉盖饭", m.itemB?.name)
        assertTrue(m.matched)
        assertFalse(m.needsConfirm)
        assertEquals(1.0, m.score, 1e-6)
    }

    @Test
    fun fuzzyNameNeedsConfirm() {
        val matches = ProductMatcher.match(a, b)
        val m = matches.first { it.itemA?.name == "红烧肉饭" }
        assertEquals("红烧肉盖饭", m.itemB?.name)
        assertTrue(m.needsConfirm)
    }

    @Test
    fun unmatchedLeftoversReported() {
        val matches = ProductMatcher.match(a, b)
        val unmatchedA = matches.first { it.itemA?.name == "可乐" }
        assertFalse(unmatchedA.matched)
        assertEquals(null, unmatchedA.itemB)
        val unmatchedB = matches.first { it.itemB?.name == "紫菜蛋花汤" }
        assertFalse(unmatchedB.matched)
        assertEquals(null, unmatchedB.itemA)
    }

    @Test
    fun confirmedPairAutoAccepted() {
        val confirmed = setOf(NamePair("红烧肉饭", "红烧肉盖饭"))
        val matches = ProductMatcher.match(a, b, confirmed)
        val m = matches.first { it.itemA?.name == "红烧肉饭" }
        assertEquals("红烧肉盖饭", m.itemB?.name)
        assertFalse(m.needsConfirm)
        assertEquals(1.0, m.score, 1e-6)
    }

    @Test
    fun fixturesAllMatchAutomatically() {
        val meituan = com.team.pricecompare.data.repo.FixtureProvider.meituanStore
        val flash = com.team.pricecompare.data.repo.FixtureProvider.flashStore
        val matches = ProductMatcher.match(meituan, flash)
        val auto = matches.count { it.matched && !it.needsConfirm }
        assertEquals(meituan.items.size, auto)
        assertTrue(matches.all { !it.needsConfirm })
    }
}
