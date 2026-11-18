package com.example.animepahedl

import androidx.media3.exoplayer.offline.Downloader
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DeferredDownloaderTest {
    private class Fake(private val failure: Exception? = null) : Downloader {
        var downloads = 0
        var cancelled = false
        override fun download(progressListener: Downloader.ProgressListener?) {
            downloads++
            failure?.let { throw it }
        }
        override fun cancel() { cancelled = true }
        override fun remove() = Unit
    }

    @Test fun badRestoredItemFailsOnlyWhenItsTaskRunsAndOtherItemsStillDownload() {
        var attempted = false
        val bad = DeferredDownloader({ attempted = true; error("Missing saved source context") }, {})
        assertFalse(attempted)
        try { bad.download(null); fail("Expected item failure") } catch (_: IllegalStateException) { }
        val good = Fake()
        var creations = 0
        val next = DeferredDownloader({ creations++; good }, {})
        next.download(null)
        next.download(null)
        assertEquals(1, creations)
        assertEquals(2, good.downloads)
    }

    @Test fun removalDoesNotLoadBrokenSourceOrRequireOutputFolder() {
        var removed = false
        DeferredDownloader({ error("Missing metadata or folder permission") }, { removed = true }).remove()
        assertTrue(removed)
    }

    @Test fun expiredUrlIsRefreshedOnlyInsideTheRunningTransfer() {
        val expired = java.io.IOException("expired")
        val original = Fake(expired)
        val replacement = Fake()
        var resolutions = 0
        val item = DeferredDownloader({ resolutions++; original }, {}, { resolutions++; replacement }) { it === expired }
        assertEquals(0, resolutions)
        item.download(null)
        assertEquals(2, resolutions)
        assertEquals(1, original.downloads)
        assertEquals(1, replacement.downloads)
    }

    @Test fun aSecondExpiredUrlFailsWithoutAnotherRefresh() {
        val expired = java.io.IOException("expired")
        var refreshes = 0
        val item = DeferredDownloader({ Fake(expired) }, {}, { refreshes++; Fake(expired) }) { it === expired }
        repeat(2) {
            try { item.download(null); fail("Expected expired URL failure") } catch (_: MediaLinkExpired) { }
        }
        assertEquals(1, refreshes)
    }

    @Test fun media3RetryContinuesRenewalAfterTemporaryProviderFailure() {
        val expired = java.io.IOException("CDN link expired")
        val offline = java.io.IOException("Provider temporarily unavailable")
        val original = Fake(expired)
        val replacement = Fake()
        var refreshes = 0
        val item = DeferredDownloader({ original }, {}, {
            if (++refreshes == 1) throw offline
            replacement
        }) { it === expired }
        assertSame(offline, assertThrows(java.io.IOException::class.java) { item.download(null) })
        item.download(null)
        assertEquals(2, refreshes)
        assertEquals(1, original.downloads)
        assertEquals(1, replacement.downloads)
    }

    @Test fun cancellationBeforeSetupDoesNotStartDownload() {
        val item = DeferredDownloader({ error("Must not initialize") }, {})
        item.cancel()
        try { item.download(null); fail("Expected cancellation") } catch (_: InterruptedException) { }
    }

    @Test fun cancellationDuringSetupCancelsNewDelegateBeforeTransfer() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val fake = Fake()
        val failure = AtomicReference<Throwable>()
        val item = DeferredDownloader({
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            fake
        }, {})
        val worker = thread {
            try { item.download(null) } catch (error: Throwable) { failure.set(error) }
        }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            item.cancel()
        } finally { release.countDown(); worker.join(5_000) }
        assertFalse(worker.isAlive)
        assertTrue(failure.get() is InterruptedException)
        assertTrue(fake.cancelled)
        assertEquals(0, fake.downloads)
    }
}
