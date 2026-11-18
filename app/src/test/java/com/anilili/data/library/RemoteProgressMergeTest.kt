package com.anilili.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteProgressMergeTest {
    private val local = HistoryEntry(
        anilistId = 17,
        title = "Series",
        cover = null,
        episodeNumber = 3.0,
        provider = AUTO_PROVIDER,
        category = "TV",
    )

    @Test fun implausiblyLargeRemoteProgressIsIgnoredBeforeExpandingWatchedEpisodes() {
        assertNull(historyAfterRemoteProgress(local, MAX_REMOTE_PROGRESS_EPISODES + 1, 100, null))
    }

    @Test fun maximumIntegerProgressCannotOverflowTheNextEpisode() {
        assertNull(remoteRowEpisode(Int.MAX_VALUE, null, null))
        assertNull(historyAfterRemoteProgress(local, Int.MAX_VALUE, 100, null))
    }

    @Test fun validRemoteProgressStillAdvancesAndMarksFinishedEpisodes() {
        val merged = historyAfterRemoteProgress(local, 4, 100, totalEpisodes = 12)!!

        assertEquals(5.0, merged.episodeNumber, 0.0)
        assertEquals(setOf(1.0, 2.0, 3.0, 4.0), merged.watchedEpisodes)
        assertEquals(100L, merged.updatedAt)
    }
}
