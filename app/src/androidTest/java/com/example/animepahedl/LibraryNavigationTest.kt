package com.example.animepahedl

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LibraryNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun preferredAudioIsSavedFromCompactSettings() {
        val settings = AppSettings(compose.activity)
        val previous = settings.defaultAudio
        try {
            compose.onNode(hasText("Settings") and hasClickAction()).performClick()
            compose.onNodeWithText("Default audio").performClick()
            compose.onNodeWithText("English · ENG").performClick()
            compose.runOnIdle { org.junit.Assert.assertEquals("eng", settings.defaultAudio) }
        } finally { settings.defaultAudio = previous }
    }

    @Test fun largeCollectionsCollapseAndDeletionCanBeCancelled() {
        val app = compose.activity
        val prefix = "test-library-"
        try {
            repeat(100) { index -> Exports.put(app, "$prefix$index", ExportRecord(
                "Library Fixture · Episode ${index + 1} · 720p · JPN", "", "", uri = "content://fixture/$index")) }
            compose.waitUntil(10000) { androidx.lifecycle.ViewModelProvider(app)[DownloaderViewModel::class.java].downloads.any { it.id == "${prefix}99" } }
            compose.onNode(hasText("Downloads") and hasClickAction()).performClick()
            compose.onNodeWithText("100 · Expand").assertIsDisplayed()
            compose.onNodeWithText("Find downloaded episodes").performTextInput("Episode 100")
            compose.onNodeWithContentDescription("Actions for Library Fixture · Episode 100 · 720p · JPN").performClick()
            compose.onNodeWithText("Delete").performClick()
            compose.onNodeWithText("Remove download?").assertIsDisplayed()
            compose.onNodeWithText("Keep").performClick()
            compose.runOnIdle { org.junit.Assert.assertTrue(Exports.all(app).containsKey("${prefix}99")) }
        } finally { repeat(100) { Exports.remove(app, "$prefix$it") } }
    }

    @Test fun downloadsAndSettingsAreReachable() {
        compose.onNode(hasText("Downloads") and hasClickAction()).performClick()
        compose.onNodeWithText("Your offline collection").assertIsDisplayed()
        compose.onNode(hasText("In progress") and hasClickAction()).assertExists()
        compose.onNode(hasText("Settings") and hasClickAction()).performClick()
        compose.onNodeWithText("Download storage").assertIsDisplayed()
        compose.onNodeWithText("Download storage").performClick()
        compose.onNodeWithText("Choose folder").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithText("Browser session").assertIsDisplayed()
    }
}
