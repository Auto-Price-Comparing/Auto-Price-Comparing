package com.team.pricecompare.data

/**
 * 双方共享数据契约（见 AGENTS.md）。
 * 修改任何字段必须协商一致，并同步更新 fixtures。
 * 平台范围：meituan（美团）、flash（淘宝闪购）。
 */
data class ItemPrice(
    val name: String,
    val price: Double,
    val packageFee: Double,
)

data class StoreInfo(
    val platform: String,            // "meituan" | "flash"
    val storeName: String,
    val rating: Double,
    val monthlySales: Int,
    val deliveryFee: Double,
    val minOrder: Double,
    val discounts: List<String>,
    val items: List<ItemPrice>,
    val capturedAt: Long,
)

data class UserDealInput(
    val redPacket: Double = 0.0,
    val selectedCoupon: Double = 0.0,
)

data class Deal(
    val platform: String,            // "meituan" | "flash"
    val finalPrice: Double,
    val breakdown: List<String>,
)
