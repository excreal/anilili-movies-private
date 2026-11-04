package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimeRepositoryTest {
    private val items = listOf(
        Anime("a", "Zeta", "", "2020", "TV", "Finished"),
        Anime("b", "Alpha", "", "2024", "Movie", "Finished"),
        Anime("c", "Beta", "", "", "TV", "Airing")
    )

    @Test fun sortingPlacesUnknownYearsLast() {
        assertEquals(listOf("b", "a", "c"), sortResults(items, ResultSort.NEWEST).map { it.session })
        assertEquals(listOf("a", "b", "c"), sortResults(items, ResultSort.OLDEST).map { it.session })
        assertEquals(listOf("b", "c", "a"), sortResults(items, ResultSort.TITLE).map { it.session })
    }

    @Test fun playlistKeepsSignedQueryAndUnescapesSlashes() {
        assertEquals("https://cdn.example/video.m3u8?token=abc&expires=123",
            findMediaUrl("source: 'https:\\/\\/cdn.example\\/video.m3u8?token=abc&amp;expires=123'"))
    }

    @Test fun directMediaAndMissingSource() {
        assertEquals("https://cdn.example/file.mp4?sig=123", findMediaUrl("<source src=\"https://cdn.example/file.mp4?sig=123\">"))
        assertNull(findMediaUrl("No downloadable media here"))
    }
}
