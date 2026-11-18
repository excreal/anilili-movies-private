package com.example.animepahedl

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class PersistedExportRecordTest {
    @Test fun absentAndNullNonNullFieldsGetSafeDefaults() {
        val record = requireNotNull(parsePersistedExportRecord("""{"title":null,"tree":null,"folder":null,"uri":null,"error":null,"bytes":-9}"""))
        assertEquals("Downloaded video", record.title)
        assertTrue(record.tree.isEmpty())
        assertTrue(record.folder.isEmpty())
        assertTrue(record.uri.isEmpty())
        assertTrue(record.error.isEmpty())
        assertEquals(0L, record.bytes)
        assertEquals("Old video", parsePersistedExportRecord("""{"title":"Old video"}""")!!.title)
    }

    @Test fun malformedRecoveryContextDoesNotHideSavedVideo() {
        val record = requireNotNull(parsePersistedExportRecord("""{"title":"Saved video","uri":"content://downloads/42",
            "tree":[],"error":{},"bytes":"broken","identity":{"anime":null},"media":{"url":null}}"""))
        assertEquals("content://downloads/42", record.uri)
        assertEquals("Saved video", record.title)
        assertNull(record.identity)
        assertNull(record.media)
        assertEquals("", record.error)
    }

    @Test fun incompleteNestedFieldsAndCollectionsCannotIntroduceNulls() {
        val record = requireNotNull(parsePersistedExportRecord("""{"title":"Video",
            "identity":{"anime":{"session":"show"},"episode":{"session":"ep"},"quality":"720p","audio":"und","source":"sites"},
            "media":{"url":"https://cdn.test/video","headers":{"Cookie":null,"Referer":"https://site.test/"},
                "backupUrls":[null,{},"https://backup.test/video"],"subtitles":[null,{},
                    {"url":"file:///caption.vtt","label":null,"language":null,"mimeType":null}]}}"""))
        val identity = requireNotNull(record.identity)
        assertEquals("Video", identity.anime.title)
        assertEquals("", identity.anime.synopsis)
        assertEquals("", identity.episode.number)
        assertTrue(identity.key.startsWith("sites:"))
        val media = requireNotNull(record.media)
        assertEquals("720p", media.quality)
        assertEquals(mapOf("Referer" to "https://site.test/"), media.headers)
        assertEquals(listOf("https://backup.test/video"), media.backupUrls)
        assertEquals(listOf(MediaSubtitle("file:///caption.vtt", "", "")), media.subtitles)
    }

    @Test fun validRowsPreserveIdentityMediaAndSavedOutput() {
        val original = ExportRecord("Video", "content://tree", "Shows", "content://downloads/42", bytes = 100,
            identity = DownloadIdentity(Anime("show", "Show", "poster", "2026", "Series", "Airing"),
                Episode("ep", "2", "snapshot", "10m"), "720p", "und", "sites", "https://site.test/ep"),
            pendingUri = "content://downloads/43", media = ResolvedMedia("https://cdn.test/video", "720p", "und",
                mapOf("Cookie" to "session=value"), listOf("https://backup.test/video"), "video/mp4",
                listOf(MediaSubtitle("file:///caption.vtt", "English", "en", isDefault = true))))
        assertEquals(original, parsePersistedExportRecord(Gson().toJson(original)))
    }

    @Test fun unrecognizableRowsAreIgnoredWithoutThrowing() {
        listOf("not json", "null", "[]", "42", "{}", "{\"other\":true}").forEach {
            assertNull(parsePersistedExportRecord(it))
        }
    }
}
