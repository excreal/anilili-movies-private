package com.example.animepahedl

/** Separate from AnimePahe's "episodes" preparation record. Contains no cookie input. */
internal data class SiteDownloadPreparation(
    val series: SiteDownloadSeries,
    val episodes: List<SiteDownloadEpisode>,
    val quality: String,
    val queued: List<SiteDownloadBatch> = emptyList()
)

internal data class SiteDownloadBatch(
    val series: SiteDownloadSeries,
    val episodes: List<SiteDownloadEpisode>,
    val quality: String
)

internal fun SiteDownloadPreparation.batches(): List<SiteDownloadBatch> =
    listOf(SiteDownloadBatch(series, episodes, quality)) + queued

internal const val SITE_DOWNLOAD_PREPARATION_KEY = "sites"
