package com.example.animepahedl

/** Separate from AnimePahe's "episodes" preparation record. Contains no cookie input. */
internal data class SiteDownloadPreparation(
    val series: SiteDownloadSeries,
    val episodes: List<SiteDownloadEpisode>,
    val quality: String,
    val queued: List<SiteDownloadBatch>? = null
)

internal data class SiteDownloadBatch(
    val series: SiteDownloadSeries,
    val episodes: List<SiteDownloadEpisode>,
    val quality: String
)

internal fun SiteDownloadPreparation.batches(): List<SiteDownloadBatch> =
    (if (episodes.isEmpty()) emptyList() else listOf(SiteDownloadBatch(series, episodes, quality))) + queued.orEmpty()

internal fun SiteDownloadPreparation.withBatches(batches: List<SiteDownloadBatch>): SiteDownloadPreparation? =
    batches.firstOrNull()?.let { first ->
        copy(series = first.series, episodes = first.episodes, quality = first.quality, queued = batches.drop(1))
    }

/** Bulk selection is an input convenience; every source episode and quality owns its journal. */
internal fun splitPreparation(record: SiteDownloadPreparation): Map<String, SiteDownloadPreparation> = buildMap {
    for (batch in record.batches()) for (episode in chronologicalSiteEpisodes(batch.episodes)) {
        // Persist only what preparation needs. Copying a show's entire episode catalog into every
        // item makes a large selection quadratic in size and can exhaust memory on restart.
        val series = batch.series.copy(episodes = listOf(episode), metadata = null,
            posterThumbnailOverride = batch.series.posterThumbnail(), description = "")
        putIfAbsent(pendingEpisodeRowId(series, episode, batch.quality),
            SiteDownloadPreparation(series, listOf(episode), batch.quality))
    }
}

internal fun aggregatePreparationStatus(items: Map<String, PreparationStatus>): PreparationStatus {
    val values = items.values
    val running = values.count { it.running }
    val waiting = values.count { it.automatic && !it.running }
    val blocked = values.count { !it.automatic }
    return PreparationStatus(pending = items.isNotEmpty(), paused = items.isNotEmpty() && values.all { it.paused },
        label = "$running preparing · $waiting waiting · $blocked need attention",
        verificationUrl = values.firstNotNullOfOrNull { it.verificationUrl },
        needsFolderAccess = values.any { it.needsFolderAccess }, totalSelected = items.size,
        running = running > 0, items = items)
}

internal fun inheritLegacyPreparationState(kind: String, first: Boolean, failed: Boolean): Boolean =
    first || kind == "preparation-folder" || (kind == "preparation-paused" && !failed)

/** Gson can return null or leave required Kotlin fields null in an old/damaged journal. */
private val preparationGson = com.google.gson.Gson()
internal fun parseSiteDownloadPreparation(raw: String): SiteDownloadPreparation? = runCatching {
    preparationGson.fromJson(raw, SiteDownloadPreparation::class.java)?.takeIf { record ->
        val batches = record.batches()
        batches.isNotEmpty() && batches.all { batch ->
            requireNotNull(batch.series.poster)
            batch.series.provider.name.isNotBlank() && batch.series.id.isNotBlank() &&
                batch.series.title.isNotBlank() &&
                batch.quality.isNotBlank() && batch.episodes.isNotEmpty() && batch.episodes.all {
                    it.id.isNotBlank() && it.url.isNotBlank()
                }
        }
    }
}.getOrNull()

/** Keep batch execution chronological even when a provider lists its newest episode first. */
internal fun chronologicalSiteEpisodes(episodes: List<SiteDownloadEpisode>): List<SiteDownloadEpisode> =
    episodes.withIndex().sortedWith(compareBy<IndexedValue<SiteDownloadEpisode>> {
        it.value.displayNumber.toBigDecimalOrNull() ?: java.math.BigDecimal.valueOf(Long.MAX_VALUE)
    }.thenBy { it.index }).map { it.value }

internal const val SITE_DOWNLOAD_PREPARATION_KEY = "sites"
