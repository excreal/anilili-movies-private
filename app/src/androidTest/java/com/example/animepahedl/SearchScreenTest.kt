package com.example.animepahedl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun clearingSearchReturnsToTheEmptyExploreScreen() {
        compose.onNodeWithText("Search anime").performTextInput("Naruto")
        compose.onNodeWithText("Search anime").performTextClearance()
        compose.onNodeWithText("Good stories, ready to go.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search").assertIsNotEnabled()
    }

    @Test fun searchIsTheEntryPointWithoutManualEpisodeOrSessionFields() {
        compose.onNodeWithText("Search anime").assertIsDisplayed()
        compose.onNodeWithText("Session ID").assertDoesNotExist()
        compose.onNodeWithText("Episodes", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Search").assertIsNotEnabled()
        compose.onNodeWithText("Search anime").performTextInput("Naruto")
        compose.onNodeWithContentDescription("Search").assertIsEnabled()
    }
}
