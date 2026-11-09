package com.example.animepahedl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeNavigatesToBrowseWithTheCurrentSearchPlaceholder() {
        compose.onNodeWithContentDescription("Search").performClick()

        compose.onNodeWithText("Browse").assertIsDisplayed()
        compose.onNodeWithText("Search Dramatip…").assertIsDisplayed()
    }

    @Test fun clearingBrowseSearchRestoresTheCurrentPlaceholder() {
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithText("Search Dramatip…").performTextInput("Naruto")
        compose.onNodeWithText("Naruto").performTextClearance()

        // Check only local navigation and field state; catalog results depend on the network.
        compose.onNodeWithText("Browse").assertIsDisplayed()
        compose.onNodeWithText("Search Dramatip…").assertIsDisplayed()
    }
}
