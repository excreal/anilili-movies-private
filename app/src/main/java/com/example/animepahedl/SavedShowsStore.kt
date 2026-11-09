package com.example.animepahedl

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.MessageDigest

/** Explicit bookmarks are independent from playback history and downloaded files. */
internal class SavedShowsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("saved-shows", Context.MODE_PRIVATE)
    private val gson = Gson()
    private fun key(series: SiteDownloadSeries) = "show:" + MessageDigest.getInstance("SHA-256")
        .digest("${series.provider}:${series.id}".toByteArray()).joinToString("") { "%02x".format(it) }
    fun contains(series: SiteDownloadSeries) = preferences.contains(key(series))
    fun toggle(series: SiteDownloadSeries) {
        if (contains(series)) preferences.edit().remove(key(series)).apply()
        else preferences.edit().putString(key(series), gson.toJson(series)).apply()
        changes.value += 1
    }
    fun all(): List<SiteDownloadSeries> = preferences.all.entries.filter { it.key.startsWith("show:") }
        .mapNotNull { runCatching { gson.fromJson(it.value as String, SiteDownloadSeries::class.java) }.getOrNull() }
        .sortedBy { it.title.lowercase() }
    companion object { val changes = MutableStateFlow(0L) }
}

internal fun savedSeriesFromDownloads(rows: List<DownloadRow>): SiteDownloadSeries? {
    val first = rows.firstOrNull() ?: return null
    val records = DownloadStore.all()
    val identity = records[first.id]?.identity?.takeIf { it.isSiteDownload } ?: return null
    val source = identity.anime.session.removePrefix("sites:")
    val provider = runCatching { SiteDownloadProvider.valueOf(source.substringBefore(':')) }.getOrNull() ?: return null
    val episodes = rows.mapIndexedNotNull { index, row ->
        records[row.id]?.identity?.locator?.takeIf(String::isNotBlank)?.let { url ->
            SiteDownloadEpisode(row.episodeId.ifBlank { row.id }, if (provider == SiteDownloadProvider.KISSKH) index + 1 else row.episodeNumber.toIntOrNull() ?: 1, row.title, url,
                if (provider == SiteDownloadProvider.KISSKH) row.episodeNumber else null)
        }
    }
    if (episodes.isEmpty()) return null
    return SiteDownloadSeries(provider, source.substringAfter(':'), first.showTitle.ifBlank { identity.anime.title },
        first.thumbnail, episodes.first().url, episodes)
}

/** Groups saved episodes by their saved show ID, or by stored series session for legacy rows. */
internal fun savedDownloadPlaylist(
    initial: DownloadRow,
    rows: List<DownloadRow>,
    records: Map<String, ExportRecord>,
): List<DownloadRow> {
    val matching = if (initial.showId.isNotBlank()) {
        rows.filter { it.complete && it.showId == initial.showId }
    } else {
        val session = records[initial.id]?.identity?.anime?.session.orEmpty()
        rows.filter { row ->
            row.complete && (row.id == initial.id || (session.isNotBlank() && records[row.id]?.identity?.anime?.session == session))
        }
    }
    return matching.sortedBy { it.episodeNumber.toDoubleOrNull() ?: 0.0 }.ifEmpty { listOf(initial) }
}

/** Rebuilds an offline, landscape-only queue when a saved site row has no readable provider series. */
internal fun savedLandscapeFallbackFromDownloads(
    rows: List<DownloadRow>,
    records: Map<String, ExportRecord>,
): SiteDownloadSeries? {
    val savedRows = rows.filter { it.complete }
    val first = savedRows.firstOrNull() ?: return null
    val storedSession = records[first.id]?.identity?.anime?.session.orEmpty()
    val session = storedSession.ifBlank { first.showId }
    val encodedProvider = session.takeIf { it.startsWith("sites:") }
        ?.removePrefix("sites:")?.substringBefore(':').orEmpty()
    val provider = runCatching { SiteDownloadProvider.valueOf(encodedProvider) }.getOrNull()
    if (provider?.isShortDrama == true) return null
    val episodes = savedRows.mapIndexedNotNull { index, row ->
        val uri = row.savedUri.ifBlank { records[row.id]?.uri.orEmpty() }.takeIf(String::isNotBlank) ?: return@mapIndexedNotNull null
        SiteDownloadEpisode(row.id, row.episodeNumber.toIntOrNull() ?: index + 1, row.title, uri)
    }
    if (episodes.isEmpty()) return null
    val stableKey = session.takeIf(String::isNotBlank) ?: first.id
    val title = first.showTitle.ifBlank { records[first.id]?.identity?.anime?.title.orEmpty().ifBlank { first.title } }
    return SiteDownloadSeries(SiteDownloadProvider.DIRECT, "saved:$stableKey",
        title, first.thumbnail, episodes.first().url, episodes)
}
