package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class SourceOptionsTest {
    private fun source(quality: String, audio: String) = EpisodeSource("https://kwik.cx/e/example", quality, audio)

    @Test fun onlyAdvertisedSupportedSourcesAreOffered() {
        val options = parseEpisodeSources("""
            <button data-src="https://kwik.cx/e/one" data-resolution="360" data-audio="jpn" class="active"></button>
            <button data-src="https://kwik.cx/e/av1" data-resolution="1080" data-audio="eng" data-av1="1"></button>
            <button data-src="javascript:bad" data-resolution="720" data-audio="eng"></button>
        """.trimIndent())
        assertEquals(listOf("360p"), options.map { it.quality })
        assertEquals(listOf("jpn"), options.map { it.audio })
        assertEquals("jpn", selectEpisodeSource(options, "360p", "Original").audio)
    }

    @Test fun choicesMustExistForEverySelectedEpisodeAndLanguage() {
        val first = listOf(source("360p", "jpn"), source("720p", "jpn"), source("360p", "eng"))
        val second = listOf(source("360p", "jpn"), source("720p", "eng"))
        assertEquals(listOf("360p" to "jpn"), commonSourceOptions(listOf(first, second)).map { it.quality to it.audio })
        assertTrue(commonSourceOptions(listOf(first, emptyList())).isEmpty())
    }

    @Test fun languagesAreExplicitAndUnavailableChoicesFail() {
        assertEquals("Japanese · JPN", audioLabel("jpn"))
        assertEquals("English · ENG", audioLabel("eng"))
        assertEquals("Spanish · SPA", audioLabel("spa"))
        assertThrows(java.io.IOException::class.java) { selectEpisodeSource(listOf(source("360p", "jpn")), "720p", "jpn") }
        assertThrows(java.io.IOException::class.java) { selectEpisodeSource(listOf(source("360p", "jpn")), "360p", "eng") }
    }
}
