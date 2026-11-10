package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class SiteDownloadRepositoryTest {
    @Test fun metadataMergeDropsResultsForReplacedTitleAndGeneration() {
        val old = SiteDownloadSeries(SiteDownloadProvider.KISSKH, "old", "Old", "", "https://kisskh.do/Drama/Old?id=1", emptyList())
        val replacement = old.copy(id = "new", title = "New")
        assertSame(replacement, mergeInspectionMetadata(replacement, old.provider, old.id, 4, 5, SiteShowMetadata(description = "stale")))
        val updated = mergeInspectionMetadata(replacement, replacement.provider, replacement.id, 5, 5,
            SiteShowMetadata(description = "Current synopsis"))
        assertEquals("New", updated.title)
        assertEquals("Current synopsis", updated.metadata?.description)
    }

    @Test fun httpErrorsDoNotExposeCookieOrBackendResponse() = runBlocking {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(403).message("Forbidden")
                .body("private backend detail".toResponseBody("text/html".toMediaType())).build()
        }.build()
        val result = runCatching { SiteDownloadRepository(client).inspect("https://eroticmv.com/film/", "session=secret") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message.orEmpty().contains("HTTP 403"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("secret"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("private backend"))
    }

    @Test fun directFilesPreserveTypesAndHttpLinks() = runBlocking {
        val repo = SiteDownloadRepository()
        assertEquals("video/webm", repo.resolve("https://cdn.test/video.webm").mimeType)
        assertEquals("http://cdn.test/video.mp4", repo.inspect("http://cdn.test/video.mp4").episodes.single().url)
    }
}
