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

    @Test fun yearAndTypeFiltersCombine() {
        assertEquals(listOf(items[0]), filterResults(items, "2020", "TV", ResultSort.RELEVANCE))
        assertEquals(emptyList<Anime>(), filterResults(items, "2024", "TV", ResultSort.RELEVANCE))
    }

    @Test fun sortingPlacesUnknownYearsLast() {
        assertEquals(listOf("b", "a", "c"), filterResults(items, "All years", "All types", ResultSort.NEWEST).map { it.session })
        assertEquals(listOf("a", "b", "c"), filterResults(items, "All years", "All types", ResultSort.OLDEST).map { it.session })
        assertEquals(listOf("b", "c", "a"), filterResults(items, "All years", "All types", ResultSort.TITLE).map { it.session })
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
