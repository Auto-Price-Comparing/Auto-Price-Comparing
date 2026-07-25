package com.team.pricecompare.data.repo

import com.team.pricecompare.data.ItemPrice
import com.team.pricecompare.data.StoreInfo

object FixtureProvider {

    val meituanStore: StoreInfo = StoreInfo(
        platform = "meituan",
        storeName = "老王盖码饭（示范店）",
        rating = 4.6,
        monthlySales = 1200,
        deliveryFee = 3.0,
        minOrder = 20.0,
        discounts = listOf("满30减5", "满50减10"),
        items = listOf(
            ItemPrice("辣椒炒肉盖码饭", 18.0, 1.0),
            ItemPrice("红烧肉盖码饭", 22.0, 1.0),
            ItemPrice("酸辣土豆丝", 8.0, 0.5),
            ItemPrice("紫菜蛋花汤", 6.0, 0.0),
            ItemPrice("可乐", 4.0, 0.0),
        ),
        capturedAt = System.currentTimeMillis(),
    )

    val flashStore: StoreInfo = StoreInfo(
        platform = "flash",
        storeName = "老王盖码饭（示范店）",
        rating = 4.5,
        monthlySales = 980,
        deliveryFee = 2.0,
        minOrder = 15.0,
        discounts = listOf("满20减3"),
        items = listOf(
            ItemPrice("辣椒炒肉盖码饭", 17.5, 1.0),
            ItemPrice("红烧肉盖码饭", 21.0, 1.0),
            ItemPrice("酸辣土豆丝", 7.5, 0.5),
            ItemPrice("紫菜蛋花汤", 5.5, 0.0),
            ItemPrice("可乐", 3.5, 0.0),
        ),
        capturedAt = System.currentTimeMillis(),
    )

    fun all(): List<StoreInfo> = listOf(meituanStore, flashStore)
}
