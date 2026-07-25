package com.team.pricecompare.parsers

import com.team.pricecompare.data.repo.FakeCollectionOrchestrator
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCollectionOrchestratorTest {

    @Test
    fun completesWithAllPlatforms() = runTest {
        val orch = FakeCollectionOrchestrator()
        val states = orch.collect("老王盖码饭（示范店）").toList()
        val last = states.last()
        assertTrue(last is CollectionState.Completed)
        assertEquals(2, (last as CollectionState.Completed).stores.size)
        assertTrue(states.any { it is CollectionState.InProgress && it.platform == "meituan" })
        assertTrue(states.any { it is CollectionState.InProgress && it.platform == "flash" })
    }
}
