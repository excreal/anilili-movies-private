package com.example.animepahedl

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class IndividualPreparationRecoveryTest {
    @Test fun legacyBatchMigratesAndRapidAdditionsKeepIndependentVideoJournals() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        DownloadStore.awaitReady()
        val fixture = "preparation-test-${System.nanoTime()}"
        val episodes = (1..3).map { SiteDownloadEpisode("$fixture-$it", it, "Episode $it", "https://example.invalid/video-$it.mp4") }
        val first = SiteDownloadSeries(SiteDownloadProvider.KISSKH, fixture, "Fixture", "", "https://example.invalid", episodes)
        val second = first.copy(id = "$fixture-other", title = "Another fixture")
        val legacy = SiteDownloadPreparation(first, episodes.take(2), "Best")
        val additions = listOf(SiteDownloadPreparation(first, episodes.drop(2), "Best"),
            SiteDownloadPreparation(second, episodes.take(1), "Best"))
        val ids = splitPreparation(legacy).keys + additions.flatMap { splitPreparation(it).keys }
        val kinds = listOf("preparation", "preparation-generation", "preparation-paused", "preparation-verification",
            "preparation-folder", "preparation-total", "preparation-failures", "preparation-error", "preparation-order")
        val previous = kinds.associateWith { DownloadStore.operations(it)[PreparationJobs.SITES] }
        try {
            DownloadStore.operation(PreparationJobs.SITES, "preparation", Gson().toJson(legacy))
            DownloadStore.operation(PreparationJobs.SITES, "preparation-generation", "legacy-generation")
            DownloadStore.operation(PreparationJobs.SITES, "preparation-paused", "1")
            DownloadStore.flush()
            // New shows and additional episodes from the current show may arrive concurrently.
            coroutineScope {
                additions.map { record -> async(Dispatchers.Default) { PreparationJobs.submit(app, PreparationJobs.SITES, record) } }.awaitAll()
            }
            ids.forEach { id ->
                val raw = PreparationJobs.payload(PreparationJobs.itemSlot(id))
                assertNotNull(raw)
                assertEquals(1, parseSiteDownloadPreparation(raw!!)!!.episodes.size)
            }
            assertNull(DownloadStore.operations("preparation")[PreparationJobs.SITES])
            val legacyIds = splitPreparation(legacy).keys.toList()
            assertTrue(legacyIds.all { PreparationJobs.itemSlot(it) in DownloadStore.operations("preparation-paused") })
            val untouched = PreparationJobs.itemSlot(legacyIds[1])
            val untouchedGeneration = DownloadStore.operations("preparation-generation")[untouched]
            PreparationJobs.resume(app, PreparationJobs.itemSlot(legacyIds[0]))
            assertEquals(untouchedGeneration, DownloadStore.operations("preparation-generation")[untouched])
            assertTrue(untouched in DownloadStore.operations("preparation-paused"))
            PreparationJobs.discard(app, PreparationJobs.itemSlot(legacyIds[0]))
            assertNotNull(PreparationJobs.payload(untouched))
        } finally {
            ids.forEach { PreparationJobs.discard(app, PreparationJobs.itemSlot(it)) }
            previous.forEach { (kind, value) -> DownloadStore.operation(PreparationJobs.SITES, kind, value) }
            DownloadStore.flush()
        }
    }
}
