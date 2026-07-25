package com.team.pricecompare.engine.match

import com.team.pricecompare.data.ItemPrice
import com.team.pricecompare.data.StoreInfo
import com.team.pricecompare.engine.store.StoreNameNormalizer

data class NamePair(val nameA: String, val nameB: String)

data class ItemMatch(
    val itemA: ItemPrice?,
    val itemB: ItemPrice?,
    val score: Double,
    val needsConfirm: Boolean,
) {
    val matched: Boolean get() = itemA != null && itemB != null
}

object ProductMatcher {

    private const val AUTO_THRESHOLD = 0.85
    private const val CONFIRM_THRESHOLD = 0.6

    fun match(
        a: StoreInfo,
        b: StoreInfo,
        confirmed: Set<NamePair> = emptySet(),
    ): List<ItemMatch> {
        val usedB = mutableSetOf<Int>()
        val result = mutableListOf<ItemMatch>()

        for (itemA in a.items) {
            var bestIdx = -1
            var bestScore = 0.0
            for ((idx, itemB) in b.items.withIndex()) {
                if (idx in usedB) continue
                val score = if (isConfirmed(confirmed, itemA.name, itemB.name)) {
                    1.0
                } else {
                    similarity(itemA.name, itemB.name)
                }
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = idx
                }
            }
            if (bestIdx == -1) {
                result.add(ItemMatch(itemA, null, 0.0, false))
                continue
            }
            val itemB = b.items[bestIdx]
            when {
                bestScore >= AUTO_THRESHOLD -> {
                    usedB.add(bestIdx)
                    result.add(ItemMatch(itemA, itemB, bestScore, false))
                }
                bestScore >= CONFIRM_THRESHOLD -> {
                    usedB.add(bestIdx)
                    result.add(ItemMatch(itemA, itemB, bestScore, true))
                }
                else -> result.add(ItemMatch(itemA, null, bestScore, false))
            }
        }

        for ((idx, itemB) in b.items.withIndex()) {
            if (idx in usedB) continue
            result.add(ItemMatch(null, itemB, 0.0, false))
        }
        return result
    }

    private fun isConfirmed(confirmed: Set<NamePair>, a: String, b: String) =
        confirmed.contains(NamePair(a, b)) || confirmed.contains(NamePair(b, a))

    private fun similarity(a: String, b: String): Double {
        val na = StoreNameNormalizer.normalize(a)
        val nb = StoreNameNormalizer.normalize(b)
        val maxLen = maxOf(na.length, nb.length)
        if (maxLen == 0) return 1.0
        val d = levenshtein(na, nb)
        return 1.0 - d.toDouble() / maxLen
    }

    private fun levenshtein(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n
        var prev = IntArray(m + 1) { it }
        val curr = IntArray(m + 1)
        for (i in 1..n) {
            curr[0] = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            for (j in 0..m) prev[j] = curr[j]
        }
        return prev[m]
    }
}
