package com.example.animepahedl

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.*
import org.junit.Test

class PreparationResponsivenessTest {
    @Test fun preparingEpisodesAllowsLeavingTheShowAndSearching() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val anime = Anime("fixture", "Fixture", "", "2026", "TV", "Finished")
        val episode = Episode("episode", "1", "", "24 min")
        val fake = object : AnimeRepository() {
            override suspend fun detailsIncremental(anime: Anime, onPage: suspend (AnimeDetails) -> Unit) = AnimeDetails(anime, listOf(episode))
            override suspend fun sources(anime: Anime, episode: Episode) = listOf(EpisodeSource("https://example.org/play", "720p", "jpn"))
            override suspend fun playlist(anime: Anime, episode: Episode, quality: String, audio: String): ResolvedMedia = awaitCancellation()
            override suspend fun search(query: String) = listOf(anime.copy(title = query))
        }
        lateinit var model: DownloaderViewModel
        fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
        fun waitFor(check: () -> Boolean) {
            val end = System.nanoTime() + 10_000_000_000L
            while (System.nanoTime() < end) {
                var ready = false
                main { ready = check() }
                if (ready) return
                Thread.sleep(25)
            }
            main { assertTrue("Timed out", check()) }
        }
        main { model = DownloaderViewModel(instrumentation.targetContext.applicationContext as Application, fake); model.open(anime) }
        try {
            waitFor { model.details != null && !model.loadingSources }
            main { model.toggle(episode) }
            waitFor { !model.loadingSources && model.availableSources.isNotEmpty() }
            main { model.downloadSelected() }
            waitFor { model.resolving && model.pendingPreparation != null }
            main { model.back(); assertNull(model.selectedAnime); model.updateQuery("Another show") }
            waitFor { model.results.firstOrNull()?.title == "Another show" }
            main { assertTrue(model.resolving); model.showDownloads = true; assertTrue(model.showDownloads) }
        } finally {
            main { model.cancelPreparation(); model.updateQuery("") }
            waitFor { !model.resolving }
            main { model.discardPreparation() }
        }
    }
}
