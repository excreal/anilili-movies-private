package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadGroupArtworkTest {
    @Test fun groupPosterPrefersSeriesArtworkOverEpisodeThumbnails() {
        val identity = DownloadIdentity(
            Anime("show", "Show", "series-poster.jpg", "", "Series", ""),
            Episode("episode-1", "1", "series-poster.jpg", ""), "Best", "und",
        )
        val record = ExportRecord("Show · Episode 1", "", "", thumbnail = "episode-frame.jpg", identity = identity)
        val row = DownloadRow("episode-1", record.title, TransferState.COMPLETE, 100f,
            thumbnail = "episode-frame.jpg", showTitle = "Show")

        assertEquals("series-poster.jpg", downloadGroupArtwork(listOf(row), mapOf(row.id to record)))
    }

    @Test fun groupPosterFallsBackToEpisodeArtworkWhenNoSeriesRecordExists() {
        val first = DownloadRow("one", "Show · Episode 1", TransferState.COMPLETE, 100f,
            thumbnail = "", showTitle = "Show")
        val second = DownloadRow("two", "Show · Episode 2", TransferState.COMPLETE, 100f,
            thumbnail = "available-artwork.jpg", showTitle = "Show")

        assertEquals("available-artwork.jpg", downloadGroupArtwork(listOf(first, second), emptyMap()))
    }

    @Test fun eroticMovieDownloadIdentityUsesItsNormalPosterVariant() {
        val series = SiteDownloadSeries(SiteDownloadProvider.EROTICMV, "movie", "Movie", "banner.jpg",
            "https://eroticmv.com/movie/", listOf(SiteDownloadEpisode("movie", 1, "Movie", "https://eroticmv.com/movie/")),
            metadata = SiteShowMetadata(images = listOf(SiteShowImage("poster.jpg", "video-poster"),
                SiteShowImage("banner.jpg", "banner"))))

        assertEquals("poster.jpg", DownloadIdentity.siteDownload(series, series.episodes.single(), "Best", "und").anime.poster)
    }
}
