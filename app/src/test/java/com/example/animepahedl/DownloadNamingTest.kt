package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class DownloadNamingTest {
    @Test fun outOfOrderCompletionKeepsSourceEpisodeIdentity() {
        val records = (1..3).map { number ->
            val identity = DownloadIdentity(Anime("show", "My Show", "", "", "", ""),
                Episode("session-$number", "$number", "", ""), "720p", "jpn")
            ExportRecord("Wrong title · Episode 99", "", "", showTitle = "Wrong title",
                episodeNumber = "99", identity = identity)
        }
        val completed = listOf(records[2], records[0], records[1])
        assertEquals(listOf("My Show ep03 720p JPN.mp4", "My Show ep01 720p JPN.mp4", "My Show ep02 720p JPN.mp4"),
            completed.map { it.outputLocation("mp4").fileName })
        assertEquals(listOf("3", "1", "2"), completed.map { it.episodeTitle().split(" · ")[1].removePrefix("Episode ") })
    }

    @Test fun desktopStyleFoldersAndPaddedEpisodes() {
        assertEquals(DownloadLocation("My Show", "My Show ep01 720p.mp4"), downloadLocation("My Show", "1", "720p", "mp4"))
        assertEquals("My Show ep123 360p.webm", downloadLocation("My Show", "123", "360p", "webm").fileName)
        assertEquals("My Show ep01.5 1080p.mp4", downloadLocation("My Show", "1.5", "1080p", "mp4").fileName)
    }
    @Test fun audioAndQualityDistinguishOtherwiseIdenticalEpisodes() {
        assertEquals("My Show ep01 1080p ENG.mp4", downloadLocation("My Show", "1", "1080p", "mp4", "eng").fileName)
        assertNotEquals(downloadLocation("My Show", "1", "1080p", "mp4", "eng"), downloadLocation("My Show", "1", "1080p", "mp4", "jpn"))
        assertNotEquals(downloadLocation("My Show", "1", "1080p", "mp4", "eng"), downloadLocation("My Show", "1", "720p", "mp4", "eng"))
    }
    @Test fun showNamesCannotEscapeOutputDirectory() {
        val location = downloadLocation("../Show: Part/Two", "01", "720p", "mp4")
        assertFalse(location.showFolder.contains('/'))
        assertFalse(location.fileName.contains('/'))
        assertEquals("Anime", safeFileName(".."))
    }
}
