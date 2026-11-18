package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class DownloadGroupProgressTest {
    private fun row(id: String, percent: Float = -1f, bytes: Long = 0, total: Long = -1,
        transfer: TransferState = TransferState.DOWNLOADING, save: SaveState = SaveState.NONE,
        availability: Availability = Availability.STAGING) = DownloadRow(id, id, transfer, percent,
        bytes = bytes, totalBytes = total, save = save, availability = availability)

    @Test fun activeProgressUsesBytesAndDoesNotCountSavedEpisodes() {
        val result = downloadGroupProgress(listOf(
            row("saved", 100f, availability = Availability.SAVED),
            row("small", 50f, bytes = 50, total = 100),
            row("large", 10f, bytes = 90, total = 900)))
        assertEquals("Downloading", result.label)
        assertEquals(14f, result.percent!!, 0.01f)
        assertTrue(result.running)
    }
    @Test fun savingProgressDescribesSavingInsteadOfCompletedTransfer() {
        val result = downloadGroupProgress(listOf(row("saving", 34f, transfer = TransferState.COMPLETE, save = SaveState.COPYING), row("downloading", 70f)))
        assertEquals("Saving", result.label)
        assertEquals(34f, result.percent!!, 0.01f)
    }
    @Test fun unknownProgressIsIndeterminateAndPausedIsNotAnimated() {
        assertNull(downloadGroupProgress(listOf(row("unknown"))).percent)
        assertFalse(downloadGroupProgress(listOf(row("paused", transfer = TransferState.PAUSED))).running)
        assertEquals("Paused", downloadGroupProgress(listOf(row("paused", transfer = TransferState.PAUSED))).label)
    }
    @Test fun savedGroupCanStillBePreparingAdditionalLinks() {
        val saved = listOf(row("saved", availability = Availability.SAVED))
        assertEquals("Saved", downloadGroupProgress(saved).label)
        assertEquals("Preparing links", downloadGroupProgress(saved, preparingLinks = true).label)
        assertTrue(downloadGroupProgress(saved, preparingLinks = true).running)
    }

    @Test fun hlsUsesSelectedTrackSamplesInsteadOfAnUnverifiedTransferTotal() {
        assertEquals(-1L, displayTransferTotal(250_000_000, 1_100_000_000, hls = true))
        assertEquals(500_000_000L, displayTransferTotal(250_000_000, 500_000_000, hls = false))
        assertNull(projectHlsTrackSize(60_000_000, 1_000_000, 30_000_000))
        val projected = projectHlsTrackSize(60_000_000, 30_000_000, 250_000_000)
        assertEquals(500_000_000L, projected)
        assertEquals(50f, estimatedTransferProgress(22.7f, 250_000_000, projected!!), 0.01f)
        assertEquals(99f, estimatedTransferProgress(99f, 500_000_000, projected), 0.01f)
        assertEquals(-1f, normalizedProgress(Float.NaN), 0.01f)
    }
}
