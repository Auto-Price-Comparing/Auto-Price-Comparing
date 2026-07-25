package com.team.pricecompare.engine.store

import java.text.Normalizer

object StoreNameNormalizer {

    private val bracketContent = listOf(
        Regex("\\([^)]*\\)"),
        Regex("（[^）]*）"),
        Regex("\\[[^]]*]"),
        Regex("【[^】]*】"),
        Regex("\\{[^}]*}"),
        Regex("《[^》]*》"),
    )
    private val separators = Regex("[·・\\-_—–]+")
    private val whitespace = Regex("\\s+")

    fun normalize(name: String): String {
        var s = Normalizer.normalize(name, Normalizer.Form.NFKD)
        for (re in bracketContent) s = re.replace(s, " ")
        s = separators.replace(s, " ")
        s = whitespace.replace(s, " ").trim()
        return s.lowercase()
    }

    fun similar(a: String, b: String): Boolean = normalize(a) == normalize(b)
}
