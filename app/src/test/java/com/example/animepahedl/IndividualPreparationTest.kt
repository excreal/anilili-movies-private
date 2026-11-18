package com.example.animepahedl

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class IndividualPreparationTest {
    private val episodes = (1..3).map { SiteDownloadEpisode("ep-$it", it, "Episode $it", "https://kisskh.do/episode/$it") }
    private val series = SiteDownloadSeries(SiteDownloadProvider.KISSKH, "show", "Show", "", "https://kisskh.do/show", episodes)

    @Test fun failureAndPauseRemainLocalAndOtherEpisodesKeepPreparing() {
        val ids = episodes.map { pendingEpisodeRowId(series, it) }
        val status = aggregatePreparationStatus(mapOf(
            ids[0] to PreparationStatus(true, paused = true, label = "Could not prepare this link after 3 attempts. Resume to retry."),
            ids[1] to PreparationStatus(true, running = true, activeRowId = ids[1]),
            ids[2] to PreparationStatus(true)))
        val rows = mergePreparedQueueRows(emptyList(), SiteDownloadPreparation(series, episodes, "Best"), status)
        assertTrue(status.automatic)
        assertEquals(TransferState.PAUSED, rows[0].transfer)
        assertEquals(setOf(DownloadAction.DELETE, DownloadAction.RESUME), rows[0].actions)
        assertEquals("Preparing download link", rows[1].status)
        assertTrue(DownloadAction.PAUSE in rows[1].actions)
        assertFalse(rows[2].status.contains("3 attempts"))
    }

    @Test fun anotherRequestedQualityStaysVisibleBesideExistingEpisode() {
        val pending = SiteDownloadPreparation(series, episodes.take(1), "1080p")
        val existing = DownloadRow("saved", "Episode", TransferState.COMPLETE, 100f,
            showId = "sites:KISSKH:show", episodeId = episodes[0].id, quality = "720p")
        assertEquals(2, mergePreparedQueueRows(listOf(existing), pending, PreparationStatus(true)).size)
        assertEquals(1, mergePreparedQueueRows(listOf(existing.copy(quality = "1080p")), pending, PreparationStatus(true)).size)
        val failed = existing.copy(quality = "1080p", transfer = TransferState.FAILED)
        assertEquals(2, mergePreparedQueueRows(listOf(failed), pending, PreparationStatus(true)).size)
        assertNotEquals(pendingEpisodeRowId(series, episodes[0], "720p"), pendingEpisodeRowId(series, episodes[0], "1080p"))
    }

    @Test fun legacyMissingQueuedFieldRestoresAndSplitsChronologically() {
        val raw = Gson().toJson(SiteDownloadPreparation(series, episodes.reversed(), "Best"))
        val restored = parseSiteDownloadPreparation(raw)!!
        val items = splitPreparation(restored).values.toList()
        assertEquals(episodes.map { it.id }, items.map { it.episodes.single().id })
        assertTrue(items.all { it.batches().size == 1 })
        assertTrue(inheritLegacyPreparationState("preparation-paused", first = false, failed = false))
        assertFalse(inheritLegacyPreparationState("preparation-verification", first = false, failed = false))
        assertFalse(inheritLegacyPreparationState("preparation-paused", first = false, failed = true))
    }

    @Test fun largeSelectionPersistsOnlyOneEpisodeAndPreservesMovieArtworkPerItem() {
        val movie = series.copy(provider = SiteDownloadProvider.EROTICMV, episodes = (1..1000).map {
            episodes[0].copy(id = "ep-$it", number = it)
        }, metadata = SiteShowMetadata(images = listOf(SiteShowImage("https://example.test/poster.jpg", "video-poster")),
            episodes = listOf(SiteEpisodeMetadata("1", "Episode", description = "x".repeat(10000)))))
        val items = splitPreparation(SiteDownloadPreparation(movie, movie.episodes, "Best"))
        assertTrue(items.values.all { it.series.episodes.size == 1 && it.series.metadata == null })
        assertTrue(items.values.all { it.series.posterThumbnail() == "https://example.test/poster.jpg" })
        assertTrue(Gson().toJson(items.values.first()).length < 2000)
    }

    @Test fun corruptPreparationDoesNotPreventOtherJournalsFromRestoring() {
        val valid = Gson().toJson(SiteDownloadPreparation(series, episodes.take(1), "Best"))
        val recovered = listOf("null", "{}", "{", valid, "{\"series\":null}").mapNotNull(::parseSiteDownloadPreparation)
        assertEquals(1, recovered.size)
        assertEquals(episodes[0], recovered.single().episodes.single())
    }

    @Test fun verificationAndFolderAccessOnlyAffectTheirOwnEpisodes() {
        val ids = episodes.map { pendingEpisodeRowId(series, it) }
        val status = aggregatePreparationStatus(mapOf(
            ids[0] to PreparationStatus(true, verificationUrl = series.url),
            ids[1] to PreparationStatus(true, needsFolderAccess = true),
            ids[2] to PreparationStatus(true)))
        val rows = mergePreparedQueueRows(emptyList(), SiteDownloadPreparation(series, episodes, "Best"), status)
        assertTrue(status.automatic)
        assertEquals(TransferState.PAUSED, rows[0].transfer)
        assertTrue(DownloadAction.RESUME in rows[0].actions)
        assertEquals(FailureReason.FOLDER_ACCESS, rows[1].failure)
        assertEquals(null, rows[2].failure)
    }
}
