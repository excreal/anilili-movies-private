package com.example.animepahedl

/** Placeholder IDs describe source episodes, not resolved quality or signed media URLs. */
internal fun pendingEpisodeRowId(series: SiteDownloadSeries, episode: SiteDownloadEpisode): String {
    val stable = "${series.provider.name}\u0000${series.id}\u0000${episode.id}"
    return "preparing:" + java.security.MessageDigest.getInstance("SHA-256")
        .digest(stable.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

internal fun isPreparedQueueRow(id: String) = id.startsWith("preparing:")

/** Every selected item is visible immediately, while real downloads replace placeholders by source identity. */
internal fun mergePreparedQueueRows(actual: List<DownloadRow>, pending: SiteDownloadPreparation?, status: PreparationStatus): List<DownloadRow> {
    if (pending == null) return actual
    val showId = "sites:${pending.series.provider.name}:${pending.series.id}"
    val existing = actual.filter { it.showId == showId }.map { it.episodeId }.filter { it.isNotBlank() }.toHashSet()
    val existingNumbers = actual.filter { it.showId == showId && it.episodeId.isBlank() }.map { it.episodeNumber }.toHashSet()
    val queued = pending.episodes.filterNot { it.id in existing || it.number.toString() in existingNumbers }.map { episode ->
        DownloadRow(
            id = pendingEpisodeRowId(pending.series, episode),
            title = pending.series.title + (if (pending.series.isSingleVideo) "" else " · Episode ${episode.displayNumber}") + " · ${pending.quality}",
            transfer = if (status.paused) TransferState.PAUSED else TransferState.QUEUED,
            percent = -1f,
            thumbnail = pending.series.poster,
            quality = pending.quality,
            explanation = when {
                status.needsFolderAccess -> "Queued · choose downloads folder"
                status.paused -> "Queued · link preparation paused"
                status.verificationUrl != null -> "Queued · session verification needed"
                else -> "Queued · preparing link in background"
            },
            showId = showId,
            episodeId = episode.id,
            showTitle = pending.series.title,
            episodeNumber = episode.displayNumber,
        )
    }
    return if (queued.isEmpty()) actual else actual + queued
}
