package com.example.animepahedl

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DownloadSelectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun selectAllIncludesCollapsedGroupsAndRotationPreservesSelection() {
        val app = compose.activity
        val ids = (1..3).map { "direct:${System.nanoTime() + it}" }
        try {
            ids.forEachIndexed { index, id -> Exports.put(app, id, ExportRecord("Selection Fixture · Episode ${index + 1}", "", "", error = "Fixture failure")) }
            val model = ViewModelProvider(app)[DownloaderViewModel::class.java]
            compose.waitUntil(10000) { model.downloads.count { it.id in ids } == 3 }
            compose.onNode(hasText("Downloads") and hasClickAction()).performClick()
            compose.onNodeWithTag("downloads-search").performTextInput("Selection Fixture")
            compose.onAllNodesWithText("Selection Fixture", substring = false).onFirst().performClick()
            compose.onNodeWithContentDescription("Select downloads").performClick()
            compose.onNodeWithText("Select all", substring = false).performClick()
            compose.runOnIdle { assertEquals(ids.toSet(), model.selectedDownloadIds) }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("3 selected").assertIsDisplayed()
            compose.onNodeWithText("Delete", substring = false).performClick()
            compose.onNodeWithText("Delete 3 items?").assertIsDisplayed()
            compose.onNodeWithText("Keep").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
            compose.runOnIdle {
                assertSame(model, ViewModelProvider(compose.activity)[DownloaderViewModel::class.java])
                assertTrue("Keep must retain all selected records", Exports.all(compose.activity).keys.containsAll(ids))
            }
            compose.onNodeWithTag("downloads-list").performScrollToNode(hasTestTag("downloads-search"))
            compose.onNodeWithTag("downloads-search").performTextInput("other")
            compose.runOnIdle { assertTrue(model.selectedDownloadIds.isEmpty()) }
        } finally { ids.forEach { Exports.remove(app, it) } }
    }
}
