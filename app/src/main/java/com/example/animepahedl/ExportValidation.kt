package com.example.animepahedl

// Allow timestamp rounding and a trailing partial segment, but reject truncated episodes.
internal fun isExportDurationComplete(expectedMs: Long, actualMs: Long): Boolean =
    expectedMs > 0 && actualMs > 0 && actualMs >= expectedMs - minOf(2000L, expectedMs / 20)
