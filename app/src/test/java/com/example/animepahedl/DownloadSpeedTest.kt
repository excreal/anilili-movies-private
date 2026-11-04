package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadSpeedTest {
    @Test fun reportsBytesPerSecondAndFallsToZeroWhenTrafficStops() {
        DownloadSpeed.reset()
        assertEquals(0L, DownloadSpeed.sample(1000))
        DownloadSpeed.add(2048)
        assertEquals(2048L, DownloadSpeed.sample(2000))
        assertEquals(0L, DownloadSpeed.sample(3000))
    }
}
