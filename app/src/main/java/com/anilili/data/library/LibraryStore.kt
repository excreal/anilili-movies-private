package com.anilili.data.library

/** Progress callbacks from the source player's external-player handoff use the host history. */
internal object LibraryStore {
    fun updateProgress(anilistId: Int, episodeNumber: Double, positionMs: Long, durationMs: Long) {
        com.example.animepahedl.ReferencePlaybackRegistry.progress(anilistId, episodeNumber, positionMs, durationMs, false)
    }
    fun markEpisodeWatched(anilistId: Int, watchedEpisode: Double, nextEpisode: Double? = null,
        nextEpisodeTitle: String? = null, seriesCompleted: Boolean = false, totalEpisodes: Int? = null) {
        com.example.animepahedl.ReferencePlaybackRegistry.progress(anilistId, watchedEpisode, 0, 0, true)
    }
    fun canSyncProgress(anilistId: Int) = false // Host has no AniList/MAL account connection.
    fun enqueueProgressSync(anilistId: Int, episode: Int, total: Int?) = Unit
}
