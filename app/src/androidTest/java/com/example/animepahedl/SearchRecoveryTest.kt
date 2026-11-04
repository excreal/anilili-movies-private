package com.example.animepahedl

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class SearchRecoveryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun waitFor(condition: () -> Boolean) {
        val end = System.nanoTime() + 5_000_000_000L
        while (System.nanoTime() < end) { var met = false; main { met = condition() }; if (met) return; Thread.sleep(20) }
        main { assertTrue("Timed out", condition()) }
    }
    @Test fun typingIsUninterruptedAndVerificationRetriesLatestQueryOnlyOnce() {
        val calls = CopyOnWriteArrayList<String>()
        val fake = object : AnimeRepository() {
            override suspend fun search(query: String): List<Anime> { calls += query; throw VerificationRequired("https://example.org/api") }
        }
        lateinit var model: DownloaderViewModel
        main { model = DownloaderViewModel(instrumentation.targetContext.applicationContext as Application, fake); model.setForeground(true); model.updateQuery("nar") }
        waitFor { model.searchFailure != null }
        main { assertNull(model.verification); model.updateQuery("naruto") }
        waitFor { calls.contains("naruto") }
        main { assertNull(model.verification); assertEquals("naruto", model.query) }
        waitFor { model.verification != null }
        main { model.closeVerification(true) }
        waitFor { model.searchFailure?.message?.contains("still rejected") == true }
        Thread.sleep(1300)
        main { assertNull(model.verification); assertEquals("naruto", model.query); model.updateQuery(""); model.setForeground(false) }
        assertEquals(2, calls.count { it == "naruto" })
    }
    @Test fun navigatingAwayAndClearingInvalidatePendingVerification() {
        val fake = object : AnimeRepository() {
            override suspend fun search(query: String): List<Anime> = throw VerificationRequired("https://example.org/api")
        }
        lateinit var model: DownloaderViewModel
        main { model = DownloaderViewModel(instrumentation.targetContext.applicationContext as Application, fake); model.setForeground(true); model.updateQuery("one") }
        waitFor { model.searchFailure != null }
        main { model.showDownloads = true }
        Thread.sleep(1300)
        main { assertNull(model.verification); model.showDownloads = false; model.updateQuery("") }
        Thread.sleep(1300)
        main { assertNull(model.verification); assertNull(model.searchFailure); model.setForeground(false) }
    }
}
