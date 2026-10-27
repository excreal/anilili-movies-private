package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class OutputFolderTest {
    @Test fun supportsRootAndNestedFolders() {
        assertEquals("", normalizeOutputFolder(" / "))
        assertEquals("Anime/Season 1", normalizeOutputFolder(" /Anime/Season 1/ "))
    }
    @Test fun rejectsTraversalAndInvalidNames() {
        listOf("../Movies", "Anime/../Movies", "Anime//Season", "Anime/./Season", "Anime\\Season", "Anime:Season", "Anime/\nSeason")
            .forEach { assertNull(it, normalizeOutputFolder(it)) }
    }
}
