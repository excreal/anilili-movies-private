package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class SiteDownloadSourcesTest {
    @Test fun movieInputRejectsMalformedAndCredentialedLinks() {
        assertEquals(SiteDownloadProvider.EROTICMV, siteDownloadProvider(siteDownloadInput("https://eroticmv.com/film/")))
        assertEquals(SiteDownloadProvider.DIRECT, siteDownloadProvider(siteDownloadInput("https://cdn.test/video.mp4")))
        assertTrue(runCatching { siteDownloadInput("123") }.isFailure)
        assertTrue(runCatching { siteDownloadInput("file:///etc/passwd") }.isFailure)
        assertTrue(runCatching { siteDownloadInput("https://user:secret@eroticmv.com/film/") }.isFailure)
    }

    @Test fun metadataOverviewAndEroticMvPosterUseTheDetailedArtwork() {
        val series = SiteDownloadSeries(SiteDownloadProvider.EROTICMV, "m", "Movie", "banner.jpg", "https://eroticmv.com/m/",
            emptyList(), description = "Short OG text", metadata = SiteShowMetadata(description = "Full synopsis",
                images = listOf(SiteShowImage("poster.jpg", "video-poster"))))
        assertEquals("Full synopsis", series.overviewText())
        assertEquals("poster.jpg", series.posterThumbnail())
        assertEquals("banner.jpg", series.copy(provider = SiteDownloadProvider.KISSKH).posterThumbnail())
    }

    @Test fun selectedEpisodesPrepareInChronologicalOrderRegardlessOfSourceOrder() {
        val episodes = listOf("10", "2", "1", "1.5").mapIndexed { index, number ->
            SiteDownloadEpisode("id-$index", index + 1, "", "https://kisskh.do/show?ep=$number", number)
        }

        assertEquals(listOf("1", "1.5", "2", "10"), chronologicalSiteEpisodes(episodes).map { it.displayNumber })
    }

    @Test fun linkRefreshIsBoundedPerEpisodeAndDoesNotRetryDirectUrls() {
        val guard = MediaLinkRefreshGuard()
        assertTrue(guard.shouldRefresh(SiteDownloadProvider.EROTICMV, "video-1"))
        assertFalse(guard.shouldRefresh(SiteDownloadProvider.EROTICMV, "video-1"))
        assertTrue(guard.shouldRefresh(SiteDownloadProvider.EROTICMV, "video-2"))
        assertFalse(guard.shouldRefresh(SiteDownloadProvider.DIRECT, "file-1"))
    }

    @Test fun encodedMediaAndHtmlEntities() {
        val raw = "https://cdn.test/video.m3u8?x=1&y=2"
        assertEquals(raw, decodeEroticMedia("http://${Base64.getEncoder().encodeToString(raw.toByteArray())}.m3u8"))
        assertEquals(raw, eroticVideoSource("""<meta content="https://cdn.test/video.m3u8?x=1&amp;y=2" property="og:video:url">"""))
        assertEquals("https://cdn.test/a.m3u8", eroticVideoSource("""<script>{"single_video_url":"https:\/\/cdn.test\/a.m3u8"}</script>"""))
    }
}
