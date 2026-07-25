package com.team.pricecompare.engine

import com.team.pricecompare.data.ItemPrice
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.UserDealInput
import com.team.pricecompare.data.repo.FixtureProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActualPriceCalculatorTest {

    private fun store(
        items: List<ItemPrice>,
        deliveryFee: Double = 0.0,
        discounts: List<String> = emptyList(),
    ): StoreInfo = StoreInfo(
        platform = "meituan",
        storeName = "测试店",
        rating = 4.5,
        monthlySales = 100,
        deliveryFee = deliveryFee,
        minOrder = 0.0,
        discounts = discounts,
        items = items,
        capturedAt = 0L,
    )

    @Test
    fun basicSubtotalWithPackageAndDelivery() {
        val s = store(
            items = listOf(
                ItemPrice("A", 10.0, 1.0),
                ItemPrice("B", 20.0, 2.0),
            ),
            deliveryFee = 5.0,
        )
        val deal = ActualPriceCalculator.calculate(s)
        assertEquals(38.0, deal.finalPrice, 1e-6)
    }

    @Test
    fun picksBestDiscountWhenThresholdMet() {
        val s = store(
            items = listOf(ItemPrice("A", 60.0, 0.0)),
            discounts = listOf("满30减5", "满50减10"),
        )
        val deal = ActualPriceCalculator.calculate(s)
        assertEquals(50.0, deal.finalPrice, 1e-6)
        assertTrue(deal.breakdown.any { it.contains("满减") })
    }

    @Test
    fun noDiscountWhenBelowThreshold() {
        val s = store(
            items = listOf(ItemPrice("A", 20.0, 0.0)),
            discounts = listOf("满30减5"),
        )
        val deal = ActualPriceCalculator.calculate(s)
        assertEquals(20.0, deal.finalPrice, 1e-6)
        assertFalse(deal.breakdown.any { it.contains("满减") })
    }

    @Test
    fun appliesRedPacketAndCoupon() {
        val s = store(items = listOf(ItemPrice("A", 50.0, 0.0)))
        val deal = ActualPriceCalculator.calculate(
            s, UserDealInput(redPacket = 5.0, selectedCoupon = 3.0)
        )
        assertEquals(42.0, deal.finalPrice, 1e-6)
        assertTrue(deal.breakdown.any { it.contains("红包") })
        assertTrue(deal.breakdown.any { it.contains("券") })
    }

    @Test
    fun finalPriceFlooredAtZero() {
        val s = store(items = listOf(ItemPrice("A", 10.0, 0.0)))
        val deal = ActualPriceCalculator.calculate(s, UserDealInput(redPacket = 100.0))
        assertEquals(0.0, deal.finalPrice, 1e-6)
    }

    @Test
    fun breakdownContainsFinalLine() {
        val s = store(items = listOf(ItemPrice("A", 10.0, 0.0)))
        val deal = ActualPriceCalculator.calculate(s)
        assertTrue(deal.breakdown.any { it.contains("实付") })
    }

    @Test
    fun ignoresUnparseableDiscountText() {
        val s = store(
            items = listOf(ItemPrice("A", 40.0, 0.0)),
            discounts = listOf("新客立减", "满30减5"),
        )
        val deal = ActualPriceCalculator.calculate(s)
        assertEquals(35.0, deal.finalPrice, 1e-6)
    }

    @Test
    fun fixtureMeituanMatchesExpected() {
        val deal = ActualPriceCalculator.calculate(FixtureProvider.meituanStore)
        // 58 + 2.5 + 3 = 63.5；满50减10 → 53.5
        assertEquals(53.5, deal.finalPrice, 1e-6)
    }

    @Test
    fun fixtureFlashMatchesExpected() {
        val deal = ActualPriceCalculator.calculate(FixtureProvider.flashStore)
        // 55 + 2.5 + 2 = 59.5；满20减3 → 56.5
        assertEquals(56.5, deal.finalPrice, 1e-6)
    }
}
