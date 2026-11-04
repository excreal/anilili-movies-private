package com.example.animepahedl

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LibraryNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun downloadNotificationOpensDownloadsFromAbout() {
        compose.onNode(hasText("Settings") and hasClickAction()).performClick()
        compose.onNodeWithText("About").performClick()
        compose.onNodeWithText("What’s new").assertIsDisplayed()
        val activity = compose.activity
        val launchIntent = android.content.Intent(activity.intent)
        try {
            downloadsPendingIntent(activity).send()
            compose.waitUntil(10000) {
                androidx.lifecycle.ViewModelProvider(activity)[DownloaderViewModel::class.java].showDownloads
            }
            compose.onNodeWithText("Your offline collection").assertIsDisplayed()
        } finally {
            // ActivityScenario matches lifecycle events against the original launch intent.
            // onNewIntent legitimately replaces it, so restore the test identity for teardown.
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                activity.intent = launchIntent
            }
        }
    }

    @Test fun deletedSavedFileRemainsAvailableForRepair() {
        val app = compose.activity
        val id = "test-deleted-${System.currentTimeMillis()}"
        val uri = app.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$id.mp4")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Animepahe-test/")
            }) ?: error("Cannot create test file")
        try {
            Exports.put(app, id, ExportRecord("Deleted fixture", "", "", uri = uri.toString()))
            val model = androidx.lifecycle.ViewModelProvider(app)[DownloaderViewModel::class.java]
            compose.waitUntil(10000) { model.downloads.any { it.id == id } }
            app.contentResolver.delete(uri, null, null)
            compose.waitUntil(10000) { Exports.all(app)[id]?.error?.contains("missing") == true }
            compose.runOnIdle { org.junit.Assert.assertTrue(model.downloads.any { it.id == id }) }
        } finally {
            app.contentResolver.delete(uri, null, null)
            Exports.remove(app, id)
        }
    }

    @Test fun aboutShowsInstalledVersionAndReturnsToSettings() {
        val info = compose.activity.packageManager.getPackageInfo(compose.activity.packageName, 0)
        compose.onNode(hasText("Settings") and hasClickAction()).performClick()
        compose.onNodeWithText("About").performClick()
        compose.onNodeWithText("Version ${info.versionName} · Build ${info.longVersionCode}").assertIsDisplayed()
        compose.onNodeWithText("What’s new").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Download storage").assertIsDisplayed()
    }

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

    @Test fun simultaneousDownloadLimitIsSavedAndApplied() {
        val app = compose.activity
        val model = androidx.lifecycle.ViewModelProvider(app)[DownloaderViewModel::class.java]
        val previous = AppSettings(app).simultaneousDownloads
        try {
            compose.onNode(hasText("Settings") and hasClickAction()).performClick()
            compose.onNodeWithText("Download speed").performClick()
            compose.onNodeWithText("Episodes at a time").assertIsDisplayed()
            compose.onNodeWithText("3", useUnmergedTree = true).performClick()
            compose.runOnIdle {
                org.junit.Assert.assertEquals(3, AppSettings(app).simultaneousDownloads)
                org.junit.Assert.assertEquals(3, OfflineDownloads.manager(app).maxParallelDownloads)
            }
        } finally { compose.runOnIdle { model.updateSimultaneousDownloads(previous) } }
    }

    @Test fun largeCollectionsAreVisibleAndDeletionCanBeCancelled() {
        val app = compose.activity
        val prefix = "test-library-"
        try {
            repeat(100) { index -> Exports.put(app, "$prefix$index", ExportRecord(
                "Library Fixture · Episode ${index + 1} · 720p · JPN", "", "", uri = "content://fixture/$index")) }
            compose.waitUntil(10000) { androidx.lifecycle.ViewModelProvider(app)[DownloaderViewModel::class.java].downloads.any { it.id == "${prefix}99" } }
            compose.onNode(hasText("Downloads") and hasClickAction()).performClick()
            compose.onNodeWithText("Find downloaded episodes").performTextInput("Episode 100")
            compose.onNodeWithText("Library Fixture", substring = false).performClick()
            compose.onNodeWithContentDescription("Actions for Library Fixture · Episode 100 · 720p · JPN").performClick()
            compose.onNodeWithText("Delete saved video").performClick()
            compose.onNodeWithText("Delete saved video?").assertIsDisplayed()
            compose.onNodeWithText("Keep").performClick()
            compose.runOnIdle { org.junit.Assert.assertTrue(Exports.all(app).containsKey("${prefix}99")) }
        } finally { repeat(100) { Exports.remove(app, "$prefix$it") } }
    }

    @Test fun downloadsAndSettingsAreReachable() {
        compose.onNode(hasText("Downloads") and hasClickAction()).performClick()
        compose.onNodeWithText("Your offline collection").assertIsDisplayed()
        compose.onNode(hasText("Sort: Show title") and hasClickAction()).assertExists()
        compose.onNode(hasText("Settings") and hasClickAction()).performClick()
        compose.onNodeWithText("Download storage").assertIsDisplayed()
        compose.onNodeWithText("Download storage").performClick()
        compose.onNodeWithText("Choose folder").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithText("Browser session").assertIsDisplayed()
    }
}
