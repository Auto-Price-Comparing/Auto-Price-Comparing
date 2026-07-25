package com.team.pricecompare.parsers

import android.view.accessibility.AccessibilityNodeInfo
import com.team.pricecompare.data.StoreInfo

object SafeParse {

    fun parse(parser: ParserInterface, root: AccessibilityNodeInfo?): StoreInfo? = try {
        parser.parse(root)
    } catch (_: Throwable) {
        null
    }
}
