package com.team.pricecompare.parsers

import android.view.accessibility.AccessibilityNodeInfo
import com.team.pricecompare.data.StoreInfo

interface ParserInterface {
    fun parse(root: AccessibilityNodeInfo?): StoreInfo?
}
