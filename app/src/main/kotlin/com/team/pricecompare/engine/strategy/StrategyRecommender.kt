package com.team.pricecompare.engine.strategy

import com.team.pricecompare.data.Deal
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.data.UserDealInput
import com.team.pricecompare.engine.ActualPriceCalculator

data class Strategy(
    val bestPlatform: String?,
    val bestFinalPrice: Double,
    val reason: String,
    val perPlatform: List<Deal>,
)

object StrategyRecommender {

    fun recommend(
        stores: List<StoreInfo>,
        input: UserDealInput = UserDealInput(),
    ): Strategy {
        val deals = stores.map { ActualPriceCalculator.calculate(it, input) }
        val best = deals.minByOrNull { it.finalPrice }
        return Strategy(
            bestPlatform = best?.platform,
            bestFinalPrice = best?.finalPrice ?: 0.0,
            reason = best?.let {
                "${label(it.platform)} 实付最低 ¥${"%.2f".format(it.finalPrice)}"
            } ?: "无可比数据",
            perPlatform = deals,
        )
    }

    private fun label(platform: String): String = when (platform) {
        "meituan" -> "美团"
        "flash" -> "淘宝闪购"
        else -> platform
    }
}
