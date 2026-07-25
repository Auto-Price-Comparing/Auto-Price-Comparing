package com.team.pricecompare.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayControllerMatchTest {

    private val expected =
        "com.team.pricecompare/com.team.pricecompare.parsers.CollectorAccessibilityService"

    @Test
    fun nullOrBlankReturnsFalse() {
        assertEquals(false, OverlayController.matchesEnabledService(null, expected))
        assertEquals(false, OverlayController.matchesEnabledService("", expected))
        assertEquals(false, OverlayController.matchesEnabledService("   ", expected))
    }

    @Test
    fun singleMatch() {
        assertEquals(true, OverlayController.matchesEnabledService(expected, expected))
    }

    @Test
    fun multipleSeparatedByColon() {
        val raw = "com.other/com.other.Svc:$expected:com.foo/com.foo.Svc"
        assertEquals(true, OverlayController.matchesEnabledService(raw, expected))
    }

    @Test
    fun caseInsensitive() {
        val raw = expected.uppercase()
        assertEquals(true, OverlayController.matchesEnabledService(raw, expected))
    }

    @Test
    fun surroundingWhitespace() {
        val raw = "  $expected  "
        assertEquals(true, OverlayController.matchesEnabledService(raw, expected))
    }

    @Test
    fun prefixSubstringDoesNotMatch() {
        val expectedA = "com.foo/com.foo.Svc"
        val raw = "com.foo/com.foo.SvcX"
        assertEquals(false, OverlayController.matchesEnabledService(raw, expectedA))
    }

    @Test
    fun noMatch() {
        val raw = "com.other/com.other.Svc"
        assertEquals(false, OverlayController.matchesEnabledService(raw, expected))
    }
}
