package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class ExistingVideoNameTest {
    private val expected = "Masamune-kun's Revenge ep01 1080p ENG.mp4"
    @Test fun exactVariantAndAndroidCopiesMatch() {
        assertTrue(sameVideoFile(expected, expected))
        assertTrue(sameVideoFile("Masamune-kun's Revenge ep01 1080p ENG (1).mp4", expected))
        assertTrue(sameVideoFile("Masamune-kun's Revenge ep01 1080p ENG.webm", expected))
    }
    @Test fun otherEpisodeQualityLanguageAndNonVideoFilesDoNotMatch() {
        assertFalse(sameVideoFile(expected.replace("ep01", "ep02"), expected))
        assertFalse(sameVideoFile(expected.replace("1080p", "720p"), expected))
        assertFalse(sameVideoFile(expected.replace("ENG", "JPN"), expected))
        assertFalse(sameVideoFile(expected.replace(".mp4", ".srt"), expected))
    }
    @Test fun movieYearsAreNotTreatedAsDuplicateSuffixes() {
        assertFalse(sameVideoFile("Movie (2025).mp4", "Movie (2024).mp4"))
        assertTrue(sameVideoFile("Movie (2024) (1).mp4", "Movie (2024).mp4"))
    }
}
