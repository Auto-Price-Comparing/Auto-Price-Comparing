package com.team.pricecompare.engine.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreNameNormalizerTest {

    @Test
    fun removesBracketedContent() {
        assertEquals("老王盖码饭", StoreNameNormalizer.normalize("老王盖码饭（示范店）"))
    }

    @Test
    fun fullwidthDigitsToHalf() {
        assertEquals("1号店", StoreNameNormalizer.normalize("１号店"))
    }

    @Test
    fun separatorsToSpaces() {
        assertEquals("老王 盖码饭", StoreNameNormalizer.normalize("老王·盖码饭"))
    }

    @Test
    fun collapsesWhitespace() {
        assertEquals("a b", StoreNameNormalizer.normalize("a   b"))
    }

    @Test
    fun lowercasesAscii() {
        assertEquals("abc", StoreNameNormalizer.normalize("ABC"))
    }

    @Test
    fun similarHandlesBracketedVariant() {
        assertTrue(StoreNameNormalizer.similar("老王盖码饭（示范店）", "老王盖码饭"))
    }

    @Test
    fun similarFalseForDifferent() {
        assertFalse(StoreNameNormalizer.similar("老王盖码饭", "张三烧烤"))
    }
}
