package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class DownloadNamingTest {
    @Test fun desktopStyleFoldersAndPaddedEpisodes() {
        assertEquals(DownloadLocation("My Show", "My Show ep01 720p.mp4"), downloadLocation("My Show", "1", "720p", "mp4"))
        assertEquals("My Show ep123 360p.webm", downloadLocation("My Show", "123", "360p", "webm").fileName)
        assertEquals("My Show ep01.5 1080p.mp4", downloadLocation("My Show", "1.5", "1080p", "mp4").fileName)
    }
    @Test fun showNamesCannotEscapeOutputDirectory() {
        val location = downloadLocation("../Show: Part/Two", "01", "720p", "mp4")
        assertFalse(location.showFolder.contains('/'))
        assertFalse(location.fileName.contains('/'))
        assertEquals("Anime", safeFileName(".."))
    }
}
