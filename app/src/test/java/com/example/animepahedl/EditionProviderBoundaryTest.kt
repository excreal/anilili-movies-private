package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EditionProviderBoundaryTest {
    @Test fun movieEditionRejectsShortDramaLinksBeforeAnyNetworkRequest() = runBlocking {
        val repository = appSiteDownloadRepository()
        val inspectError = runCatching { repository.inspect("https://narto-drama.com/detail/watch/story") }.exceptionOrNull()
        val playbackError = runCatching { repository.resolve("https://kiss18.vip/watch/story/1") }.exceptionOrNull()

        assertTrue(inspectError is IOException)
        assertEquals("Short drama sources aren't included in this edition.", inspectError?.message)
        assertTrue(playbackError is IOException)
        assertEquals("Short drama sources aren't included in this edition.", playbackError?.message)
    }

    @Test fun movieEditionExposesOnlyItsCatalogProvidersAndDirectMedia() {
        assertEquals(
            setOf(SiteDownloadProvider.KISSKH, SiteDownloadProvider.EROTICMV, SiteDownloadProvider.DIRECT),
            MovieCatalogConfig.supportedProviders,
        )
    }
}
