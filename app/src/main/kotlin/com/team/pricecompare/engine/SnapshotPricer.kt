package com.team.pricecompare.engine

import com.team.pricecompare.data.ItemPrice
import com.team.pricecompare.data.StoreInfo

object SnapshotPricer {

    fun subtotal(deliveryFee: Double, items: List<ItemPrice>): Double =
        items.sumOf { it.price + it.packageFee } + deliveryFee

    fun subtotal(store: StoreInfo): Double = subtotal(store.deliveryFee, store.items)
}
