package com.example.animepahedl

/** Placeholder IDs use source episodes and requested quality, never signed media URLs. */
internal fun pendingEpisodeRowId(series: SiteDownloadSeries, episode: SiteDownloadEpisode, quality: String = "Best"): String {
    val stable = "${series.provider.name}\u0000${series.id}\u0000${episode.id}\u0000$quality"
    val digest = java.security.MessageDigest.getInstance("SHA-256").digest(stable.toByteArray(Charsets.UTF_8))
    return "preparing:" + java.util.HexFormat.of().formatHex(digest)
}

internal fun isPreparedQueueRow(id: String) = id.startsWith("preparing:")

/** Every selected item is visible immediately, while real downloads replace placeholders by source identity. */
internal fun mergePreparedQueueRows(actual: List<DownloadRow>, pending: SiteDownloadPreparation?, status: PreparationStatus): List<DownloadRow> {
    if (pending == null) return actual
    data class EpisodeMatch(val showId: String, val id: String, val number: String = "")
    val existing = actual.filter { !it.failed && !it.deleting }.groupBy { row ->
        EpisodeMatch(row.showId, row.episodeId, row.episodeNumber.takeIf { row.episodeId.isBlank() }.orEmpty())
    }
    val queued = pending.batches().flatMap { batch ->
        val showId = "sites:${batch.series.provider.name}:${batch.series.id}"
        batch.episodes.filterNot { episode ->
            val matchingEpisode = existing[EpisodeMatch(showId, episode.id)].orEmpty() +
                existing[EpisodeMatch(showId, "", episode.displayNumber)].orEmpty()
            matchingEpisode.any { matchesDownloadQuality(batch.quality, it.requestedQuality ?: it.quality, it.quality) }
        }.map { episode ->
            val id = pendingEpisodeRowId(batch.series, episode, batch.quality)
            val status = status.items[id] ?: if (status.items.isEmpty()) status else PreparationStatus(pending = true)
            DownloadRow(
                id = id,
                title = batch.series.title + (if (batch.series.isSingleVideo) "" else " · Episode ${episode.displayNumber}") + " · ${batch.quality}",
                transfer = if (status.paused || status.verificationUrl != null) TransferState.PAUSED else TransferState.QUEUED,
                percent = -1f,
                transferControllable = true,
                failure = when {
                    status.needsFolderAccess -> FailureReason.FOLDER_ACCESS
                    else -> null
                },
                thumbnail = batch.series.posterThumbnail(),
                quality = batch.quality,
                explanation = when {
                    status.needsFolderAccess -> "Choose folder"
                    status.verificationUrl != null -> "Verify session"
                    status.paused -> status.label.ifBlank { "Paused" }
                    status.running && status.activeRowId == id -> "Adding to queue"
                    else -> status.label.ifBlank { "Queued" }
                },
                showId = showId,
                episodeId = episode.id,
                showTitle = batch.series.title,
                episodeNumber = episode.displayNumber,
            )
        }
    }.distinctBy { it.id }
    return if (queued.isEmpty()) actual else actual + queued
}
