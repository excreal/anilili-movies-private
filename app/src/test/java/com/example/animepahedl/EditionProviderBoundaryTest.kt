package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import okhttp3.OkHttpClient

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

    @Test fun directlyConstructedRepositoryAlsoRejectsShortDramaBeforeNetwork() = runBlocking {
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor {
            requests++
            throw IOException("Unexpected request")
        }.build()
        val repository = SiteDownloadRepository(client = client)

        val inspectError = runCatching {
            repository.inspect("https://narto-drama.com/detail/watch/story")
        }.exceptionOrNull()
        val playbackError = runCatching {
            repository.resolve("https://kiss18.vip/watch/story/1")
        }.exceptionOrNull()

        assertTrue(inspectError is IOException)
        assertTrue(playbackError is IOException)
        assertEquals("Short drama sources aren't included in this edition.", inspectError?.message)
        assertEquals("Short drama sources aren't included in this edition.", playbackError?.message)
        assertEquals(0, requests)
    }

    @Test fun movieEditionExposesOnlyItsCatalogProvidersAndDirectMedia() {
        assertEquals(
            setOf(SiteDownloadProvider.KISSKH, SiteDownloadProvider.EROTICMV, SiteDownloadProvider.DIRECT),
            MovieCatalogConfig.supportedProviders,
        )
    }
}
