package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class OrderedWritesTest {
    @Test fun failedHeadCannotBeBypassedOrForgottenByLaterWrites() {
        val writes = OrderedWrites()
        val committed = mutableListOf<Int>()
        var diskFull = true
        writes.enqueue { if (diskFull) throw java.io.IOException("full"); committed += 1 }
        writes.enqueue { committed += 2 }
        repeat(2) { writes.drain(); assertNotNull(writes.failure); assertTrue(committed.isEmpty()) }
        diskFull = false
        writes.drain()
        assertNull(writes.failure)
        assertEquals(listOf(1, 2), committed)
        writes.drain()
        assertEquals(listOf(1, 2), committed)
    }
}
