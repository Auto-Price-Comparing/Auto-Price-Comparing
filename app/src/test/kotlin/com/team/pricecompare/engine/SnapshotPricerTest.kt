package com.team.pricecompare.engine

import com.team.pricecompare.data.ItemPrice
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotPricerTest {

    @Test
    fun sumsItemsAndDelivery() {
        val items = listOf(
            ItemPrice("A", 10.0, 1.0),
            ItemPrice("B", 20.0, 2.0),
        )
        assertEquals(38.0, SnapshotPricer.subtotal(5.0, items), 1e-6)
    }

    @Test
    fun emptyItemsOnlyDelivery() {
        assertEquals(3.0, SnapshotPricer.subtotal(3.0, emptyList()), 1e-6)
    }

    @Test
    fun fixtureMeituanSubtotal() {
        val s = com.team.pricecompare.data.repo.FixtureProvider.meituanStore
        assertEquals(63.5, SnapshotPricer.subtotal(s), 1e-6)
    }
}
