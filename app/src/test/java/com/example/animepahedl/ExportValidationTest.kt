package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class ExportValidationTest {
    @Test fun rejectsOnlyTheOpeningSegmentOfAnEpisode() {
        assertFalse(isExportDurationComplete(24 * 60_000, 30_000))
        assertFalse(isExportDurationComplete(24 * 60_000, 23 * 60_000))
    }
    @Test fun allowsOnlySmallTimestampDifferences() {
        assertTrue(isExportDurationComplete(24 * 60_000, 24 * 60_000 - 1500))
        assertTrue(isExportDurationComplete(3000, 2900))
        assertFalse(isExportDurationComplete(3000, 2000))
    }
    @Test fun rejectsMissingOrEmptyDurations() {
        assertFalse(isExportDurationComplete(0, 3000))
        assertFalse(isExportDurationComplete(3000, 0))
    }
}
