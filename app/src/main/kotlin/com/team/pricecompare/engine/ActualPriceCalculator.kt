package com.team.pricecompare.engine

import com.team.pricecompare.data.Deal
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.UserDealInput

object ActualPriceCalculator {

    fun calculate(store: StoreInfo, input: UserDealInput = UserDealInput()): Deal {
        val breakdown = mutableListOf<String>()

        val itemsSubtotal = store.items.sumOf { it.price }
        breakdown.add("商品小计: ¥${fmt(itemsSubtotal)}")

        val packageTotal = store.items.sumOf { it.packageFee }
        breakdown.add("包装费: ¥${fmt(packageTotal)}")

        val delivery = store.deliveryFee
        breakdown.add("配送费: ¥${fmt(delivery)}")

        val subtotal = itemsSubtotal + packageTotal + delivery

        val discount = pickBestDiscount(store.discounts, subtotal)
        if (discount > 0.0) {
            breakdown.add("满减: -¥${fmt(discount)}")
        }

        val redPacket = input.redPacket
        if (redPacket > 0.0) {
            breakdown.add("红包: -¥${fmt(redPacket)}")
        }

        val coupon = input.selectedCoupon
        if (coupon > 0.0) {
            breakdown.add("券: -¥${fmt(coupon)}")
        }

        val finalPrice = (subtotal - discount - redPacket - coupon).coerceAtLeast(0.0)
        breakdown.add("实付: ¥${fmt(finalPrice)}")

        return Deal(
            platform = store.platform,
            finalPrice = finalPrice,
            breakdown = breakdown,
        )
    }

    private fun pickBestDiscount(descriptions: List<String>, subtotal: Double): Double {
        var best = 0.0
        for (desc in descriptions) {
            val (threshold, off) = parseManJian(desc) ?: continue
            if (subtotal >= threshold && off > best) best = off
        }
        return best
    }

    private val manJianRegex = Regex("满(\\d+(?:\\.\\d+)?)\\s*减(\\d+(?:\\.\\d+)?)")

    private fun parseManJian(text: String): Pair<Double, Double>? {
        val m = manJianRegex.find(text) ?: return null
        val threshold = m.groupValues[1].toDoubleOrNull() ?: return null
        val off = m.groupValues[2].toDoubleOrNull() ?: return null
        return threshold to off
    }

    private fun fmt(v: Double): String = "%.2f".format(v)
}
