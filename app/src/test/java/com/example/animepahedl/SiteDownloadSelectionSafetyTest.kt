package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteDownloadSelectionSafetyTest {
    private val episodes = listOf(1, 2, 3, 5, 9).map { number ->
        SiteDownloadEpisode("episode-$number", number, "Episode $number", "https://provider.test/episode/$number")
    }

    @Test fun blankSelectsAllButExplicitClearNeverSelectsAll() {
        assertEquals(episodes, parseSiteDownloadSelection("", episodes))
        assertEquals(episodes, parseSiteDownloadSelection("   ", episodes))
        assertTrue(parseSiteDownloadSelection("none", episodes).isEmpty())
        assertTrue(parseSiteDownloadSelection(" NoNe ", episodes).isEmpty())
        // Reapplying a cleared selection after a new preview must remain empty.
        assertTrue(parseSiteDownloadSelection("none", episodes + SiteDownloadEpisode("new", 10, "New", "https://provider.test/10")).isEmpty())
    }

    @Test fun rangesAndRepeatedNumbersKeepSeriesOrderWithoutDuplicates() {
        val selected = parseSiteDownloadSelection("9, 1-3, 2, 5", episodes)
        assertEquals(listOf(1, 2, 3, 5, 9), selected.map { it.number })
        assertEquals(listOf(2, 3, 9), parseSiteDownloadSelection("9,2-3", episodes).map { it.number })
    }

    @Test fun invalidOrUnavailableSelectionNeverFallsBackToAllEpisodes() {
        for (input in listOf("1-", "abc", "1,,2", "3-1", "0", "10001", "4", "1-5", "none,1", "1-2-3")) {
            assertThrows("Input '$input' must be rejected", Exception::class.java) {
                parseSiteDownloadSelection(input, episodes)
            }
        }
    }

    @Test fun noEpisodesRemainEmptyForBothBlankAndClear() {
        assertTrue(parseSiteDownloadSelection("", emptyList()).isEmpty())
        assertTrue(parseSiteDownloadSelection("none", emptyList()).isEmpty())
        assertThrows(Exception::class.java) { parseSiteDownloadSelection("1", emptyList()) }
    }
}
