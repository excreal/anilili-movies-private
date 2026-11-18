package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class FfmpegProgressTest {
    @Test fun completeReportsCombineTimeAndBytesWithoutCompletingBeforeValidation() {
        val parser = FfmpegProgressParser(60_000_000)
        assertNull(parser.accept("total_size=250000000"))
        assertNull(parser.accept("out_time_us=30000000"))
        assertEquals(FfmpegProgress(30_000_000, 250_000_000, 50), parser.accept("progress=continue"))
        parser.accept("total_size=500000000")
        parser.accept("out_time_us=60000123")
        assertEquals(FfmpegProgress(60_000_123, 500_000_000, 99), parser.accept("progress=end"))
    }

    @Test fun timestampFallbackDoesNotMisreadLegacyUnitsOrRegress() {
        val parser = FfmpegProgressParser(60_000_000)
        parser.accept("out_time_us=N/A")
        parser.accept("out_time_ms=12000000")
        parser.accept("total_size=N/A")
        assertEquals(FfmpegProgress(12_000_000, 0, 20), parser.accept("progress=continue"))
        parser.accept("out_time_us=-1")
        parser.accept("out_time=00:00:15.25")
        assertEquals(FfmpegProgress(15_250_000, 0, 25), parser.accept("progress=continue"))
        parser.accept("out_time_us=14000000")
        parser.accept("total_size=-1")
        assertNull(parser.accept("progress=continue"))
    }
}
